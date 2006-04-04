/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public interface FindValueListener {
    
    /**
     * Called after a FIND_VALUE lookup has finished.
     * 
     * @param key The key we were looking for
     * @param values Collection of KeyValues
     * @param time Time in milliseconds
     */
    public void foundValue(KUID key, Collection values, long time);
}
