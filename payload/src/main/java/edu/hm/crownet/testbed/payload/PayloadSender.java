package edu.hm.crownet.testbed.payload;

import java.time.LocalDateTime;

public interface PayloadSender {

  void startSending(boolean useRateAdaption);

  void stopSending();
  
  void scheduleSending(boolean useRateAdaption, LocalDateTime startTime, LocalDateTime endTime);
} 