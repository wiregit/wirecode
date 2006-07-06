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
 
package com.limegroup.mojito.settings;

import com.limegroup.gnutella.settings.BooleanSetting;
import com.limegroup.gnutella.settings.IntSetting;
import com.limegroup.gnutella.settings.LongSetting;

public final class RouteTableSettings extends MojitoProps {

    private RouteTableSettings() {}
    
    // TODO reasonable min and max values
    public static final IntSetting MAX_CACHE_SIZE
        = FACTORY.createSettableIntSetting("MAX_CACHE_SIZE", 16, "max_cache_size", 1, 256);
    
    public static final BooleanSetting SKIP_STALE
        = FACTORY.createBooleanSetting("SKIP_STALE", true);
    
    // TODO reasonable min and max values
    public static final IntSetting MAX_LIVE_NODE_FAILURES
        = FACTORY.createSettableIntSetting("MAX_LIVE_NODE_FAILURES", 4, "max_live_node_failures", 4, 10);
   
    // TODO reasonable min and max values
    public static final IntSetting MAX_UNKNOWN_NODE_FAILURES
        = FACTORY.createSettableIntSetting("MAX_UNKNOWN_NODE_FAILURES", 2, "max_unknown_node_failures", 2, 10);
    
    // TODO reasonable min and max values
    public static final LongSetting MIN_RECONNECTION_TIME
        = FACTORY.createSettableLongSetting("MIN_RECONNECTION_TIME", 1L*60L, "min_reconnect_time", 0, 1L*60L);
    
    /**
     * The symbol size, i.e. the number of bits improved at each step
     */
    public static final IntSetting DEPTH_LIMIT //a.k.a B
        = FACTORY.createSettableIntSetting("DEPTH_LIMIT", 4, "depth_limit", 1, 16);
    
    // TODO reasonable min and max values
    // 15 minutes for now
    public static final LongSetting BUCKET_REFRESH_TIME
        = FACTORY.createSettableLongSetting("BUCKET_REFRESH_TIME", 15L*60L*1000L, "bucket_refresh_time", 15L*60L*1000L, 15L*60L*1000L);
    
    public static final IntSetting MAX_CONSECUTIVE_FAILURES
        = FACTORY.createIntSetting("MAX_CONSECUTIVE_FAILURES", 100);
}
