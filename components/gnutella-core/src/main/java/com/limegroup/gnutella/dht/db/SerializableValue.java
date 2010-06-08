package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.Value;

/**
 * A value that is serializable into a {@link Value}.
 */
public interface SerializableValue {

    /**
     * Turns this value into a DHT {@link Value}.
     */
    public Value serialize();
}
