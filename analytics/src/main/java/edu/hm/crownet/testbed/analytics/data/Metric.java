package edu.hm.crownet.testbed.analytics.data;

public record Metric(
        String sourceId,
        long timestamp,
        int neighborCount,
        double sendThroughput
) {
}

