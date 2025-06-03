package edu.hm.crownet.testbed.ratecontrol;

/**
 * Service interface for adaptive rate control.
 */
public interface RateAdaptionService {

  /**
   * Updates the estimated number of active nodes.
   *
   * @param nodeCount number of observed neighbor nodes
   */
  void updateEstimatedNodeCount(int nodeCount);

  /**
   * Updates the estimated average message size in bytes.
   *
   * @param newSize new average message size
   */
  void updateAverageMessageSize(double newSize);

  /**
   * Returns the current transmission interval in milliseconds.
   *
   * @return interval in ms
   */
  long obtainNextTransmissionTime();

  /**
   * Calculates the time delta for the next transmission based on the current
   * estimated node count and message size.
   *
   * @return time delta in milliseconds
   */
  long obtainDeltaT();
}