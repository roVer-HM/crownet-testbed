package edu.hm.crownet.testbed.ratecontrol.impl;

import edu.hm.crownet.testbed.ratecontrol.NodeEstimatorService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the NodeEstimatorService that estimates the number of one-hop neighboring nodes
 * based on received beacons within a specified time window.
 *
 * <p>This implementation maintains a mapping of neighbor node IDs to their last received beacon
 * timestamps. It periodically cleans up entries that are older than a defined time window to ensure
 * that only active neighbors are counted.
 */
public class OneHopNodeEstimatorImpl implements NodeEstimatorService {

  /**
   * Time window in milliseconds to consider a neighbor as active (5 seconds).
   */
  private static final long WINDOW_MILLIS = 5000;

  /**
   * A thread-safe map to store the last received beacon timestamp for each neighbor node ID.
   */
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