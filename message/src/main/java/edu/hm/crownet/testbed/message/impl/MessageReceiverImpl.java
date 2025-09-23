package edu.hm.crownet.testbed.message.impl;

import edu.hm.crownet.testbed.client.Receiver;
import edu.hm.crownet.testbed.message.MessageReceiver;
import edu.hm.crownet.testbed.message.data.Message;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

@Service
public class MessageReceiverImpl implements MessageReceiver {

  @Value("${crownet.testbed.adhoc.node-id}")
  private int sourceId;

  @Value("${crownet.testbed.adhoc.message.receive-port}")
  private int receivePort;

  // Rate Adaption Algorithm
  private final NodeEstimatorService nodeEstimatorService;
  private final MessageSizeService messageSizeService;
  private final RateAdaptionService rateAdaptionService;

  // Responsible for receiving UDP packets from the network
  private Receiver receiver;

  public MessageReceiverImpl(
          @Qualifier("oneHopNodeEstimator") NodeEstimatorService nodeEstimatorService,
          @Qualifier("messageMessageSizeService") MessageSizeService messageSizeService,
          @Qualifier("messageRateAdaptionService") RateAdaptionService rateAdaptionService
  ) {
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
    Message beacon = deserializeMessage(packet.getData());
    if (beacon.sourceId() == sourceId) return;

    // Update message size statistics
    messageSizeService.registerMessageSize(packet.getLength());

    // Get current neighbor count and update rate adaptation immediately
    int currentNeighborCount = nodeEstimatorService.currentAmountOfNeighbours();
    double avgMessageSize = messageSizeService.getAverageMessageSize();

    // Update rate adaptation with latest statistics
    rateAdaptionService.updateEstimatedNodeCount(currentNeighborCount);
    rateAdaptionService.updateAverageMessageSize(avgMessageSize);
  }

  private Message deserializeMessage(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    short seqNo = buffer.getShort();
    int sourceId = buffer.getInt();
    int timestamp = buffer.getInt();
    byte[] payload = new byte[1390];
    buffer.get(payload);

    return new Message(seqNo, sourceId, timestamp, payload);
  }
} 