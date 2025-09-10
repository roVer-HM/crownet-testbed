package edu.hm.crownet.testbed.ratecontrol;

/**
 * Service interface for adapting the transmission rate based on network conditions.
 *
 * <p>This service adjusts the transmission interval based on the estimated number of active nodes
 * and the average message size to optimize network performance.
 */
public interface RateAdaptionService {

  /**
   * Updates the estimated number of active nodes in the network.
   *
   * @param nodeCount the new estimated number of nodes
   */
  void updateEstimatedNodeCount(int nodeCount);

  /**
   * Updates the average message size used for rate adaptation calculations.
   *
   * @param newSize the new average message size in bytes
   */
  void updateAverageMessageSize(double newSize);

  /**
   * Calculates the next transmission time based on the current estimated node count and message size.
   *
   * @return the next transmission time in milliseconds since epoch
   */
  long obtainNextTransmissionTime();

  /**
   * Calculates the delta time until the next transmission.
   *
   * @return the delta time in milliseconds
   */
  long obtainDeltaT();
}