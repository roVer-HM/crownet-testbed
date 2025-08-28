package edu.hm.crownet.testbed.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Sender {

  private DatagramSocket socket;

  public void initialize(int port) {
    try {
      socket = new DatagramSocket(port);
      socket.setBroadcast(true);
    } catch (SocketException e) {
      throw new RuntimeException("Failed to initialize UDP sender on port " + port, e);
    }
  }

  public void send(DatagramPacket packet) throws IOException {
    socket.send(packet);
  }

  public void close() {
    if (socket != null && !socket.isClosed()) {
      socket.close();
    }
  }
}
