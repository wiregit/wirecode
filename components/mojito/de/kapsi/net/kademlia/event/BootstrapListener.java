/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public interface BootstrapListener {
    public void initialPhaseComplete(KUID nodeId, Collection nodes, long time);
    
    public void secondPhaseComplete(long time, boolean foundNodes);
}
