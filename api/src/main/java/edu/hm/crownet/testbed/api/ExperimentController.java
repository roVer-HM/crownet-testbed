package edu.hm.crownet.testbed.api;

import edu.hm.crownet.testbed.api.dto.ExperimentScheduleRequest;
import edu.hm.crownet.testbed.beacon.BeaconReceiver;
import edu.hm.crownet.testbed.beacon.BeaconSender;
import edu.hm.crownet.testbed.scheduler.Scheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class ExperimentController {

  private final BeaconSender beaconSender;
  private final BeaconReceiver beaconReceiver;
  private final Scheduler scheduler;

  @PostMapping("/schedule")
  public ResponseEntity<Void> schedule(@RequestBody ExperimentScheduleRequest request) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = request.getStartTime();
    LocalDateTime endTime = request.getEndTime();

    // Calculate delays
    long startDelay = java.time.Duration.between(now, startTime).toMillis();
    long endDelay = java.time.Duration.between(now, endTime).toMillis();

    // Schedule start task
    scheduler.scheduleOneShotTask("start", () -> {
      beaconReceiver.receive();
      beaconSender.send();
    }, startDelay);

    // Schedule stop task
    scheduler.scheduleOneShotTask("stop", () -> {
      beaconSender.stop();
      beaconReceiver.stop();
    }, endDelay);

    return ResponseEntity.ok().build();
  }
}
