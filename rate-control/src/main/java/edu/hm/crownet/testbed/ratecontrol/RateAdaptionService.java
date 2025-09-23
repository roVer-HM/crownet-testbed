package edu.hm.crownet.testbed.ratecontrol;

/**
 * Adapts the transmission interval based on current network conditions.
 *
 * <p>The service computes the next transmission time/interval using the
 * estimated number of active nodes and the average message size.
 */
public interface RateAdaptionService {

  /**
   * Updates the estimated number of active nodes in the network.
   *
   * @param nodeCount estimated active nodes (>= 0)
   */
  void updateEstimatedNodeCount(int nodeCount);

  /**
   * Updates the average application-layer message size used in calculations.
   *
   * @param sizeInBytes average message size in bytes (> 0)
   */
  void updateAverageMessageSize(double sizeInBytes);

  /**
   * Computes the absolute timestamp for the next transmission.
   *
   * @return next transmission time as an Instant
   */
  long obtainNextTransmissionTime(); // TODO - v.auricchi@hm.edu: Rename method to "computeNextTransmissionTime"

  /**
   * Computes the interval until the next transmission from now.
   *
   * @return time until next transmission as a Duration (non-negative)
   */
  long obtainDeltaT(); // TODO - v.auricchio@hm.edu: Rename to "computeNextInterval"
}