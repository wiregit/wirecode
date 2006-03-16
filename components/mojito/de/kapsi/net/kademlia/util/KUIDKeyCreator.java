/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.util;

import de.kapsi.net.kademlia.KUID;

public class KUIDKeyCreator implements PatriciaTrie.KeyCreator {
    
    public boolean isBitSet(Object key, int bitIndex) {
        return ((KUID)key).isBitSet(bitIndex);
    }

    public int length() {
        return KUID.LENGTH;
    }
    
    public int bitIndex(Object key, Object found) {
        if (found == null) {
            found = KUID.MIN_ID;
        }
        return ((KUID)key).bitIndex((KUID)found);
    }
}
