package edu.hm.crownet.testbed.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;

public class Receiver implements Runnable {

  private static final int DEFAULT_SOCKET_TIMEOUT = 1000;

  private final DatagramSocket socket;
  private final Consumer<DatagramPacket> packetHandler;

  private Thread receiveThread;
  private volatile boolean running = false;

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

  public void start() {
    running = true;
    receiveThread = new Thread(this, "UDP-Receiver-Thread");
    receiveThread.start();
  }

  public void stop() {
    running = false;
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
    if (receiveThread != null) {
      receiveThread.interrupt();
    }
  }

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
