/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
 
package com.limegroup.mojito.util;

import com.limegroup.mojito.KUID;

public class KUIDKeyCreator implements PatriciaTrie.KeyCreator<KUID> {
    
    private static final long serialVersionUID = 6412279289438108492L;

    public boolean isBitSet(KUID key, int bitIndex) {
        return key.isBitSet(bitIndex);
    }

    public int length() {
        return KUID.LENGTH_IN_BITS;
    }
    
    public int bitIndex(KUID key, KUID found) {
        if (found == null) {
            found = KUID.MIN_NODE_ID;
        }
        
        return key.bitIndex(found);
    }
}
