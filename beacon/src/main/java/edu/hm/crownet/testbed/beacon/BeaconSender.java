package edu.hm.crownet.testbed.beacon;

public interface BeaconSender {

  void startSending(boolean useRateAdaption);

  void stopSending();
}
