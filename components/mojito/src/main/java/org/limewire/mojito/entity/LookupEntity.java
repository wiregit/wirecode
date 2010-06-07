package org.limewire.mojito.entity;

import org.limewire.mojito.KUID;

/**
 * A {@link LookupEntity} is the result of a DHT <tt>FIND_NODE</tt> 
 * or <tt>FIND_VALUE</tt> operation.
 */
public interface LookupEntity extends Entity {

    /**
     * The lookup {@link KUID}.
     */
    public KUID getKey();
}
