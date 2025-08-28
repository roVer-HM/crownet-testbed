package edu.hm.crownet.testbed.beacon;

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
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.InetAddress.getByName;

@Service
public class BeaconSenderImpl implements BeaconSender {

  private static final String TASK_ID = "beacon-sender";

  @Value("${crownet.testbed.adhoc.node-id}")
  private int sourceId;

  @Value("${crownet.testbed.adhoc.broadcast-address}")
  private String broadcastAddress;

  @Value("${crownet.testbed.adhoc.send-port}")
  private int sendPort;

  @Value("${crownet.testbed.adhoc.receive-port}")
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

  public BeaconSenderImpl(
          Scheduler scheduler,
          @Qualifier("beaconRateAdaptionService") RateAdaptionService rateAdaptionService,
          @Qualifier("beaconMessageSizeService") MessageSizeService messageSizeService,
          @Qualifier("beaconNodeEstimatorService") NodeEstimatorService nodeEstimatorService) {
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
  }

  @Override
  public void send() {
    sender = new Sender();
    sender.initialize(sendPort);

    long initialDelay = rateAdaptionService.obtainNextTransmissionTime();
    delayTransmission(initialDelay);
  }

  private void delayTransmission(long delay) {
    scheduler.scheduleOneShotTask(TASK_ID, this::reconsiderTransmission, delay);
  }

  private void reconsiderTransmission() {
    long delta = rateAdaptionService.obtainDeltaT();
    if (delta <= 0) {
      transmit();
    } else {
      delayTransmission(delta);
    }
  }

  private void transmit() {
    try {
      Beacon beacon = createBeacon();
      byte[] data = serializeBeacon(beacon);
      DatagramPacket packet = new DatagramPacket(data, data.length, getByName(broadcastAddress), receivePort);
      sender.send(packet);

      messageSizeService.registerMessageSize(data.length);
      var avgMessageSize = messageSizeService.getAverageMessageSize();
      var nodeCount = nodeEstimatorService.currentAmountOfNeighbours();
      rateAdaptionService.updateEstimatedNodeCount(nodeCount);
      rateAdaptionService.updateAverageMessageSize(avgMessageSize);

      long delay = rateAdaptionService.obtainNextTransmissionTime();
      delayTransmission(delay);
    } catch (Exception e) {
      throw new RuntimeException("Beacon sending failed", e);
    }
  }

  private Beacon createBeacon() {
    short seqNo = (short) (sequenceNumber.getAndIncrement() & 0xFFFF);
    int timestamp = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    return new Beacon(seqNo, sourceId, timestamp);
  }

  private byte[] serializeBeacon(Beacon beacon) {
    ByteBuffer buffer = ByteBuffer.allocate(10);
    buffer.putShort(beacon.sequenceNumber());
    buffer.putInt(beacon.sourceId());
    buffer.putInt(beacon.timestampMs());
    return buffer.array();
  }
}