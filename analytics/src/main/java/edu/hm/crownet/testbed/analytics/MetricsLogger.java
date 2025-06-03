package edu.hm.crownet.testbed.analytics;

import edu.hm.crownet.testbed.analytics.data.Metric;

import java.util.List;

public interface MetricsLogger {

  void logMetric(String sourceId, long timestamp, int neighborCount, double sendThroughput);

  List<Metric> getBeaconHistory();
}