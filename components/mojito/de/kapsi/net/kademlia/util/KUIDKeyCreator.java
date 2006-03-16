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
            switch(((KUID)key).getType()) {
                case KUID.NODE_ID:
                    found = KUID.MIN_NODE_ID;
                    break;
                case KUID.VALUE_ID:
                    found = KUID.MIN_VALUE_ID;
                    break;
                case KUID.MESSAGE_ID:
                    found = KUID.MIN_MESSAGE_ID;
                    break;
                default:
                    found = KUID.MIN_UNKNOWN_ID;
                    break;
            }
        }
        
        return ((KUID)key).bitIndex((KUID)found);
    }
}
