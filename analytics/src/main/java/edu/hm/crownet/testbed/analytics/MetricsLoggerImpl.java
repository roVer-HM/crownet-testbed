package edu.hm.crownet.testbed.analytics;

import edu.hm.crownet.testbed.analytics.data.Metric;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MetricsLoggerImpl implements MetricsLogger {

  private static final int MAX_HISTORY_SIZE = 10_000;
  private static final long MAX_HISTORY_AGE_MS = 120_000;

  private final Deque<Metric> beaconHistory = new ArrayDeque<>();

  @Override
  public synchronized void logMetric(String sourceId, long timestamp, int neighborCount, double sendThroughput) {
    Metric metric = new Metric(sourceId, timestamp, neighborCount, sendThroughput);
    beaconHistory.addLast(metric);

    long cutoff = System.currentTimeMillis() - MAX_HISTORY_AGE_MS;
    while (!beaconHistory.isEmpty()) {
      Metric first = beaconHistory.peekFirst();
      if (beaconHistory.size() > MAX_HISTORY_SIZE || first.timestamp() < cutoff) {
        beaconHistory.removeFirst();
      } else {
        break;
      }
    }
  }

  @Override
  public synchronized List<Metric> getBeaconHistory() {
    return new ArrayList<>(beaconHistory);
  }
}