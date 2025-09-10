package edu.hm.crownet.testbed.message.data;

import java.io.Serializable;

/**
 * Java Record representing a message packet.
 * The size of one message is fixed at 11200 bits (1400 B).
 * <p>
 * Fields:
 * - sequenceNumber: 16 bit (2 B) sequence number
 * - sourceId: 32 bit (4 B) source identifier
 * - timestampMs: 32 bit (4 B) timestamp in milliseconds
 * - payload: 11120 bit (1390 B) dummy extension to reach 1400 B total size; reserved for padding in this prototype
 */
public record Message(
        short sequenceNumber,
        int sourceId,
        int timestampMs,
        byte[] payload
) implements Serializable {
}