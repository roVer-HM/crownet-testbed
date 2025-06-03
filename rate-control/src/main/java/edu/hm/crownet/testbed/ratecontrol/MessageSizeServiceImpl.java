package edu.hm.crownet.testbed.ratecontrol;

import org.springframework.stereotype.Service;

/**
 * Service for calculating the average message size using an exponential moving average.
 */
@Service
public class MessageSizeServiceImpl implements MessageSizeService {

  /**
   * Estimated average message size in bytes.
   */
  private double averageMessageSize;

  /**
   * Returns the current estimate of the average message size.
   *
   * @return the average message size in bytes
   */
  @Override
  public double getAverageMessageSize() {
    return this.averageMessageSize;
  }

  /**
   * Registers a new observed message size and updates the estimate
   * using exponential smoothing.
   *
   * @param length the length of the new message in bytes
   */
  @Override
  public void registerMessageSize(int length) {
    double smoothingFactor = 0.1;
    this.averageMessageSize = smoothingFactor * length + (1 - smoothingFactor) * this.averageMessageSize;
  }
}
