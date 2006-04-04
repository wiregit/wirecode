/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public interface BootstrapListener {
    
    /**
     * Called after the inital bootstrap phase has finished. In the inital
     * phase we are looking for the K closest Nodes.
     * 
     * @param nodeId The NodeID we are looking for
     * @param nodes Collection of K closest ContactNodes sorted by closeness
     * @param time Time in milliseconds
     */
    public void initialPhaseComplete(KUID nodeId, Collection nodes, long time);
    
    /**
     * Called after the second and final bootstrap phase has finished. In 
     * the second phase we are refreshing the furthest away Buckets. 
     * 
     * @param time Time in milliseconds
     * @param foundNodes wheather or not Nodes were found
     */
    public void secondPhaseComplete(long time, boolean foundNodes);
}
