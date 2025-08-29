package edu.hm.crownet.testbed.message;

public interface MessageSender {

  void stop();

  void send(boolean useRateAdaption);
}