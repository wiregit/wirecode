/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;


import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public interface FindNodeListener {
    public void foundNodes(KUID lookup, Collection nodes, long time);
}
