package edu.hm.crownet.testbed.beacon;

import edu.hm.crownet.testbed.analytics.MetricsLoggerImpl;
import edu.hm.crownet.testbed.beacon.data.Beacon;
import edu.hm.crownet.testbed.client.UdpClient;
import edu.hm.crownet.testbed.client.impl.UdpClientImpl;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import edu.hm.crownet.testbed.scheduler.Scheduler;
import lombok.RequiredArgsConstructor;
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
public class BeaconSenderImpl implements BeaconSender {

  private static final String TASK_ID = "beacon-sender-task";

  @Value("${crownet.testbed.wifi.broadcast.address:255.255.255.255}")
  private String broadcastAddress;

  @Value("${crownet.testbed.wifi.broadcast.receive-port:8888}")
  private int receivePort;

  @Value("${crownet.testbed.wifi.broadcast.send-port:8887}")
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

  public BeaconSenderImpl(
      Scheduler scheduler,
      @Qualifier("beaconRateAdaptionService") RateAdaptionService rateAdaptionService,
      @Qualifier("beaconMessageSizeService") MessageSizeService messageSizeService,
      @Qualifier("beaconNodeEstimatorService") NodeEstimatorService nodeEstimatorService,
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
      System.out.println("BeaconSender is already running");
      return;
    }

    try {
      senderUdpClient = new UdpClientImpl();
      senderUdpClient.initialize(senderPort);
      isRunning.set(true);

        if (useRateAdaption) {
          long delay = rateAdaptionService.obtainNextTransmissionTime();
          delayTransmission(delay);
          System.out.println("Started beacon sender with initial delay: " + delay + " ms");
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
      throw new RuntimeException("Failed to start beacon sender", e);
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
      System.out.println("BeaconSender stopped");
    } catch (IOException e) {
      System.err.println("Error closing resources: " + e.getMessage());
    }
  }

  @Override
  public void scheduleSending(boolean useRateAdaption, LocalDateTime startTime, LocalDateTime endTime) {
    if (isRunning.get()) {
      System.out.println("BeaconSender is already running");
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
    
    System.out.println("BeaconSender scheduled to start at " + startTime + " and stop at " + endTime);
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
      var beacon = new Beacon(sourceId, currentTimeMillis());
      var data = serializeBeacon(beacon);
      var packet = new DatagramPacket(data, data.length, getByName(broadcastAddress), receivePort);
      
      System.out.println("DEBUG: BeaconSender sending beacon to " + broadcastAddress + ":" + receivePort + " from " + sourceId);
      senderUdpClient.broadcast(packet);
      System.out.println("DEBUG: BeaconSender successfully sent beacon packet of size " + data.length + " bytes");

      messageSizeService.registerMessageSize(data.length);
      var avgMessageSize = messageSizeService.getAverageMessageSize();
      var nodeCount = nodeEstimatorService.currentAmountOfNeighbours();
      rateAdaptionService.updateEstimatedNodeCount(nodeCount);
      rateAdaptionService.updateAverageMessageSize(avgMessageSize);

      metricsLoggerImpl.logMetric(sourceId, beacon.timestamp(), nodeCount, packet.getLength());
      System.out.printf("%s sent a beacon.\n", beacon.sourceId());

      var delay = rateAdaptionService.obtainNextTransmissionTime();
      delayTransmission(delay);
    } catch (Exception e) {
      System.err.println("DEBUG: BeaconSender transmit failed: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Beacon sending failed", e);
    }
  }

  private void stupidTransmit() {
    if (!isRunning.get()) return;

    try {
      var beacon = new Beacon(sourceId, currentTimeMillis());
      var data = serializeBeacon(beacon);
      var packet = new DatagramPacket(data, data.length, getByName(broadcastAddress), receivePort);
      
      System.out.println("DEBUG: BeaconSender sending beacon (stupid mode) to " + broadcastAddress + ":" + receivePort + " from " + sourceId);
      senderUdpClient.broadcast(packet);
      System.out.println("DEBUG: BeaconSender successfully sent beacon packet (stupid mode) of size " + data.length + " bytes");

      var nodeCount = nodeEstimatorService.currentAmountOfNeighbours();
      metricsLoggerImpl.logMetric(sourceId, beacon.timestamp(), nodeCount, packet.getLength());
      System.out.printf("%s sent a beacon.\n", beacon.sourceId());
    } catch (Exception e) {
      System.err.println("DEBUG: BeaconSender stupidTransmit failed: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Beacon sending failed", e);
    }
  }


  private byte[] serializeBeacon(Beacon beacon) throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(beacon);
      oos.flush();
      return baos.toByteArray();
    }
  }
}