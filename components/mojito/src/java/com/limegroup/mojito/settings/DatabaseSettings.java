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
 * Settings for Database, KeyValues and for the republisher 
 */
public final class DatabaseSettings extends MojitoProps {
    
    private DatabaseSettings() {}
    
    /**
     * The maximum number of Keys a single Node can store
     * 
     * TODO reasonable min and max values
     */
    public static final IntSetting MAX_DATABASE_SIZE
        = FACTORY.createSettableIntSetting("MAX_DATABASE_SIZE", 16384, "max_database_size", 16384, 65536);
    
    /**
     * The maximum number of Values per Key a single Node can store
     * 
     * TODO reasonable min and max values
     */
    public static final IntSetting MAX_KEY_VALUES
        = FACTORY.createSettableIntSetting("MAX_KEY_VALUES", 5, "max_key_values", 1, 10);
    
    /**
     * The maximum number of KeyValues that can be store-forwarded.
     * 
     * TODO reasonable min and max values
     */
    public static final IntSetting MAX_STORE_FORWARD
        = FACTORY.createSettableIntSetting("MAX_STORE_FORWARD", 16384, "max_store_forward", 16384, 65536);
    
    /**
     * The maximum number of KeyValues a Node can request or send
     * at once.
     * 
     * TODO reasonable min and max values
     */
    public static final IntSetting MAX_STORE_FORWARD_ONCE
        = FACTORY.createSettableIntSetting("MAX_STORE_FORWARD_ONCE", 5, "max_store_forward_once", 1, 10);
    
    public static final LongSetting EXPIRATION_TIME_CLOSEST_NODE
        = FACTORY.createLongSetting("EXPIRATION_TIME_CLOSEST_NODE", 60L * 60L * 1000L); //1 hour
    
    public static final LongSetting EXPIRATION_TIME_UNKNOWN
        = FACTORY.createLongSetting("EXPIRATION_TIME_UNKNOWN", EXPIRATION_TIME_CLOSEST_NODE.getValue()/2); //30 min
    
    public static final LongSetting MIN_REPUBLISH_INTERVAL
        = FACTORY.createLongSetting("MIN_REPUBLISH_INTERVAL", 2L * 60L * 1000L);
    
    /**
     * The republishing interval in milliseconds.
     * 
     * TODO reasonable min and max values
     */
    public static final LongSetting REPUBLISH_INTERVAL
        = FACTORY.createSettableLongSetting("REPUBLISH_INTERVAL", EXPIRATION_TIME_CLOSEST_NODE.getValue()/2 , 
                "republish_interval", 3L*60L*1000L, 3L*60L*1000L);
    
    public static final LongSetting REPUBLISH_PERIOD
        = FACTORY.createSettableLongSetting("REPUBLISH_PERIOD", 5L*60L*1000L, 
                "republish_period", 5L*60L*1000L, 30L*60L*1000L);
    
    /**
     * 
     */
    public static final BooleanSetting SIGN_KEY_VALUES
        = FACTORY.createBooleanSetting("SIGN_KEY_VALUES", false);
}
