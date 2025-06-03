package edu.hm.crownet.testbed.ratecontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OneHopNodeEstimatorImplTest {

  private OneHopNodeEstimatorImpl estimator;

  @BeforeEach
  void setUp() {
    estimator = new OneHopNodeEstimatorImpl();
  }

  @Test
  void shouldStartWithZeroNeighbors() {
    assertEquals(0, estimator.currentAmountOfNeighbours());
  }

  @Test
  void shouldCountOneNeighborAfterSingleBeacon() {
    estimator.registerBeacon("node-1");
    assertEquals(1, estimator.currentAmountOfNeighbours());
  }

  @Test
  void shouldCountMultipleDistinctNeighbors() {
    estimator.registerBeacon("node-1");
    estimator.registerBeacon("node-2");
    estimator.registerBeacon("node-3");

    assertEquals(3, estimator.currentAmountOfNeighbours());
  }

  @Test
  void shouldOverwriteTimestampForSameNode() throws InterruptedException {
    estimator.registerBeacon("node-1");
    Thread.sleep(10);
    estimator.registerBeacon("node-1");

    assertEquals(1, estimator.currentAmountOfNeighbours());
  }

  @Test
  void shouldRemoveExpiredNeighborsAfterWindow() throws InterruptedException {
    estimator.registerBeacon("node-1");

    // Wait longer than the 5-second window
    Thread.sleep(5100);

    assertEquals(0, estimator.currentAmountOfNeighbours());
  }

  @Test
  void shouldCleanUpStaleEntriesAutomatically() throws InterruptedException {
    estimator.registerBeacon("node-1");
    estimator.registerBeacon("node-2");
    estimator.registerBeacon("node-3");

    Thread.sleep(5100);

    // Triggers cleanup
    estimator.registerBeacon("node-4");

    assertEquals(1, estimator.currentAmountOfNeighbours());
  }
}