package edu.hm.crownet.testbed.api;

import edu.hm.crownet.testbed.analytics.MetricsLogger;
import edu.hm.crownet.testbed.analytics.data.Metric;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class MetricsController {

  private final MetricsLogger metricsLogger;

  @GetMapping("/log")
  public ResponseEntity<List<Metric>> getBeaconLog(@RequestParam(name = "since", required = false, defaultValue = "0") long since) {
    var filtered = metricsLogger.getBeaconHistory().stream().filter(m -> m.timestamp() >= since).toList();
    return ResponseEntity.ok(filtered);
  }
}