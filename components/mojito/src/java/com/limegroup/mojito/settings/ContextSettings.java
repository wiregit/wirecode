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

import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.settings.BooleanSetting;
import com.limegroup.gnutella.settings.IntSetting;
import com.limegroup.gnutella.settings.LongSetting;
import com.limegroup.gnutella.settings.StringSetting;

/**
 * Misc Context Settings
 */
public class ContextSettings extends MojitoProps {
    
    private ContextSettings() {}
    
    /**
     * The time interval to compute the estimated Network size
     */
    public static final LongSetting ESTIMATE_NETWORK_SIZE_EVERY
        = FACTORY.createLongSetting("ESTIMATE_NETWORK_SIZE_EVERY", 60L*1000L);
    
    /**
     * The maximum number of locally estimated Network sizes to
     * keep in Memory and to use as basis for the local estimation.
     */
    public static final IntSetting MAX_LOCAL_HISTORY_SIZE
        = FACTORY.createIntSetting("MAX_LOCAL_HISTORY_SIZE", 20);
    
    /**
     * The maximum number of remotely estimated Network sizes to
     * keep in Memory and to use as basis for the local estimation.
     */
    public static final IntSetting MAX_REMOTE_HISTORY_SIZE
        = FACTORY.createIntSetting("MAX_REMOTE_HISTORY_SIZE", 10);
    
    /**
     * Whether or not to estimate the Network size
     */
    public static final BooleanSetting COUNT_REMOTE_SIZE
        = FACTORY.createSettableBooleanSetting("COUNT_REMOTE_SIZE", true, "count_remote_size");
    
    /**
     * The name of the master key file
     */
    public static final StringSetting MASTER_KEY
        = FACTORY.createStringSetting("MASTER_KEY", "public.key");
    
    /**
     * The maximum time a Node may stay inactive before
     * the Node ID becomes invalid a new ID is generated.
     */
    public static final LongSetting NODE_ID_TIMEOUT
        = FACTORY.createSettableLongSetting("NODE_ID_TIMEOUT", 14L*24L*60L*60L*1000L, 
                "node_id_timeout", 0L, 14L*24L*60L*60L*1000L);
    
    /**
     * The maximum time to wait on an Object
     */
    public static final LongSetting WAIT_ON_LOCK
        = FACTORY.createLongSetting("WAIT_ON_LOCK", 3L*60L*1000L);
    
    /**
     * This Node's Vendor code
     */
    public static final IntSetting VENDOR
        = FACTORY.createIntSetting("VENDOR", parseVendorID("LIME"));
    
    /**
     * This Node's Version
     */
    public static final IntSetting VERSION
        = FACTORY.createIntSetting("VERSION", 0);
    
    /**
     * A helper method to convert a 4 character ASCII String
     * into an Interger
     */
    public static int parseVendorID(String vendorId) {
        char[] chars = vendorId.toCharArray();
        if (chars.length != 4) {
            throw new IllegalArgumentException("VendorID must be 4 characters");
        }
        
        int id = 0;
        for(char c : chars) {
            id = (id << 8) | (int)(c & 0xFF);
        }
        return id;
    }
    
    /**
     * A helper method to convert each of vendorId's 4 bytes
     * into an ASCII character and to return them as String
     */
    public static String toVendorString(int vendorId) {
        try {
            byte[] name = new byte[]{
                (byte)((vendorId >> 24) & 0xFF),
                (byte)((vendorId >> 16) & 0xFF),
                (byte)((vendorId >>  8) & 0xFF),
                (byte)((vendorId      ) & 0xFF)
            };
            return new String(name, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
