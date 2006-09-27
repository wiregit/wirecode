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

/**
 * Settings for the Database, DHTValue and for the DHTValueRepublisher 
 */
public final class DatabaseSettings extends MojitoProps {
    
    private DatabaseSettings() {}
    
    /**
     * The maximum number of Keys a single Node can store
     */
    public static final IntSetting MAX_DATABASE_SIZE
        = FACTORY.createSettableIntSetting("MAX_DATABASE_SIZE", 16384, 
                "max_database_size", 8192, 65536);
    
    /**
     * The maximum number of Values per Key a single Node can store
     */
    public static final IntSetting MAX_VALUES_PER_KEY
        = FACTORY.createSettableIntSetting("MAX_VALUES_PER_KEY", 5, 
                "max_values_per_key", 1, 10);
    
    /**
     * The time after a non-local value expires
     */
    public static final LongSetting VALUE_EXPIRATION_TIME
        = FACTORY.createSettableLongSetting("VALUE_EXPIRATION_TIME", 60L*60L*1000L, 
                "value_expiration_time", 30L*60L*1000L, 24L*60L*60L*1000L);
    
    /**
     * The lower bound republishing interval for a DHTValue. That
     * means a DHTValue cannot be republished more often than this
     * interval.
     */
    public static final LongSetting MIN_VALUE_REPUBLISH_INTERVAL
        = FACTORY.createLongSetting("MIN_VALUE_REPUBLISH_INTERVAL", 2L*60L*1000L);
    
    /**
     * The republishing interval in milliseconds.
     */
    public static final LongSetting VALUE_REPUBLISH_INTERVAL
        = FACTORY.createSettableLongSetting("VALUE_REPUBLISH_INTERVAL", 30L*60L*1000L, 
                "value_republish_interval", 3L*60L*1000L, 24L*60L*60L*1000L);
    
    /**
     * The period of the DHTValuePublisher
     */
    public static final LongSetting REPUBLISH_PERIOD
        = FACTORY.createSettableLongSetting("REPUBLISH_PERIOD", 5L*60L*1000L, 
                "republish_period", 5L*60L*1000L, 60L*60L*1000L);
    
    /**
     * Whether or not to delete a DHTValue from the Database if we're
     * the furthest of the k closest Nodes and a new Node comes along
     * that is nearer
     */
    // TODO: Set to false!!!
    public static final BooleanSetting DELETE_VALUE_IF_FURTHEST_NODE
        = FACTORY.createSettableBooleanSetting("DELETE_VALUE_IF_FURTHEST_NODE", false, 
                "delete_value_if_furthest_node");
}
