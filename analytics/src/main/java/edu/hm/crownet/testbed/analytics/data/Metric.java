package edu.hm.crownet.testbed.analytics.data;

/**
 * Represents a metric. Contains the source ID of the node, the timestamp of when
 * the metric was collected, the number of neighbors detected, and the send throughput.
 */
public record Metric(
        String sourceId,
        long timestamp,
        int neighborCount,
        double sendThroughput
) {
}

