package edu.hm.crownet.testbed.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;

/**
 * UDP Receiver that listens for incoming DatagramPackets on a specified port.
 * It runs in its own thread and processes received packets using a provided handler.
 */
public class Receiver implements Runnable {

  /**
   * Default socket timeout in milliseconds to allow periodic checks of the running flag.
   */
  private static final int DEFAULT_SOCKET_TIMEOUT = 1000;

  /**
   * The DatagramSocket used for receiving UDP packets.
   */
  private final DatagramSocket socket;

  /**
   * Handler to process received packets.
   */
  private final Consumer<DatagramPacket> packetHandler;

  /**
   * Thread for receiving packets.
   */
  private Thread receiveThread;

  /**
   * Flag to control the running state of the receiver.
   */
  private volatile boolean running = false;

  /**
   * Constructs a UDP Receiver that listens on the specified port and uses the given handler to process incoming packets.
   *
   * @param port    the port to listen on
   * @param handler a Consumer to handle received DatagramPackets
   */
  public Receiver(int port, Consumer<DatagramPacket> handler) {
    try {
      this.socket = new DatagramSocket(port);
      this.socket.setBroadcast(true);
      this.socket.setSoTimeout(DEFAULT_SOCKET_TIMEOUT);
      this.socket.setReuseAddress(true);
      this.packetHandler = handler;
    } catch (SocketException e) {
      throw new RuntimeException("Failed to initialize UDP receiver on port " + port, e);
    }
  }

  /**
   * Starts the receiver thread.
   */
  public void start() {
    running = true;
    receiveThread = new Thread(this, "UDP-Receiver-Thread");
    receiveThread.start();
  }

  /**
   * Stops the receiver thread and closes the socket.
   */
  public void stop() {
    running = false;
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
    if (receiveThread != null) {
      receiveThread.interrupt();
    }
  }

  /**
   * Main loop for receiving UDP packets.
   */
  @Override
  public void run() {
    while (running && socket != null && !socket.isClosed()) {
      try {
        byte[] receiveBuffer = new byte[1500];  // adjust size as needed
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);
        if (packetHandler != null) {
          packetHandler.accept(receivePacket);
        }
      } catch (SocketTimeoutException ignore) {
        // Check running flag again and continue
      } catch (IOException e) {
        if (running) {
          System.err.println("UDP receive error: " + e.getMessage());
        }
      }
    }
  }
}
