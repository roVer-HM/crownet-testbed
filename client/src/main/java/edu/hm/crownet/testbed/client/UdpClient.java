package edu.hm.crownet.testbed.client;

import java.io.Closeable;
import java.net.DatagramPacket;

/**
 * Interface for a lightweight UDP client that supports receiving and broadcasting UDP packets.
 * The receive method is blocking and should be called in a separate thread if needed.
 */
public interface UdpClient extends Closeable {

  /**
   * Initializes the UDP client on the given port.
   *
   * @param port the port number to bind the socket to
   */
  void initialize(int port);

  /**
   * Receives a single UDP packet. This is a blocking call.
   *
   * @return the received {@link DatagramPacket}
   */
  DatagramPacket receive();

  /**
   * Sends the given UDP packet as a broadcast.
   *
   * @param packet the {@link DatagramPacket} to send
   */
  void broadcast(DatagramPacket packet);
}