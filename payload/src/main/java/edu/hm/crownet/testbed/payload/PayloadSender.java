package edu.hm.crownet.testbed.payload;

public interface PayloadSender {

  void startSending(boolean useRateAdaption);

  void stopSending();
} 