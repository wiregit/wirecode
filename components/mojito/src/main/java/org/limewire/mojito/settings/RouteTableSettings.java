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
 
package org.limewire.mojito.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;

/**
 * Miscellaneous RouteTable related settings
 */
public final class RouteTableSettings extends MojitoProps {

    private RouteTableSettings() {}
    
    /**
     * The maximum number of Contacts we're keeping in the
     * Bucket replacement cache.
     */
    public static final IntSetting MAX_CACHE_SIZE
        = FACTORY.createRemoteIntSetting("MAX_CACHE_SIZE", 16, 
                "max_cache_size", 1, 256);
    
    /**
     * The maximum number of failures a node may have before beeing completely
     * evicted from the routing table. This also serves as a basis for the 
     * probability of a node to be included in the list of k closest nodes.
     */
    public static final IntSetting MAX_ACCEPT_NODE_FAILURES 
        = FACTORY.createRemoteIntSetting("MAX_ACCEPT_NODE_FAILURES", 20, 
                "max_accept_node_failures", 4, 200);
    
    /**
     * The maximum number of errors that may occur before an
     * alive Contact is considered as dead.
     */
    public static final IntSetting MAX_ALIVE_NODE_FAILURES
        = FACTORY.createRemoteIntSetting("MAX_ALIVE_NODE_FAILURES", 4, 
                "max_alive_node_failures", 4, 10);
   
    /**
     * The maximum number of errors that may occur before an
     * unknown Contact is considered as dead.
     */
    public static final IntSetting MAX_UNKNOWN_NODE_FAILURES
        = FACTORY.createRemoteIntSetting("MAX_UNKNOWN_NODE_FAILURES", 2, 
                "max_unknown_node_failures", 2, 10);
    
    /**
     * The minimum time that must pass since the last successful contact 
     * before we're contacting a Node for RouteTable maintenance reasons.
     */
    public static final LongSetting MIN_RECONNECTION_TIME
        = FACTORY.createRemoteLongSetting("MIN_RECONNECTION_TIME", 30L*1000L, 
                "min_reconnect_time", 0, 5L*60L*1000L);
    
    /**
     * The symbol size, i.e. the number of bits improved at each step.
     * Also known as parameter B
     */
    public static final IntSetting DEPTH_LIMIT
        = FACTORY.createRemoteIntSetting("DEPTH_LIMIT", 4, "depth_limit", 1, 16);
    
    /**
     * The period of the Bucket freshness
     */
    public static final LongSetting BUCKET_REFRESH_PERIOD
        = FACTORY.createRemoteLongSetting("BUCKET_REFRESH_PERIOD", 30L*60L*1000L, 
                "bucket_refresh_period", 10L*60L*1000L, 2L*60L*60L*1000L);
    
    /**
     * The delay of the RandomBucketRefresher
     */
    public static final LongSetting RANDOM_REFRESHER_DELAY
        = FACTORY.createRemoteLongSetting("RANDOM_REFRESHER_DELAY", 1L*60L*1000L, 
                "random_refresher_delay", 1L*60L*1000L, 2L*60L*60L*1000L);
    
    /**
     * This setting is primarily for testing. It makes sure that
     * the run-times of the RandomBucketRefreshers are uniformly
     * distributed. 
     */
    public static final BooleanSetting UNIFORM_BUCKET_REFRESH_DISTRIBUTION
        = FACTORY.createBooleanSetting("UNIFORM_BUCKET_REFRESH_DISTRIBUTION", false);
    
    /**
     * A minimum time (in sec) to pass before pinging the least recently
     * seen node of a bucket again
     */
    public static final LongSetting BUCKET_PING_LIMIT
        = FACTORY.createLongSetting("BUCKET_PING_LIMIT", 30L * 1000L);
    
    /**
     * The maximum number of consecutive failures that may occur
     * in a row before we're suspending all maintenance operations
     * (we're maybe no longer connected to the Internet and we'd
     * kill our RouteTable).
     */
    public static final IntSetting MAX_CONSECUTIVE_FAILURES
        = FACTORY.createIntSetting("MAX_CONSECUTIVE_FAILURES", 100);
}
