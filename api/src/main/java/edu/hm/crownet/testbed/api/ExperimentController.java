package edu.hm.crownet.testbed.api;

import edu.hm.crownet.testbed.api.dto.ExperimentScheduleRequest;
import edu.hm.crownet.testbed.beacon.BeaconReceiver;
import edu.hm.crownet.testbed.beacon.BeaconSender;
import edu.hm.crownet.testbed.payload.PayloadReceiver;
import edu.hm.crownet.testbed.payload.PayloadSender;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class ExperimentController {

  private final BeaconSender beaconSender;
  private final BeaconReceiver beaconReceiver;
  private final PayloadSender payloadSender;
  private final PayloadReceiver payloadReceiver;

  @PostMapping("/start")
  public ResponseEntity<Void> start(@RequestParam(name = "rateAdaption", defaultValue = "true") boolean useRateAdaption) {
    beaconReceiver.startReceiving();
    beaconSender.startSending(useRateAdaption);
    payloadReceiver.startReceiving();
    payloadSender.startSending(useRateAdaption);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/schedule")
  public ResponseEntity<Void> schedule(@RequestBody ExperimentScheduleRequest request) {
    beaconReceiver.scheduleReceiving(request.getStartTime(), request.getEndTime());
    beaconSender.scheduleSending(request.isUseRateAdaption(), request.getStartTime(), request.getEndTime());
    payloadReceiver.scheduleReceiving(request.getStartTime(), request.getEndTime());
    payloadSender.scheduleSending(request.isUseRateAdaption(), request.getStartTime(), request.getEndTime());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/stop")
  public ResponseEntity<Void> stop() {
    beaconSender.stopSending();
    beaconReceiver.stopReceiving();
    payloadSender.stopSending();
    payloadReceiver.stopReceiving();
    return ResponseEntity.ok().build();
  }
}
