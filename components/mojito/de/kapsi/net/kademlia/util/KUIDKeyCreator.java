/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package de.kapsi.net.kademlia.util;

import de.kapsi.net.kademlia.KUID;

public class KUIDKeyCreator implements PatriciaTrie.KeyCreator {
    
    private static final long serialVersionUID = 6412279289438108492L;

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
