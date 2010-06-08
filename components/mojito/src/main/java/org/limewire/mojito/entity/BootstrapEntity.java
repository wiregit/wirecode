package org.limewire.mojito.entity;

import org.limewire.mojito.DHT;

/**
 * A {@link BootstrapEntity} is the result of a DHT bootstrap operation.
 */
public interface BootstrapEntity extends Entity {

    /**
     * Returns the {@link DHT}
     */
    public DHT getDHT();
}
