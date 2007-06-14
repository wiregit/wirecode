/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.statistics;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StatisticsContext {
    
    private final Log log = LogFactory.getLog(getClass());
    
    private final DatabaseGroup database = new DatabaseGroup();
    
    private final LookupGroup FIND_NODE = new LookupGroup();
    
    private final FindValueGroup FIND_VALUE = new FindValueGroup();
    
    private final BasicGroup PING = new BasicGroup();
    
    private final StoreGroup STORE = new StoreGroup();
    
    private final StatsGroup STATS = new StatsGroup();
    
    private final NetworkGroup network = new NetworkGroup();
    
    private final RouteTableGroup routeTable = new RouteTableGroup();
    
    public DatabaseGroup getDatabaseGroup() {
        return database;
    }
    
    public LookupGroup getFindNodeGroup() {
        return FIND_NODE;
    }
    
    public FindValueGroup getFindValueGroup() {
        return FIND_VALUE;
    }
    
    public BasicGroup getPingGroup() {
        return PING;
    }
    
    public StoreGroup getStoreGroup() {
        return STORE;
    }
    
    public StatsGroup getStatsGroup() {
        return STATS;
    }
    
    public NetworkGroup getNetworkGroup() {
        return network;
    }
    
    public RouteTableGroup getRouteTableGroup() {
        return routeTable;
    }
    
    public void write(Writer out) throws IOException {
        for (Field field : getClass().getFields()) {
            try {
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                
                Object value = field.get(this);
                if (!(value instanceof StatsGroup)) {
                    continue;
                }
                
                StatsGroup group = (StatsGroup) value;
                group.write(out);
                
            } catch (IllegalAccessException err) {
                log.error("IllegalAccessException", err);
                continue;
            }
        }
    }
}
