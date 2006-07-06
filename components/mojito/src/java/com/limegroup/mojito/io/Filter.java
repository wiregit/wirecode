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
 
package com.limegroup.mojito.io;

import java.net.SocketAddress;

import com.limegroup.mojito.util.FixedSizeHashMap;


public class Filter {
    
    private FixedSizeHashMap<SocketAddress, DeltaCounter> counterMap;
    
    public Filter() {
        counterMap = new FixedSizeHashMap<SocketAddress, DeltaCounter>(64, 0.75f, true, 64);
    }
    
    public boolean allow(SocketAddress src) {
        DeltaCounter counter = counterMap.get(src);
        if (counter == null) {
            counter = new DeltaCounter();
            counterMap.put(src, counter);
        }
        return counter.allow();
    }
    
    private static class DeltaCounter {
        
        private static final long DELTA_T = 250;
        
        private static final int MAX_MESSAGES = 10;
        
        private long contact = 0;
        private int count = 0;
        
        public boolean allow() {
            long time = System.currentTimeMillis();
            long delta = time - contact;
            
            contact = time;

            if (count < MAX_MESSAGES && delta < DELTA_T) {
                return (++count < MAX_MESSAGES);
            } else if (count >= MAX_MESSAGES
                    && delta < Math.max(DELTA_T * count/MAX_MESSAGES, 750L)) {
                count++;
                return false;
            } else {
                count = 0;
            }
            
            return true;
        }
    }
}
