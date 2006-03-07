/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public interface FindValueListener {
    public void foundValue(KUID key, Collection values, long time);
}
