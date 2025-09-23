package edu.hm.crownet.testbed.api;

import edu.hm.crownet.testbed.api.request.ExperimentScheduleRequest;
import edu.hm.crownet.testbed.beacon.BeaconReceiver;
import edu.hm.crownet.testbed.beacon.BeaconSender;
import edu.hm.crownet.testbed.message.MessageReceiver;
import edu.hm.crownet.testbed.message.MessageSender;
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
  private final MessageSender messageSender;
  private final MessageReceiver messageReceiver;
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
      beaconSender.send(request.isUseRateAdaption());
      messageReceiver.receive();
      messageSender.send(request.isUseRateAdaption());
    }, startDelay);

    // Schedule stop task
    scheduler.scheduleOneShotTask("stop", () -> {
      beaconSender.stop();
      beaconReceiver.stop();
      messageSender.stop();
      messageReceiver.stop();
    }, endDelay);

    System.out.printf("Experiment scheduled | Start=%s (in %d ms) | End=%s (in %d ms)%n", startTime, startDelay, endTime, endDelay);
    return ResponseEntity.ok().build();
  }
}
