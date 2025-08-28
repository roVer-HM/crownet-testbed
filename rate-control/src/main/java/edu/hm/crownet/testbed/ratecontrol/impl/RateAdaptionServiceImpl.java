package edu.hm.crownet.testbed.ratecontrol.impl;

import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;

import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.exp;
import static java.lang.Math.max;

/**
 * Service for adaptive transmission rate control based on node density and message size,
 * implementing the ARC-DSA algorithm with timer reconsideration.
 */
public class RateAdaptionServiceImpl implements RateAdaptionService {

  /**
   * Minimum sending interval in milliseconds.
   * This is used to ensure that the transmission interval does not fall below a certain threshold.
   */
  private static final long MINIMUM_SENDING_INTERVAL_MILLIS = 100;

  /**
   * Correction factor for the randomized interval.
   * This is used to adjust the randomized transmission time to ensure it remains within a reasonable range.
   */
  private static final double CORRECTION_FACTOR = exp(-2.0 / 3.0);

  /**
   * Maximum application bandwidth in bytes per second.
   */
  private final double bandwidthBytesPerSec;

  private int currentNodeEstimate = 1;
  private double estimatedAvgPacketSize = -1;

  private long lastPlannedTPrime = -1;

  public RateAdaptionServiceImpl(double bandwidthBytesPerSec) {
    this.bandwidthBytesPerSec = bandwidthBytesPerSec;
  }

  @Override
  public void updateEstimatedNodeCount(int nodeCount) {
    this.currentNodeEstimate = Math.max(1, nodeCount);
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
      return 0; // fallback: send immediately for example on startup
    }

    long newTPrime = calculateTPrime();
    long deltaT = newTPrime - lastPlannedTPrime;

    if (deltaT <= 0) {
      return 0;
    } else {
      this.lastPlannedTPrime = newTPrime;
      return max(MINIMUM_SENDING_INTERVAL_MILLIS, deltaT);
    }
  }

  // Returns randomized and corrected interval in milliseconds.
  private long calculateTPrime() {
    long base = calculateT();
    double randomized = ThreadLocalRandom.current().nextDouble(0.5 * base, 1.5 * base);
    return (long) max(MINIMUM_SENDING_INTERVAL_MILLIS, randomized / CORRECTION_FACTOR);
  }

  // Returns base interval without randomization.
  private long calculateT() {
    if (estimatedAvgPacketSize <= 0) return MINIMUM_SENDING_INTERVAL_MILLIS;
    return (long) (((currentNodeEstimate * estimatedAvgPacketSize) / bandwidthBytesPerSec) * 1000.0);
  }
}