package edu.hm.crownet.testbed.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * UDP Sender that sends DatagramPackets to a specified address and port.
 */
public class Sender {

  /**
   * The DatagramSocket used for sending UDP packets.
   */
  private DatagramSocket socket;

  /**
   * Initializes the UDP sender by creating a DatagramSocket bound to the specified port.
   *
   * @param port the port to bind the socket to
   */
  public void initialize(int port) {
    try {
      socket = new DatagramSocket(port);
      socket.setBroadcast(true);
    } catch (SocketException e) {
      throw new RuntimeException("Failed to initialize UDP sender on port " + port, e);
    }
  }

  /**
   * Sends a DatagramPacket to the specified address and port.
   *
   * @param packet the DatagramPacket to send
   * @throws IOException if an I/O error occurs
   */
  public void send(DatagramPacket packet) throws IOException {
    socket.send(packet);
  }

  /**
   * Closes the DatagramSocket.
   */
  public void close() {
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
  }
}
