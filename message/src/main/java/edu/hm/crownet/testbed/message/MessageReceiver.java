package edu.hm.crownet.testbed.message;

public interface MessageReceiver {

  void stop();

  void receive();
}