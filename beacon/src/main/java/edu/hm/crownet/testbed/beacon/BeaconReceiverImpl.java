package edu.hm.crownet.testbed.beacon;

import edu.hm.crownet.testbed.beacon.data.Beacon;
import edu.hm.crownet.testbed.client.Receiver;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

@Service
public class BeaconReceiverImpl implements BeaconReceiver {

  @Value("${crownet.testbed.adhoc.node-id}")
  private int sourceId;

  @Value("${crownet.testbed.adhoc.receive-port}")
  private int receivePort;

  // Rate Adaption Algorithm
  private final NodeEstimatorService nodeEstimatorService;
  private final MessageSizeService messageSizeService;
  private final RateAdaptionService rateAdaptionService;

  // Responsible for receiving UDP packets from the network
  private Receiver receiver;

  public BeaconReceiverImpl(
          @Qualifier("beaconNodeEstimatorService") NodeEstimatorService nodeEstimatorService,
          @Qualifier("beaconMessageSizeService") MessageSizeService messageSizeService,
          @Qualifier("beaconRateAdaptionService") RateAdaptionService rateAdaptionService) {
    this.nodeEstimatorService = nodeEstimatorService;
    this.messageSizeService = messageSizeService;
    this.rateAdaptionService = rateAdaptionService;
  }

  @Override
  public void stop() {
    receiver.stop();
  }

  @Override
  public void receive() {
    receiver = new Receiver(receivePort, this::handlePacket);
    receiver.start();
  }

  private void handlePacket(DatagramPacket packet) {
    Beacon beacon = deserializeBeacon(packet.getData());
    if (beacon.sourceId() == sourceId) return;

    // Register beacon for neighbor tracking and update message size statistics
    nodeEstimatorService.registerBeacon(beacon.sourceId());
    messageSizeService.registerMessageSize(packet.getLength());

    // Get current neighbor count and update rate adaptation immediately
    int currentNeighborCount = nodeEstimatorService.currentAmountOfNeighbours();
    double avgMessageSize = messageSizeService.getAverageMessageSize();

    // Update rate adaptation with latest statistics
    rateAdaptionService.updateEstimatedNodeCount(currentNeighborCount);
    rateAdaptionService.updateAverageMessageSize(avgMessageSize);
  }

  private Beacon deserializeBeacon(byte[] data) {
    if (data.length < 10) {
      throw new IllegalArgumentException("Invalid beacon data length: " + data.length);
    }

    ByteBuffer buffer = ByteBuffer.wrap(data);
    short sequenceNumber = buffer.getShort();
    int sourceId = buffer.getInt();
    int timestampMs = buffer.getInt();

    return new Beacon(sequenceNumber, sourceId, timestampMs);
  }
}