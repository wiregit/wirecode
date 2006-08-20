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

package com.limegroup.mojito.statistics;

import java.util.HashMap;
import java.util.Map;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;

public class DHTStatsFactory {
    
    private static final Map<KUID, DHTNodeStat> STATS = new HashMap<KUID, DHTNodeStat>();
    
    public static synchronized DHTStats newInstance(Context context) {
        KUID nodeId = context.getLocalNodeID();
        DHTNodeStat stat = STATS.get(nodeId);
        if (stat == null) {
            stat = new DHTNodeStat(context);
            STATS.put(nodeId, stat);
        }
        
        assert (context == stat.context);
        return stat;
    }
    
    public static synchronized DHTStats getInstance(KUID nodeId) {
        return STATS.get(nodeId);
    }
    
    public static synchronized void clear() {
        STATS.clear();
    }
    
    private DHTStatsFactory() {}
}
