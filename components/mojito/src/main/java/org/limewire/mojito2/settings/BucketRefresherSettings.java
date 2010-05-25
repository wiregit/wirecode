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

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.TimeSetting;

/**
 * Settings for the BucketRefresher.
 */
public class BucketRefresherSettings extends MojitoProps {

    private BucketRefresherSettings() {}
    
    /**
     * This setting is primarily for testing. It makes sure that
     * the run-times of the BucketRefreshers are uniformly
     * distributed. 
     */
    public static final BooleanSetting UNIFORM_BUCKET_REFRESH_DISTRIBUTION
        = FACTORY.createBooleanSetting("UNIFORM_BUCKET_REFRESH_DISTRIBUTION", false);
    
    /**
     * The delay of the BucketRefresher.
     */
    public static final TimeSetting BUCKET_REFRESHER_DELAY
        = FACTORY.createRemoteTimeSetting("BUCKET_REFRESHER_DELAY", 
                1L, TimeUnit.MINUTES,
                "Mojito.BucketRefresherDelay", 
                10L, TimeUnit.SECONDS, 
                2L, TimeUnit.HOURS);

    /**
     * Whether or not to ping all k-closest Nodes. Default is off (0L) and
     * it shouldn't be set to anything lower than say 5 minutes.
     */
    public static final TimeSetting BUCKET_REFRESHER_PING_NEAREST
        = FACTORY.createRemoteTimeSetting("BUCKET_REFRESHER_PING_NEAREST", 
                0L, TimeUnit.MILLISECONDS,
                "Mojito.BucketRefresherPingNearest", 
                0L, TimeUnit.MILLISECONDS,
                1L, TimeUnit.DAYS);
}
