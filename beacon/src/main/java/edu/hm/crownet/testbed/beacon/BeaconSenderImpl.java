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
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.currentTimeMillis;
import static java.net.InetAddress.getByName;

@Service
@RequiredArgsConstructor
public class BeaconSenderImpl implements BeaconSender {

  private static final String TASK_ID = "beacon-sender-task";

  @Value("${crownet.testbed.wifi.broadcast.address}")
  private String broadcastAddress;

  @Value("${crownet.testbed.wifi.broadcast.receive-port}")
  private int receivePort;

  @Value("${crownet.testbed.wifi.broadcast.send-port}")
  private int senderPort;

  @Value("${crownet.testbed.host}")
  private String sourceId;

  private final Scheduler scheduler;
  
  @Qualifier("beaconRateAdaptionService")
  private final RateAdaptionService rateAdaptionService;
  
  @Qualifier("beaconMessageSizeService")
  private final MessageSizeService messageSizeService;
  
  @Qualifier("beaconNodeEstimatorService")
  private final NodeEstimatorService nodeEstimatorService;
  
  private final MetricsLoggerImpl metricsLoggerImpl;

  private UdpClient senderUdpClient;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);

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
      senderUdpClient.broadcast(packet);

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
      throw new RuntimeException("Beacon sending failed", e);
    }
  }

  private void stupidTransmit() {
    if (!isRunning.get()) return;

    try {
      var beacon = new Beacon(sourceId, currentTimeMillis());
      var data = serializeBeacon(beacon);
      var packet = new DatagramPacket(data, data.length, getByName(broadcastAddress), receivePort);
      senderUdpClient.broadcast(packet);

      var nodeCount = nodeEstimatorService.currentAmountOfNeighbours();
      metricsLoggerImpl.logMetric(sourceId, beacon.timestamp(), nodeCount, packet.getLength());
      System.out.printf("%s sent a beacon.\n", beacon.sourceId());
    } catch (Exception e) {
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