package edu.hm.crownet.testbed.ratecontrol.impl;

import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of the RateAdaptionService that adapts the sending rate based on estimated
 * bandwidth, average message size, and number of neighboring nodes.
 *
 * <p>This implementation calculates a base interval T for sending messages and then randomizes
 * it within a range to avoid synchronization issues. A correction factor is applied to ensure
 * that the average sending rate meets the desired bandwidth constraints.
 *
 * <p>The service maintains estimates of the average message size and the number of neighboring
 * nodes, which can be updated dynamically. The next transmission time or delta time until
 * the next transmission can be obtained through the provided methods.
 */
public class RateAdaptionServiceImpl implements RateAdaptionService {

  /**
   * Conversion factor from seconds to milliseconds.
   */
  private static final double MILLIS_PER_SECOND = 1000.0;

  /**
   * Minimum interval between sending messages in milliseconds to avoid excessive sending rates.
   */
  private static final long MINIMUM_SENDING_INTERVAL_MILLIS = 100;

  /**
   * Correction factor to adjust the randomized interval to achieve the desired average sending rate.
   * This factor is derived from the expected value of the uniform distribution used for randomization.
   */
  private static final double CORRECTION_FACTOR = 1.0 / (Math.E - 1.5);

  /**
   * The available bandwidth in bytes per second.
   */
  private final double bandwidthBytesPerSec;

  /**
   * The estimated average size of messages in bytes.
   */
  private double estimatedAvgPacketSize;

  /**
   * The current estimate of the number of neighboring nodes.
   */
  private int currentNodeEstimate = 1;

  private long lastPlannedTPrime = -1;

  public RateAdaptionServiceImpl(double bandwidthBytesPerSec, double initialEstimatedAvgPacketSize) {
    this.bandwidthBytesPerSec = bandwidthBytesPerSec;
    this.estimatedAvgPacketSize = initialEstimatedAvgPacketSize;
  }

  @Override
  public void updateEstimatedNodeCount(int nodeCount) {
    this.currentNodeEstimate = Math.max(1, nodeCount + 1);
  }

  @Override
  public void updateAverageMessageSize(double newSize) {
    if (newSize >= 0) {
      this.estimatedAvgPacketSize = newSize;
    }
  }

  @Override
  public long obtainNextTransmissionTime() {
    this.lastPlannedTPrime = calculateTPrime();
    return this.lastPlannedTPrime;
  }

  @Override
  public long obtainDeltaT() {
    if (lastPlannedTPrime < 0) {
      // No prior plan: schedule immediately based on current estimates
      lastPlannedTPrime = calculateTPrime();
      return 0;
    }

    long newTPrime = calculateTPrime();
    long deltaT = newTPrime - lastPlannedTPrime;

    if (deltaT <= 0) {
      // Send now
      lastPlannedTPrime = newTPrime;
      return 0;
    } else {
      lastPlannedTPrime = newTPrime;
      return deltaT;
    }
  }

  /*
   * Calculate randomized interval T' in milliseconds with correction factor.
   */
  private long calculateTPrime() {
    long base = calculateT();
    double randomized = ThreadLocalRandom.current().nextDouble(0.75 * base, 1.25 * base);
    double corrected = randomized * CORRECTION_FACTOR;
    return Math.max(MINIMUM_SENDING_INTERVAL_MILLIS, Math.round(corrected));
  }

  /*
   * Calculate base interval T in milliseconds based on current estimates.
   */
  private long calculateT() {
    double tSeconds = (currentNodeEstimate * estimatedAvgPacketSize) / bandwidthBytesPerSec;
    return Math.round(tSeconds * MILLIS_PER_SECOND);
  }
}
