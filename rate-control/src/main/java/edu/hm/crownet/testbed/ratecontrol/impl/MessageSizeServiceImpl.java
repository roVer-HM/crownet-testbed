package edu.hm.crownet.testbed.ratecontrol.impl;

import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;

/**
 * Implementation of the MessageSizeService that maintains an exponentially weighted moving average
 * of message sizes.
 *
 * <p>This implementation allows for dynamic updates of the average message size based on newly
 * registered message sizes. The smoothing factor (alpha) determines the weight given to new
 * measurements versus the existing average.
 */
public class MessageSizeServiceImpl implements MessageSizeService {

  /**
   * Smoothing factor for the exponentially weighted moving average (0 < alpha <= 1).
   */
  private final double alpha;

  /**
   * The current average message size in bytes.
   */
  private double averageMessageSize;

  public MessageSizeServiceImpl(double alpha, double initialBytes) {
    this.alpha = alpha;
    this.averageMessageSize = initialBytes;
  }

  @Override
  public synchronized double getAverageMessageSize() {
    return averageMessageSize;
  }

  @Override
  public synchronized void registerMessageSize(int length) {
    if (length < 0) return;
    averageMessageSize = alpha * length + (1.0 - alpha) * averageMessageSize;
  }
}