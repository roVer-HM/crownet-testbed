package edu.hm.crownet.testbed.beacon;

import edu.hm.crownet.testbed.beacon.data.Beacon;
import edu.hm.crownet.testbed.client.UdpClient;
import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;
import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;
import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BeaconReceiverImpl implements BeaconReceiver {

  @Value("${crownet.testbed.host}")
  private String sourceId;

  @Value("${crownet.testbed.wifi.broadcast.receive-port:8888}")
  private int port;

  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  private final UdpClient udpClient;
  private final NodeEstimatorService nodeEstimatorService;
  private final MessageSizeService messageSizeService;
  private final RateAdaptionService rateAdaptionService;

  private Thread receiveThread;

  public BeaconReceiverImpl(
      UdpClient udpClient,
      @Qualifier("beaconNodeEstimatorService") NodeEstimatorService nodeEstimatorService,
      @Qualifier("beaconMessageSizeService") MessageSizeService messageSizeService,
      @Qualifier("beaconRateAdaptionService") RateAdaptionService rateAdaptionService) {
    this.udpClient = udpClient;
    this.nodeEstimatorService = nodeEstimatorService;
    this.messageSizeService = messageSizeService;
    this.rateAdaptionService = rateAdaptionService;
  }

  @Override
  public synchronized void startReceiving() {
    if (isRunning.get()) {
      System.out.println("BeaconReceiver is already running");
      return;
    }

    try {
      udpClient.initialize(port);
      isRunning.set(true);

      receiveThread = new Thread(() -> {
        System.out.println("BeaconReceiver started on port " + port);
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
          try {
            var packet = udpClient.receive();
            handleReceivedBeacon(packet);
          } catch (RuntimeException e) {
            // Check if it's a socket-related issue
            var cause = e.getCause();
            if (cause instanceof SocketException) {
              if (isRunning.get()) {
                System.err.println("Socket error in BeaconReceiver: " + cause.getMessage());
              }
              break;
            } else if (cause instanceof SocketTimeoutException) {
              // Timeout is expected, continue listening
            } else {
              System.err.println("Runtime error in BeaconReceiver: " + e.getMessage());
            }
          } catch (Exception e) {
            System.err.println("Unexpected error in BeaconReceiver: " + e.getMessage());
          }
        }
        System.out.println("BeaconReceiver thread stopped");
      });
      receiveThread.setName("BeaconReceiver-Thread");
      receiveThread.setDaemon(true);
      receiveThread.start();
    } catch (Exception e) {
      isRunning.set(false);
      throw new RuntimeException("Failed to start beacon receiver", e);
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
          System.err.println("BeaconReceiver thread did not stop gracefully");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.err.println("Interrupted while waiting for BeaconReceiver thread to stop");
      }
    }

    try {
      udpClient.close();
      System.out.println("BeaconReceiver stopped");
    } catch (IOException e) {
      System.err.println("Error closing UDP client: " + e.getMessage());
    }
  }

  private void handleReceivedBeacon(DatagramPacket packet) {
    try (var bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength()); var ois = new ObjectInputStream(bais)) {
      var obj = ois.readObject();
      if (!(obj instanceof Beacon beacon)) {
        System.err.println("Received non-beacon object: " + obj.getClass().getSimpleName());
        return;
      }

      // Only process beacons from other nodes (not our own)
      if (!beacon.sourceId().equals(sourceId)) {
        // Register beacon for neighbor tracking and update message size statistics
        nodeEstimatorService.registerBeacon(beacon.sourceId());
        messageSizeService.registerMessageSize(packet.getLength());

        // Get current neighbor count and update rate adaptation immediately
        int currentNeighborCount = nodeEstimatorService.currentAmountOfNeighbours();
        double avgMessageSize = messageSizeService.getAverageMessageSize();

        // Update rate adaptation with latest statistics
        rateAdaptionService.updateEstimatedNodeCount(currentNeighborCount);
        rateAdaptionService.updateAverageMessageSize(avgMessageSize);

        System.out.printf("Received beacon from %s | Neighbors: %d | Avg msg size: %.1f | Packet: %d bytes%n", beacon.sourceId(), currentNeighborCount, avgMessageSize, packet.getLength());
      }
    } catch (ClassNotFoundException e) {
      System.err.println("Unknown class in received beacon: " + e.getMessage());
    } catch (IOException e) {
      System.err.println("Error deserializing beacon: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("Unexpected error handling beacon: " + e.getMessage());
    }
  }
}