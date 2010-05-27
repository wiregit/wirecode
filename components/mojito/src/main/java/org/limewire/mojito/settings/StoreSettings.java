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

import java.util.concurrent.TimeUnit;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.TimeSetting;

public class StoreSettings extends MojitoProps {

    private StoreSettings() {}
    
    /**
     * Whether or not store-forwarding of values is enabled.
     */
    public static final BooleanSetting STORE_FORWARD_ENABLED
        = FACTORY.createRemoteBooleanSetting("STORE_FORWARD_ENABLED", 
                true, "Mojito.StoreForwardEnabled");

    /**
     * Whether or not SecurityTokens are required for storing values.
     */
    public static final BooleanSetting STORE_REQUIRES_SECURITY_TOKEN
        = FACTORY.createBooleanSetting("STORE_REQUIRES_SECURITY_TOKEN", true);

    /**
     * The maximum number of parallel store requests.
     */
    public static final IntSetting PARALLEL_STORES
        = FACTORY.createIntSetting("PARALLEL_STORES", 5);
    
    /**
     * The maximum amount of time the store process can take
     * before it's interrupted.
     */
    public static final TimeSetting STORE_TIMEOUT
        = FACTORY.createRemoteTimeSetting("STORE_TIMEOUT", 
                4L, TimeUnit.MINUTES,
                "Mojito.StoreTimeout", 
                60L, TimeUnit.SECONDS,
                10L, TimeUnit.MINUTES);
}
