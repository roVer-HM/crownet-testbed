package edu.hm.crownet.testbed.ratecontrol;

/**
 * Interface for estimating the number of nearby nodes based on received beacon messages.
 */
public interface NodeEstimatorService {

  /**
   * Registers a beacon received from a neighbor node.
   *
   * @param sourceId the unique ID of the sending node
   */
  void registerBeacon(String sourceId);

  /**
   * Returns the number of active neighbor within the defined time window.
   *
   * @return the number of neighboring nodes
   */
  int currentAmountOfNeighbours();
}
