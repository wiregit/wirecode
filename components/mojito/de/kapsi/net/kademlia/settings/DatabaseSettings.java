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

/**
 * Settings for Database, KeyValues and for the republisher 
 */
public final class DatabaseSettings extends LimeDHTProps {
    
    //public static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L; // 24 horus
    public static final long EXPIRATION_TIME_CLOSEST_NODE = 60L * 60L * 1000L; // 1 hour
    public static final long EXPIRATION_TIME_UNKNOWN = 30L * 60L * 1000L; // 30 mins
    
    public static final long MILLIS_PER_HOUR = EXPIRATION_TIME_CLOSEST_NODE / 24L;
    
    public static final String DATABASE_FILE = "Database.pat";
    
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
    
    /**
     * The republishing interval in milliseconds.
     * 
     * TODO it is currently set to 3 Minutes for TESTING purposes!
     *    CHANGE IT OR IT WILL SCREW US!
     *    
     * TODO reasonable min and max values
     */
    public static final LongSetting REPUBLISH_INTERVAL
        = FACTORY.createSettableLongSetting("REPUBLISH_INTERVAL", 30L*60L*1000L, "republish_interval", 3L*60L*1000L, 3L*60L*1000L);
}
