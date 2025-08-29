package edu.hm.crownet.testbed.ratecontrol.impl;

import edu.hm.crownet.testbed.ratecontrol.MessageSizeService;

public class MessageSizeServiceImpl implements MessageSizeService {

  private final double alpha;
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