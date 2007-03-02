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
 * Miscellaneous Network settings
 */
public final class NetworkSettings extends MojitoProps {
    
    private NetworkSettings() {}
    
    /**
     * The amout of time we're waiting for a response
     * before giving up
     */
    public static final LongSetting TIMEOUT
        = FACTORY.createRemoteLongSetting("TIMEOUT", 10000L, "timeout", 10L, 30000L);
    
    /**
     * A multiplication factor for the RTT.
     */
    public static final IntSetting MIN_TIMEOUT_RTT_FACTOR
        = FACTORY.createRemoteIntSetting("MIN_TIMEOUT_RTT_FACTOR", 2, 
                "min_timeout_rtt_factor", 1, 10);
    
    /**
     * A multiplication factor for the RTT.
     */
    public static final LongSetting MIN_TIMEOUT_RTT
        = FACTORY.createRemoteLongSetting("MIN_TIMEOUT_RTT", 1000L, 
                "min_timeout_rtt", 10L, 30L*1000L);
    
    /**
     * The maximum number of errors (timeouts) that may occur 
     * before we're giving up to re-send requests
     */
    public static final IntSetting MAX_ERRORS
        = FACTORY.createRemoteIntSetting("MAX_ERRORS", 2, "max_errors", 1, 10);
    
    /**
     * The maximum size of a serialized message
     */
    public static final IntSetting MAX_MESSAGE_SIZE
        = FACTORY.createIntSetting("MAX_MESSAGE_SIZE", 1492);
    
    /**
     * The cleanup rate for Receipts
     */
    public static final LongSetting CLEANUP_RECEIPTS_DELAY
        = FACTORY.createLongSetting("CLEANUP_RECEIPTS_DELAY", 50L);
    
    /**
     * The buffer size for incoming messages
     */
    public static final IntSetting RECEIVE_BUFFER_SIZE
        = FACTORY.createIntSetting("RECEIVE_BUFFER_SIZE", 64*1024);
    
    /**
     * The buffer size for outgoing messages
     */
    public static final IntSetting SEND_BUFFER_SIZE
        = FACTORY.createIntSetting("SEND_BUFFER_SIZE", 64*1024);
    
    /**
     * Whether or not we're accepting forced addresses
     */
    public static final BooleanSetting ACCEPT_FORCED_ADDRESS
        = FACTORY.createBooleanSetting("ACCEPT_FORCED_ADDRESS", false);
    
    /**
     * Whether or not a new ByteBuffer should be allocated for
     * every message we're receiving
     */
    public static final BooleanSetting ALLOCATE_NEW_BUFFER
        = FACTORY.createBooleanSetting("ALLOCATE_NEW_BUFFER", false);
    
    /**
     * 
     */
    public static final LongSetting BOOTSTRAP_TIMEOUT
        = FACTORY.createRemoteLongSetting("BOOTSTRAP_TIMEOUT", 
                4L*60L*60L*1000L, "bootstrap_timeout", 60L*60L*1000L, 10L*60L*60L*1000L);
    
    /**
     * 
     */
    public static final LongSetting STORE_TIMEOUT
        = FACTORY.createRemoteLongSetting("STORE_TIMEOUT", 
                4L*60L*60L*1000L, "store_timeout", 60L*60L*1000L, 10L*60L*60L*1000L);
}
