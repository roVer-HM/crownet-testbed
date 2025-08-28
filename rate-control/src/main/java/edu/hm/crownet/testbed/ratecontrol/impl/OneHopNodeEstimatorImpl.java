package edu.hm.crownet.testbed.ratecontrol.impl;

import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;

import java.util.concurrent.ConcurrentHashMap;

public class OneHopNodeEstimatorImpl implements NodeEstimatorService {

  private static final long WINDOW_MILLIS = 5000;

  private final ConcurrentHashMap<Integer, Long> neighborTimestamps = new ConcurrentHashMap<>();

  @Override
  public void registerBeacon(int sourceId) {
    long now = System.currentTimeMillis();
    neighborTimestamps.put(sourceId, now);
  }

  @Override
  public int currentAmountOfNeighbours() {
    long now = System.currentTimeMillis();
    neighborTimestamps.entrySet().removeIf(e -> (now - e.getValue()) > WINDOW_MILLIS);
    return neighborTimestamps.size();
  }
}