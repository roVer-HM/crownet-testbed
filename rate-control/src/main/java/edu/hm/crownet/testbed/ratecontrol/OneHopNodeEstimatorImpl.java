package edu.hm.crownet.testbed.ratecontrol;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-hop node estimator implementation.
 * <p>
 * Estimates the number of neighbor nodes in direct communication range
 * based on received beacon messages. Beacons are timestamped and counted
 * only if received within a fixed time window.
 */
@Service
public class OneHopNodeEstimatorImpl implements NodeEstimatorService {

  /**
   * Time window (in milliseconds) to consider a neighbor as active.
   */
  private static final long WINDOW_MILLIS = 5000;

  /**
   * Stores the last seen timestamp (in ms) for each neighbor ID.
   */
  private final Map<Integer, Long> neighborTimestamps = new ConcurrentHashMap<>();

  /**
   * Registers a received beacon from a neighbor node.
   *
   * @param sourceId the unique identifier of the sending node
   */
  @Override
  public void registerBeacon(int sourceId) {
    long now = Instant.now().toEpochMilli();
    neighborTimestamps.put(sourceId, now);
  }

  /**
   * Returns the number of currently active 1-hop neighbors
   * based on the configured time window.
   *
   * @return the count of distinct neighbor nodes seen within the time window
   */
  @Override
  public int currentAmountOfNeighbours() {
    long now = Instant.now().toEpochMilli();

    // Clean up outdated entries to free memory
    neighborTimestamps.entrySet().removeIf(entry ->
            now - entry.getValue() > WINDOW_MILLIS
    );

    // Count only valid (recent) entries
    return (int) neighborTimestamps.entrySet().stream()
            .filter(entry -> now - entry.getValue() <= WINDOW_MILLIS)
            .map(Map.Entry::getKey)
            .distinct()
            .count();
  }
}
