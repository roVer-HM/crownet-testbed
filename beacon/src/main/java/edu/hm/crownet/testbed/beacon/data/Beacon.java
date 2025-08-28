package edu.hm.crownet.testbed.beacon.data;

import java.io.Serializable;

/**
 * Java Record representing a Beacon packet.
 * <p>
 * Fields:
 * - sequenceNumber: 16-bit sequence number
 * - sourceId: 32-bit source identifier
 * - timestampMs: 32-bit timestamp in milliseconds
 */
public record Beacon(
        short sequenceNumber,
        int sourceId,
        int timestampMs
) implements Serializable {
}