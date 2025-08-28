package edu.hm.crownet.testbed.message;

import edu.hm.crownet.testbed.client.Sender;
import edu.hm.crownet.testbed.message.data.Message;
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
public class MessageSenderImpl implements MessageSender {

  private static final String TASK_ID = "measurement-sender";

  @Value("${crownet.testbed.adhoc.node-id}")
  private int sourceId;

  @Value("${crownet.testbed.adhoc.broadcast-address}")
  private String broadcastAddress;

  @Value("${crownet.testbed.adhoc.message.send-port}")
  private int sendPort;

  @Value("${crownet.testbed.adhoc.message.receive-port}")
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

  public MessageSenderImpl(
          Scheduler scheduler,
          @Qualifier("oneHopNodeEstimator") NodeEstimatorService nodeEstimatorService,
          @Qualifier("messageRateAdaptionService") RateAdaptionService rateAdaptionService,
          @Qualifier("messageMessageSizeService") MessageSizeService messageSizeService
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
      Message message = createMessage();
      byte[] data = serializeMessage(message);
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

  private Message createMessage() {
    short seqNo = (short) (sequenceNumber.getAndIncrement() & 0xFFFF);
    int timestamp = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    // Dummy payload to ensure the Message reaches 11200 bit (1400 B) total size.
    // Required so that application-layer packet size matches the configuration
    // assumed in simulations executed by the lecturer.
    byte[] dummyPayload = new byte[1390];
    return new Message(seqNo, sourceId, timestamp, dummyPayload);
  }

  private byte[] serializeMessage(Message message) {
    ByteBuffer buffer = ByteBuffer.allocate(1400);
    buffer.putShort(message.sequenceNumber());
    buffer.putInt(message.sourceId());
    buffer.putInt(message.timestampMs());
    buffer.put(message.payload());

    return buffer.array();
  }
} 