package edu.hm.crownet.testbed.payload;

import edu.hm.crownet.testbed.analytics.MetricsLoggerImpl;
import edu.hm.crownet.testbed.client.UdpClient;
import edu.hm.crownet.testbed.client.impl.UdpClientImpl;
import edu.hm.crownet.testbed.payload.data.Payload;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import edu.hm.crownet.testbed.scheduler.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.currentTimeMillis;
import static java.net.InetAddress.getByName;

@Service
public class PayloadSenderImpl implements PayloadSender {

  private static final String TASK_ID = "payload-sender-task";

  @Value("${crownet.testbed.wifi.broadcast.address:255.255.255.255}")
  private String broadcastAddress;

  @Value("${crownet.testbed.wifi.broadcast.payload-receive-port:8889}")
  private int receivePort;

  @Value("${crownet.testbed.wifi.broadcast.payload-send-port:8890}")
  private int senderPort;

  @Value("${crownet.testbed.host}")
  private String sourceId;

  private final Scheduler scheduler;

  private final RateAdaptionService rateAdaptionService;
  private final MessageSizeService messageSizeService;
  private final NodeEstimatorService nodeEstimatorService;
  private final MetricsLoggerImpl metricsLoggerImpl;

  private UdpClient senderUdpClient;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  public PayloadSenderImpl(
          Scheduler scheduler,
          @Qualifier("payloadRateAdaptionService") RateAdaptionService rateAdaptionService,
          @Qualifier("payloadMessageSizeService") MessageSizeService messageSizeService,
          @Qualifier("payloadNodeEstimatorService") NodeEstimatorService nodeEstimatorService,
          MetricsLoggerImpl metricsLoggerImpl) {
    this.scheduler = scheduler;
    this.rateAdaptionService = rateAdaptionService;
    this.messageSizeService = messageSizeService;
    this.nodeEstimatorService = nodeEstimatorService;
    this.metricsLoggerImpl = metricsLoggerImpl;
  }

  @Override
  public void startSending(boolean useRateAdaption) {
    if (isRunning.get()) {
      System.out.println("PayloadSender is already running");
      return;
    }

    try {
      senderUdpClient = new UdpClientImpl();
      senderUdpClient.initialize(senderPort);
      isRunning.set(true);

      if (useRateAdaption) {
        long delay = rateAdaptionService.obtainNextTransmissionTime();
        delayTransmission(delay);
        System.out.println("Started payload sender with initial delay: " + delay + " ms");
      } else {
        scheduler.scheduleTask(TASK_ID, this::stupidTransmit, 0, 200);
      }
    } catch (Exception e) {
      isRunning.set(false);
      if (senderUdpClient != null) {
        try {
          senderUdpClient.close();
        } catch (IOException ex) {
          System.err.println("Error closing UDP client during startup failure: " + ex.getMessage());
        }
      }
      throw new RuntimeException("Failed to start payload sender", e);
    }
  }

  @Override
  public void stopSending() {
    if (!isRunning.get()) return;

    isRunning.set(false);
    scheduler.stopExistingTaskIfPresent(TASK_ID);

    try {
      if (senderUdpClient != null) {
        senderUdpClient.close();
      }
      System.out.println("PayloadSender stopped");
    } catch (IOException e) {
      System.err.println("Error closing resources: " + e.getMessage());
    }
  }

  @Override
  public void scheduleSending(boolean useRateAdaption, LocalDateTime startTime, LocalDateTime endTime) {
    if (isRunning.get()) {
      System.out.println("PayloadSender is already running");
      return;
    }

    long startDelay = java.time.Duration.between(LocalDateTime.now(), startTime).toMillis();
    long endDelay = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();

    if (startDelay < 0) {
      throw new IllegalArgumentException("Start time cannot be in the past");
    }

    if (endDelay <= startDelay) {
      throw new IllegalArgumentException("End time must be after start time");
    }

    // Schedule start
    scheduler.scheduleOneShotTask(TASK_ID + "-start", () -> startSending(useRateAdaption), startDelay);
    
    // Schedule stop
    scheduler.scheduleOneShotTask(TASK_ID + "-stop", this::stopSending, endDelay);
    
    System.out.println("PayloadSender scheduled to start at " + startTime + " and stop at " + endTime);
  }

  private void delayTransmission(long delay) {
    if (!isRunning.get()) return;
    scheduler.scheduleOneShotTask(TASK_ID, this::reconsiderTransmission, delay);
  }

  private void reconsiderTransmission() {
    if (!isRunning.get()) return;

    long delta = rateAdaptionService.obtainDeltaT();
    if (delta <= 0) {
      transmit();
    } else {
      delayTransmission(delta);
    }
  }

  private void transmit() {
    if (!isRunning.get()) return;

    try {
      String sampleData = "Sample payload data from " + sourceId;
      var payload = new Payload(sourceId, currentTimeMillis(), sampleData.getBytes());
      var data = serializePayload(payload);
      var packet = new DatagramPacket(data, data.length, getByName(broadcastAddress), receivePort);
      senderUdpClient.broadcast(packet);

      messageSizeService.registerMessageSize(data.length);
      var avgMessageSize = messageSizeService.getAverageMessageSize();
      var nodeCount = nodeEstimatorService.currentAmountOfNeighbours();
      rateAdaptionService.updateEstimatedNodeCount(nodeCount);
      rateAdaptionService.updateAverageMessageSize(avgMessageSize);

      metricsLoggerImpl.logMetric(sourceId + "-payload", payload.timestamp(), nodeCount, packet.getLength());
      System.out.printf("%s sent a payload (size: %d bytes).\n", payload.sourceId(), data.length);

      var delay = rateAdaptionService.obtainNextTransmissionTime();
      delayTransmission(delay);
    } catch (Exception e) {
      throw new RuntimeException("Payload sending failed", e);
    }
  }

  private void stupidTransmit() {
    if (!isRunning.get()) return;

    try {
      var sampleData = "Sample payload data from " + sourceId;
      var payload = new Payload(sourceId, currentTimeMillis(), sampleData.getBytes());
      var data = serializePayload(payload);
      var packet = new DatagramPacket(data, data.length, getByName(broadcastAddress), receivePort);
      senderUdpClient.broadcast(packet);

      var nodeCount = nodeEstimatorService.currentAmountOfNeighbours();
      metricsLoggerImpl.logMetric(sourceId + "-payload", payload.timestamp(), nodeCount, packet.getLength());
      System.out.printf("%s sent a payload (size: %d bytes).\n", payload.sourceId(), data.length);
    } catch (Exception e) {
      throw new RuntimeException("Payload sending failed", e);
    }
  }

  private byte[] serializePayload(Payload payload) throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(payload);
      oos.flush();
      return baos.toByteArray();
    }
  }
} 