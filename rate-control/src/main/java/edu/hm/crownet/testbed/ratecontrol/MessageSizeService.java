package edu.hm.crownet.testbed.ratecontrol;

/**
 * Service interface for estimating and updating the average size of messages.
 *
 * <p>This service allows for dynamic updates of the average message size based on newly
 * registered message sizes.
 */
public interface MessageSizeService {

  /**
   * Retrieves the current average message size in bytes.
   *
   * @return the average message size
   */
  double getAverageMessageSize();

  /**
   * Registers a new message size to update the average.
   *
   * @param length the size of the new message in bytes
   */
  void registerMessageSize(int length);
}
