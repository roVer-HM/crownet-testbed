package edu.hm.crownet.testbed.beacon;

import edu.hm.crownet.testbed.analytics.BeaconLog;
import edu.hm.crownet.testbed.analytics.BeaconLogger;
import edu.hm.crownet.testbed.beacon.data.Beacon;
import edu.hm.crownet.testbed.client.Sender;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import edu.hm.crownet.testbed.scheduler.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.InetAddress.getByName;

@Service
public class BeaconSenderImpl implements BeaconSender {

  private static final String TASK_ID = "beacon-sender";

  @Value("${crownet.testbed.adhoc.node-id}")
  private int sourceId;

  @Value("${crownet.testbed.adhoc.broadcast-address}")
  private String broadcastAddress;

  @Value("${crownet.testbed.adhoc.beacon.send-port}")
  private int sendPort;

  @Value("${crownet.testbed.adhoc.beacon.receive-port}")
  private int receivePort;

  // Rate Adaption Algorithm
  private final Scheduler scheduler;
  private final RateAdaptionService rateAdaptionService;
  private final MessageSizeService messageSizeService;
  private final NodeEstimatorService nodeEstimatorService;

  // Beacon handling
  private final AtomicInteger sequenceNumber = new AtomicInteger(0);

  // Handles transmission of outgoing UDP packets
  private Sender sender;

  // Used for analysis
  private BeaconLogger beaconLogger;

  public BeaconSenderImpl(
          Scheduler scheduler,
          @Qualifier("oneHopNodeEstimator") NodeEstimatorService nodeEstimatorService,
          @Qualifier("beaconRateAdaptionService") RateAdaptionService rateAdaptionService,
          @Qualifier("beaconMessageSizeService") MessageSizeService messageSizeService
  ) {
    this.scheduler = scheduler;
    this.rateAdaptionService = rateAdaptionService;
    this.messageSizeService = messageSizeService;
    this.nodeEstimatorService = nodeEstimatorService;
  }

  @Override
  public void stop() {
    scheduler.stopExistingTaskIfPresent(TASK_ID);
    if (sender != null) {
      sender.close();
      sender = null;
    }
    try {
      if (beaconLogger != null) beaconLogger.close();
    } catch (Exception ignored) {
    }
  }

  @Override
  public void send(boolean useRateAdaption) {
    try {
      Path logPath = Path.of("/var/log/crownet", "beacons-node-" + sourceId + ".csv");
      beaconLogger = new BeaconLogger(logPath.toString());
    } catch (Exception e) {
      throw new RuntimeException("Failed to open beacon log", e);
    }

    sender = new Sender();
    sender.initialize(sendPort);


    if (!useRateAdaption) {
      System.out.printf(
              "BeaconSender initialized (fixed interval) | Node=%d | Log=%s | SendPort=%d | Interval=%d ms%n",
              sourceId,
              "/var/log/crownet/beacons-node-" + sourceId + ".csv",
              sendPort,
              100
      );

      // Start recurring task every 100 ms, immediately
      scheduler.scheduleTask(TASK_ID, this::stupidTransmit, 100, 100);
    } else {
      long initialDelay = rateAdaptionService.obtainNextTransmissionTime();
      System.out.printf(
              "BeaconSender initialized | Node=%d | Log=%s | SendPort=%d | First send in %d ms%n",
              sourceId,
              "/var/log/crownet/beacons-node-" + sourceId + ".csv",
              sendPort,
              initialDelay
      );
      delayTransmission(initialDelay);
    }
  }

  private void delayTransmission(long delay) {
    scheduler.scheduleOneShotTask(TASK_ID, this::reconsiderTransmission, delay);
  }

  private void reconsiderTransmission() {
    long delta = rateAdaptionService.obtainDeltaT();
    if (delta <= 0) transmit();
    else delayTransmission(delta);
  }

  private void transmit() {
    try {
      Beacon beacon = createBeacon();
      byte[] data = serializeBeacon(beacon); // 38 B
      DatagramPacket packet = new DatagramPacket(data, data.length, getByName(broadcastAddress), receivePort);
      sender.send(packet);

      //
      // Logging of sent beacon
      //
      if (beaconLogger != null) {
        beaconLogger.log(new BeaconLog(
                new Timestamp(System.currentTimeMillis()),
                sourceId,
                beacon.sequenceNumber() & 0xFFFF, // cast short to unsigned: ensures sequence number is always 0–65535 instead of negative values
                data.length
        ));
      }

      //
      // Update of parameters used for adaptive rate adaption algorithm.
      //
      messageSizeService.registerMessageSize(data.length);
      var avgMessageSize = messageSizeService.getAverageMessageSize();
      var nodeCount = nodeEstimatorService.currentAmountOfNeighbours();
      rateAdaptionService.updateEstimatedNodeCount(nodeCount);
      rateAdaptionService.updateAverageMessageSize(avgMessageSize);

      long delay = rateAdaptionService.obtainNextTransmissionTime();
      delayTransmission(delay);

      System.out.printf(
              "Sent beacon seq=%d | SourceId=%d | Size=%d B | Neighbors=%d | Avg msg size=%.1f B | Next interval=%d ms%n",
              beacon.sequenceNumber() & 0xFFFF,
              sourceId,
              data.length,
              nodeCount,
              avgMessageSize,
              delay
      );
    } catch (Exception e) {
      throw new RuntimeException("Beacon sending failed", e);
    }
  }

  private void stupidTransmit() {
    try {
      Beacon beacon = createBeacon();
      byte[] data = serializeBeacon(beacon); // 38 B
      DatagramPacket packet = new DatagramPacket(data, data.length, getByName(broadcastAddress), receivePort);
      sender.send(packet);

      if (beaconLogger != null) {
        beaconLogger.log(new BeaconLog(
                new Timestamp(System.currentTimeMillis()),
                sourceId,
                beacon.sequenceNumber() & 0xFFFF,
                data.length
        ));
      }

      // Update EMA so metrics/logging remain consistent
      messageSizeService.registerMessageSize(data.length);
    } catch (Exception e) {
      throw new RuntimeException("Beacon sending failed", e);
    }
  }

  private Beacon createBeacon() {
    short seqNo = (short) (sequenceNumber.getAndIncrement() & 0xFFFF);  // cast short to unsigned: ensures sequence number is always 0–65535 instead of negative values
    int timestamp = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    // Dummy payload to ensure the Beacon reaches 304 bit (38 Byte) total size.
    // Required so that application-layer packet size matches the configuration
    // assumed in simulations executed by the lecturer.
    byte[] dummyPayload = new byte[28];
    return new Beacon(seqNo, sourceId, timestamp, dummyPayload);
  }

  private byte[] serializeBeacon(Beacon beacon) {
    ByteBuffer buffer = ByteBuffer.allocate(38);
    buffer.putShort(beacon.sequenceNumber());
    buffer.putInt(beacon.sourceId());
    buffer.putInt(beacon.timestampMs());
    buffer.put(beacon.payload());
    return buffer.array();
  }
}