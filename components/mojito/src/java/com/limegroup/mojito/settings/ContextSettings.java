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
 * 
 */
public class ContextSettings extends MojitoProps {
    
    private ContextSettings() {}
    
    public static final LongSetting DISPATCH_EVENTS_EVERY
        = FACTORY.createLongSetting("DISPATCH_EVENTS_EVERY", 25L);
    
    public static final LongSetting ESTIMATE_NETWORK_SIZE_EVERY
        = FACTORY.createLongSetting("ESTIMATE_NETWORK_SIZE_EVERY", 60L * 1000L);
    
    public static final IntSetting MAX_LOCAL_HISTORY_SIZE
        = FACTORY.createIntSetting("MAX_LOCAL_HISTORY_SIZE", 20);
    
    public static final IntSetting MAX_REMOTE_HISTORY_SIZE
        = FACTORY.createIntSetting("MAX_REMOTE_HISTORY_SIZE", 10);
    
    public static final BooleanSetting COUNT_REMOTE_SIZE
        = FACTORY.createSettableBooleanSetting("COUNT_REMOTE_SIZE", true, "count_remote_size");
    
    public static final LongSetting SYNC_PING_TIMEOUT
        = FACTORY.createLongSetting("SYNC_PING_TIMEOUT", 60L * 1000L);
    
    public static final LongSetting SYNC_GET_VALUE_TIMEOUT
        = FACTORY.createLongSetting("SYNC_GET_VALUE_TIMEOUT", 60L * 1000L);
    
    public static final LongSetting SYNC_BOOTSTRAP_TIMEOUT
        = FACTORY.createLongSetting("SYNC_BOOTSTRAP_TIMEOUT", 3L * 60L * 1000L);
    
    public static final StringSetting MASTER_KEY
        = FACTORY.createStringSetting("MASTER_KEY", "public.key");
    
    public static final LongSetting NODE_ID_TIMEOUT
        = FACTORY.createSettableLongSetting("NODE_ID_TIMEOUT", /*30L**/60L*1000L, "node_id_timeout", 0L, 24L*60L*60L*1000L);
    
    public static final IntSetting VENDOR
        = FACTORY.createIntSetting("VENDOR", parseVendorID("LIME"));
    
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
    
    public static IntSetting VERSION
        = FACTORY.createIntSetting("VERSION", 0);
}
