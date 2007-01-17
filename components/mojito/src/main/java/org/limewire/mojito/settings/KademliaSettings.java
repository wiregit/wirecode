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
 * Various Kademlia related Settings
 */
public class KademliaSettings extends MojitoProps {
    
    private KademliaSettings() {}
    
    /**
     * The replication parameter is also known as K
     */
    public static final IntSetting REPLICATION_PARAMETER
        = FACTORY.createIntSetting("REPLICATION_PARAMETER", 20);
    
    /**
     * The number of parallel FIND_NODE lookups
     */
    public static final IntSetting FIND_NODE_PARALLEL_LOOKUPS
        = FACTORY.createRemoteIntSetting("FIND_NODE_PARALLEL_LOOKUPS", 5, 
                "find_node_parallel_lookups", 1, 15);
    
    /**
     * The number of parallel FIND_VALUE lookups
     */
    public static final IntSetting FIND_VALUE_PARALLEL_LOOKUPS
        = FACTORY.createRemoteIntSetting("FIND_VALUE_PARALLEL_LOOKUPS", 10, 
                "find_value_parallel_lookups", 1, 30);
    
    /**
     * The number of pings to send in parallel
     */
    public static final IntSetting PARALLEL_PINGS
        = FACTORY.createRemoteIntSetting("PARALLEL_PINGS", 15, 
                "parallel_pings", 1, 30);
    
    /**
     * The maximum number of ping failures before pinging is
     * given up
     */
    public static final IntSetting MAX_PARALLEL_PING_FAILURES
        = FACTORY.createIntSetting("MAX_PARALLEL_PING_FAILURES", 40);
    
    /**
     * The FIND_NODE lookup timeout
     */
    public static final LongSetting FIND_NODE_LOOKUP_TIMEOUT
        = FACTORY.createRemoteLongSetting("FIND_NODE_LOOKUP_TIMEOUT", 30L*1000L, 
                "find_node_lookup_timeout", 30L*1000L, 3L*60L*1000L);
    
    /**
     * The FIND_VALUE lookup timeout
     */
    public static final LongSetting FIND_VALUE_LOOKUP_TIMEOUT
        = FACTORY.createRemoteLongSetting("FIND_VALUE_LOOKUP_TIMEOUT", 45L*1000L, 
                "find_value_lookup_timeout", 45L*1000L, 4L*60L*1000L);
    
    /**
     * Whether or not a value lookup is exhaustive
     */
    public static final BooleanSetting EXHAUSTIVE_VALUE_LOOKUP
        = FACTORY.createBooleanSetting("EXHAUSTIVE_VALUE_LOOKUP", false);
    
    /**
     * The maximum number of bootstrap failures before bootstrapping 
     * is given up.
     */
    public static final IntSetting MAX_BOOTSTRAP_FAILURES
        = FACTORY.createIntSetting("MAX_BOOTSTRAP_FAILURES", 40);
    
    /**
     * The maximum number of parallel store requests
     */
    public static final IntSetting PARALLEL_STORES
        = FACTORY.createIntSetting("PARALLEL_STORES", 5);
    
    /**
     * A multiplier that is used to determinate the number
     * of Nodes to where we're sending shutdown messages.
     */
    public static final IntSetting SHUTDOWN_MULTIPLIER
        = FACTORY.createRemoteIntSetting("SHUTDOWN_MULTIPLIER", 2, 
                "shutdown_multiplier", 0, 20);
    
    /**
     * Whether or not the (k+1)-closest Contact should be
     * removed from the response Set
     */
    public static final BooleanSetting DELETE_FURTHEST_CONTACT
        = FACTORY.createBooleanSetting("DELETE_FURTHEST_CONTACT", true);
}
