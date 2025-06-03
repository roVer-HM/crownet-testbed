package edu.hm.crownet.testbed.beacon.data;

import java.io.Serializable;

/**
 * Represents a beacon message sent by a node in the network. Contains the source ID of the sending node and
 * the timestamp of when the beacon was sent. Beacons are used for neighbor discovery.
 */
public record Beacon(
        String sourceId,
        long timestamp
) implements Serializable {
}
