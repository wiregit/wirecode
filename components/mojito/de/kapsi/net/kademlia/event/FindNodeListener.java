/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.Collection;
import java.util.Map;

import de.kapsi.net.kademlia.KUID;

public interface FindNodeListener {
    
    /**
     * Called after a FIND_NODE lookup has finished.
     * 
     * @param lookup The ID we were looking for
     * @param nodes Collection of ContactNodes that were found (K closest to lookup ID sorted by closeness)
     * @param queryKeys Map of ContactNode -> QueryKeys
     * @param time Time in milliseconds
     */
    public void foundNodes(KUID lookup, Collection nodes, Map queryKeys, long time);
}
