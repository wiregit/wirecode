/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.Collection;
import java.util.List;

public interface StoreListener {
    public void store(List keyValues, Collection nodes);
}
