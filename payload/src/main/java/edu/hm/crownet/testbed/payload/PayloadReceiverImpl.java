package edu.hm.crownet.testbed.payload;

import edu.hm.crownet.testbed.client.impl.UdpClientImpl;
import edu.hm.crownet.testbed.payload.data.Payload;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import edu.hm.crownet.testbed.scheduler.Scheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PayloadReceiverImpl implements PayloadReceiver {

  @Value("${crownet.testbed.host}")
  private String sourceId;

  @Value("${crownet.testbed.wifi.broadcast.payload-receive-port:8889}")
  private int port;

  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  private UdpClient udpClient;

  private final NodeEstimatorService nodeEstimatorService;
  private final MessageSizeService messageSizeService;
  private final RateAdaptionService rateAdaptionService;
  private final Scheduler scheduler;

  private Thread receiveThread;

  public PayloadReceiverImpl(
      @Qualifier("payloadNodeEstimatorService") NodeEstimatorService nodeEstimatorService,
      @Qualifier("payloadMessageSizeService") MessageSizeService messageSizeService,
      @Qualifier("payloadRateAdaptionService") RateAdaptionService rateAdaptionService,
      Scheduler scheduler) {
    this.nodeEstimatorService = nodeEstimatorService;
    this.messageSizeService = messageSizeService;
    this.rateAdaptionService = rateAdaptionService;
    this.scheduler = scheduler;
  }

  @Override
  public synchronized void startReceiving() {
    if (isRunning.get()) {
      System.out.println("PayloadReceiver is already running");
      return;
    }

    try {
      System.out.println("DEBUG: PayloadReceiver initializing on port " + port + " for host " + sourceId);
      udpClient = new UdpClientImpl();
      udpClient.initialize(port);
      isRunning.set(true);
      System.out.println("DEBUG: PayloadReceiver successfully initialized on port " + port);

      receiveThread = new Thread(() -> {
        System.out.println("PayloadReceiver started on port " + port);
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
          try {
            var packet = udpClient.receive();
            handleReceivedPayload(packet);
          } catch (RuntimeException e) {
            // Check if it's a socket-related issue
            var cause = e.getCause();
            if (cause instanceof SocketException) {
              if (isRunning.get()) {
                System.err.println("Socket error in PayloadReceiver: " + cause.getMessage());
              }
              break;
            } else if (cause instanceof SocketTimeoutException) {
              // Timeout is expected, continue listening
            } else {
              System.err.println("Runtime error in PayloadReceiver: " + e.getMessage());
            }
          } catch (Exception e) {
            System.err.println("Unexpected error in PayloadReceiver: " + e.getMessage());
          }
        }
        System.out.println("PayloadReceiver thread stopped");
      });
      receiveThread.setName("PayloadReceiver-Thread");
      receiveThread.setDaemon(true);
      receiveThread.start();
    } catch (Exception e) {
      isRunning.set(false);
      throw new RuntimeException("Failed to start payload receiver", e);
    }
  }

  @Override
  public synchronized void stopReceiving() {
    if (!isRunning.get()) {
      return;
    }

    isRunning.set(false);
    if (receiveThread != null) {
      receiveThread.interrupt();
      try {
        receiveThread.join(3000);
        if (receiveThread.isAlive()) {
          System.err.println("PayloadReceiver thread did not stop gracefully");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.err.println("Interrupted while waiting for PayloadReceiver thread to stop");
      }
    }

    try {
      udpClient.close();
      System.out.println("PayloadReceiver stopped");
    } catch (IOException e) {
      System.err.println("Error closing UDP client: " + e.getMessage());
    }
  }

  @Override
  public void scheduleReceiving(LocalDateTime startTime, LocalDateTime endTime) {
    if (isRunning.get()) {
      System.out.println("PayloadReceiver is already running");
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
    scheduler.scheduleOneShotTask("payload-receiver-start", this::startReceiving, startDelay);
    
    // Schedule stop
    scheduler.scheduleOneShotTask("payload-receiver-stop", this::stopReceiving, endDelay);
    
    System.out.println("PayloadReceiver scheduled to start at " + startTime + " and stop at " + endTime);
  }

  private void handleReceivedPayload(DatagramPacket packet) {
    try (var bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength()); var ois = new ObjectInputStream(bais)) {
      var obj = ois.readObject();

      if (!(obj instanceof Payload payload)) {
        System.err.println("Received non-payload object: " + obj.getClass().getSimpleName() + " from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
        return;
      }

      // Only process payloads from other nodes (not our own)
      if (!payload.sourceId().equals(sourceId)) {
        // Register payload for neighbor tracking and update message size statistics
        nodeEstimatorService.registerBeacon(payload.sourceId());
        messageSizeService.registerMessageSize(packet.getLength());

        // Get current neighbor count and update rate adaptation immediately
        int currentNeighborCount = nodeEstimatorService.currentAmountOfNeighbours();
        double avgMessageSize = messageSizeService.getAverageMessageSize();

        // Update rate adaptation with latest statistics
        rateAdaptionService.updateEstimatedNodeCount(currentNeighborCount);
        rateAdaptionService.updateAverageMessageSize(avgMessageSize);

        System.out.printf("Received payload from %s | Neighbors: %d | Avg msg size: %.1f | Packet: %d bytes%n", payload.sourceId(), currentNeighborCount, avgMessageSize, packet.getLength());
      }
    } catch (ClassNotFoundException e) {
      System.err.println("Unknown class in received payload: " + e.getMessage());
    } catch (IOException e) {
      System.err.println("Error deserializing payload: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("Unexpected error handling payload: " + e.getMessage());
    }
  }
} 