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

package org.limewire.mojito2.settings;

import java.util.concurrent.TimeUnit;

import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.TimeSetting;

/**
 * Settings for the bootstrapping process.
 */
public class BootstrapSettings extends MojitoProps {
    
    private BootstrapSettings() {}
    
    /**
     * The maximum amount of time the bootstrapping process can take
     * before it's interrupted.
     */
    public static final TimeSetting BOOTSTRAP_TIMEOUT
        = FACTORY.createRemoteTimeSetting("BOOTSTRAP_TIMEOUT", 
                8L, TimeUnit.MINUTES,
                "Mojito.BootstrapTimeout", 
                1L, TimeUnit.MINUTES, 
                30L, TimeUnit.MINUTES);
    
    /**
     * The IS_BOOTSTRAPPED_RATIO is used to determinate if a Node's RouteTable
     * is good enough to say it's bootstrapped.
     */
    public static final FloatSetting IS_BOOTSTRAPPED_RATIO
        = FACTORY.createRemoteFloatSetting("IS_BOOTSTRAPPED_RATIO", 
                0.5f, "Mojito.IsBootstrappedRatio", 0f, 1.0f);
    
    /**
     * Number of bootstrap workers to spawn.
     */
    public static final IntSetting BOOTSTRAP_WORKERS =
        FACTORY.createRemoteIntSetting("BOOSTRAP_WORKERS", 1, 
                "Mojito.BooststrapWorkers", 1, 5);
}
