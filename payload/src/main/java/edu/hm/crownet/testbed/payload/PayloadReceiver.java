package edu.hm.crownet.testbed.payload;

import java.time.LocalDateTime;

public interface PayloadReceiver {

  void startReceiving();

  void stopReceiving();
  
  void scheduleReceiving(LocalDateTime startTime, LocalDateTime endTime);
} 