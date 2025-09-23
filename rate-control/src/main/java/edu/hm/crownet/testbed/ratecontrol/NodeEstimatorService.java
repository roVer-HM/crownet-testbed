package edu.hm.crownet.testbed.ratecontrol;

/**
 * Service interface for estimating the number of neighboring nodes.
 *
 * <p>This service maintains an estimate of the number of neighboring nodes based on received
 * beacons within a specified time window.
 */
public interface NodeEstimatorService {

  /**
   * Registers a received beacon from a neighboring node.
   *
   * @param sourceId the ID of the source node that sent the beacon
   */
  void registerBeacon(int sourceId);

  /**
   * Retrieves the current estimated number of one-hop neighboring nodes.
   *
   * @return the estimated number of neighboring nodes
   */
  int currentAmountOfNeighbours();
}
