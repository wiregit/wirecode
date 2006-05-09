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

package de.kapsi.net.kademlia.settings;


public class KademliaSettings extends LimeDHTProps {
    
    private KademliaSettings() {}
    
    /**
     * The replication parameter is also known as K
     */
    public static final IntSetting REPLICATION_PARAMETER
        = FACTORY.createIntSetting("REPLICATION_PARAMETER", 20);
    /**
     * The number of parallel lookups
     */
    public static final IntSetting LOOKUP_PARAMETER
        = FACTORY.createIntSetting("LOOKUP_PARAMETER", 5);
    /**
     * The FIND_NODE lookup timeout
     */
    public static final LongSetting NODE_LOOKUP_TIMEOUT
        = FACTORY.createLongSetting("NODE_LOOKUP_TIMEOUT",30L*1000L);
    /**
     * The FIND_VALUE lookup timeout
     */
    public static final LongSetting VALUE_LOOKUP_TIMEOUT
        = FACTORY.createLongSetting("VALUE_LOOKUP_TIMEOUT",45L*1000L);
    
    /**
     * Whether or not a value lookup is exhaustive
     */
    public static final BooleanSetting EXHAUSTIVE_VALUE_LOOKUP
        = FACTORY.createBooleanSetting("EXHAUSTIVE_VALUE_LOOKUP", true);
}
