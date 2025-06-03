package edu.hm.crownet.testbed.client.impl;

import edu.hm.crownet.testbed.client.UdpClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Lightweight implementation of {@link UdpClient} without internal threading.
 * The client provides blocking receive and broadcast methods.
 */
@Service
public class UdpClientImpl implements UdpClient {

  private static final int DEFAULT_SOCKET_TIMEOUT = 1000; // 1 second timeout
  private DatagramSocket socket;
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(int port) {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
      
      socket = new DatagramSocket(port);
      socket.setBroadcast(true);
      socket.setSoTimeout(DEFAULT_SOCKET_TIMEOUT);
      socket.setReuseAddress(true);
      
      System.out.println("UDP client initialized on port " + port);
    } catch (SocketException e) {
      throw new RuntimeException("Failed to initialize UDP client on port " + port, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatagramPacket receive() {
    if (socket == null || socket.isClosed()) {
      throw new IllegalStateException("UDP client not initialized or already closed");
    }
    
    try {
      byte[] receiveBuffer = new byte[1024];
      DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
      socket.receive(receivePacket);
      return receivePacket;
    } catch (IOException e) {
      throw new RuntimeException("Failed to receive UDP message", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void broadcast(DatagramPacket packet) {
    if (socket == null || socket.isClosed()) {
      throw new IllegalStateException("UDP client not initialized or already closed");
    }
    
    try {
      socket.send(packet);
    } catch (IOException e) {
      throw new RuntimeException("Failed to send UDP broadcast to " + 
                               packet.getAddress() + ":" + packet.getPort(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    if (socket != null && !socket.isClosed()) {
      socket.close();
      System.out.println("UDP client closed");
    }
  }
}