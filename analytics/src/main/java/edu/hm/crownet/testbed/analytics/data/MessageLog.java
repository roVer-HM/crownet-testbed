package edu.hm.crownet.testbed.analytics.data;

import java.sql.Timestamp;

/**
 * Log entry for a single sent message.
 *
 * @param timestamp  send time (local clock, ms precision)
 * @param sourceId   unique ID of the sending node
 * @param sequenceNo message sequence number
 * @param sizeBytes  size of the message payload (application layer, bytes)
 */
public record MessageLog(
        Timestamp timestamp,
        int sourceId,
        int sequenceNo,
        int sizeBytes
) {
}
