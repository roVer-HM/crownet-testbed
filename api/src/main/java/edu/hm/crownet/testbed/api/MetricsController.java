package edu.hm.crownet.testbed.api;

import edu.hm.crownet.testbed.analytics.BeaconLog;
import edu.hm.crownet.testbed.analytics.BeaconLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class MetricsController {

  @Value("${crownet.testbed.adhoc.node-id}")
  private int sourceId;

  @GetMapping("/beacons")
  public ResponseEntity<List<BeaconLog>> getLogs() {
    try {
      Path logPath = Path.of("/var/log/crownet", "beacons-node-" + sourceId + ".csv");
      BeaconLogger logger = new BeaconLogger(logPath.toString());
      List<BeaconLog> logs = logger.readAll();
      logger.close();
      return ResponseEntity.ok(logs);
    } catch (IOException e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  @DeleteMapping("/beacons")
  public ResponseEntity<Void> clearLogs() {
    try {
      Path logPath = Path.of("/var/log/crownet", "beacons-node-" + sourceId + ".csv");
      BeaconLogger logger = new BeaconLogger(logPath.toString());
      logger.clear();
      logger.close();
      return ResponseEntity.noContent().build();
    } catch (IOException e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}