package edu.hm.crownet.testbed.payload.data;

import java.io.Serializable;

/**
 * Represents a payload message sent by a node in the network. Contains the source ID of the sending node,
 * the timestamp of when the payload was sent, and the actual payload data.
 */
public record Payload(
        String sourceId,
        long timestamp,
        byte[] data
) implements Serializable {
}