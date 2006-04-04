/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.Collection;
import java.util.List;

public interface StoreListener {
    
    /**
     * Called after a store request was processed
     * 
     * @param keyValues List of KeyValues we have stored
     * @param nodes Collection of ContactNodes where the keyValues were sored
     */
    public void store(List keyValues, Collection nodes);
}
