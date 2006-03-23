/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.Collection;
import java.util.Map;

import de.kapsi.net.kademlia.KUID;

public interface FindNodeListener {
    public void foundNodes(KUID lookup, Collection nodes, Map queryKeys, long time);
}
