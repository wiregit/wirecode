/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;


import java.util.Collection;

import de.kapsi.net.kademlia.db.KeyValue;

public interface StoreListener {
    public void store(KeyValue value, Collection nodes);
}
