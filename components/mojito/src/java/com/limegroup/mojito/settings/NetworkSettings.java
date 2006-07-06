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


public final class NetworkSettings extends MojitoProps {
    
    private NetworkSettings() {}
    
    public static final IntSetting PORT
        = FACTORY.createIntSetting("PORT", 31337);
    
    public static final LongSetting MAX_TIMEOUT
        = FACTORY.createSettableLongSetting("MAX_TIMEOUT", 10000L, "max_timeout", 10L, 30000L);
    
    public static final IntSetting MIN_TIMEOUT_RTT_FACTOR
        = FACTORY.createSettableIntSetting("MIN_TIMEOUT_RTT_FACTOR", 2, "min_timeout_rtt_factor", 1, 10);
    
    public static final BooleanSetting USE_RANDOM_MAX_ERRORS
        = FACTORY.createSettableBooleanSetting("USE_RANDOM_MAX_ERRORS", false, "use_random_max_errors"); 
    
    public static final IntSetting MIN_RETRIES
        = FACTORY.createSettableIntSetting("MIN_RETRIES", 0, "min_retries", 0, 10);
    
    public static final IntSetting MAX_ERRORS
        = FACTORY.createSettableIntSetting("MAX_ERRORS", 3, "max_errors", 0, 10);
    
    public static final BooleanSetting ALLOW_MULTIPLE_NODES
        = FACTORY.createBooleanSetting("ALLOW_MULTIPLE_NODES", false);
    
    public static final IntSetting MAX_MESSAGE_SIZE
        = FACTORY.createSettableIntSetting("MAX_MESSAGE_SIZE", 1492, "max_message_size", 512, 64*1024);
}
