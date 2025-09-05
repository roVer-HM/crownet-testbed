package edu.hm.crownet.testbed.ratecontrol.impl;

import edu.hm.crownet.testbed.ratecontrol.RateAdaptionService;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for adaptive transmission rate control based on node density and message size,
 * implementing the ARC-DSA algorithm with timer reconsideration.
 */
public class RateAdaptionServiceImpl implements RateAdaptionService {

  /**
   * Minimum sending interval in milliseconds.
   * Ensures the transmission interval does not fall below a safety threshold.
   */
  private static final long MINIMUM_SENDING_INTERVAL_MILLIS = 100;

  /**
   * Correction factor for the randomized interval.
   */
  private static final double CORRECTION_FACTOR = 1.0 / (Math.E - 1.5);

  /**
   * Maximum application bandwidth in bytes per second.
   */
  private final double bandwidthBytesPerSec;

  /**
   * The estimated average packet size.
   */
  private double estimatedAvgPacketSize;

  /**
   * The estimated amount of nodes in the resource sharing domain. Initialized with one to include this node itself.
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

  // Calculate randomized and corrected interval T'(t) in milliseconds.
  private long calculateTPrime() {
    long base = calculateT();
    double randomized = ThreadLocalRandom.current().nextDouble(0.5 * base, 1.5 * base);
    double corrected = randomized * CORRECTION_FACTOR;
    return Math.max(MINIMUM_SENDING_INTERVAL_MILLIS, Math.round(corrected));
  }

  // Calculate deterministic interval T(t) in milliseconds.
  private long calculateT() {
    double tSeconds = (currentNodeEstimate * estimatedAvgPacketSize) / bandwidthBytesPerSec;
    return Math.round(tSeconds * 1000.0);
  }
}
