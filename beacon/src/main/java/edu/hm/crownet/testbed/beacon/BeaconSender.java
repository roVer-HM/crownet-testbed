package edu.hm.crownet.testbed.beacon;

import java.time.LocalDateTime;

public interface BeaconSender {

  void startSending(boolean useRateAdaption);

  void stopSending();
  
  void scheduleSending(boolean useRateAdaption, LocalDateTime startTime, LocalDateTime endTime);
}
