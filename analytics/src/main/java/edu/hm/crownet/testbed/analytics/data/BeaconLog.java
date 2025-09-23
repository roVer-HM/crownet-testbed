package edu.hm.crownet.testbed.analytics.data;

import java.sql.Timestamp;

/**
 * Log entry for a single sent beacon.
 *
 * @param timestamp  send time (local clock, ms precision)
 * @param sourceId   unique ID of the sending node
 * @param sequenceNo beacon sequence number
 * @param sizeBytes  size of the beacon payload (application layer, bytes)
 */
public record BeaconLog(
        Timestamp timestamp,
        int sourceId,
        int sequenceNo,
        int sizeBytes
) {
}