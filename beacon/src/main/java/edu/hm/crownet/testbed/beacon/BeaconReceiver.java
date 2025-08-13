package edu.hm.crownet.testbed.beacon;

import java.time.LocalDateTime;

public interface BeaconReceiver {

    void startReceiving();

    void stopReceiving();
    
    void scheduleReceiving(LocalDateTime startTime, LocalDateTime endTime);
}
