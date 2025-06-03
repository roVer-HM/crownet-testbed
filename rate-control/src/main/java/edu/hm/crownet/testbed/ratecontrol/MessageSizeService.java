package edu.hm.crownet.testbed.ratecontrol;

/**
 * Interface for estimating the average size of messages over time.
 */
public interface MessageSizeService {

  /**
   * Returns the current estimate of the average message size.
   *
   * @return the average message size in bytes
   */
  double getAverageMessageSize();

  /**
   * Registers a new message size to update the average estimation.
   *
   * @param length the size of the message in bytes
   */
  void registerMessageSize(int length);
}
