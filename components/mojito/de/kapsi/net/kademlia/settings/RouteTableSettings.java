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

import java.util.prefs.Preferences;

public final class RouteTableSettings {
    
    private static final int MAX_CACHE_SIZE = 32;
    private static final String MAX_CACHE_SIZE_KEY = "MAX_CACHE_SIZE";
    
    private static final boolean SKIP_STALE = true;
    private static final String SKIP_STALE_KEY = "SKIP_STALE";
    
    private static final int MAX_LIVE_NODE_FAILURES = 4;
    private static final String MAX_LIVE_NODE_FAILURES_KEY = "MAX_LIVE_NODE_FAILURES";
    
    private static final int MAX_UNKNOWN_NODE_FAILURES = 2;
    private static final String MAX_UNKNOWN_NODE_FAILURES_KEY = "MAX_UNKNOWN_NODE_FAILURES";
    
    private static final int DEPTH_LIMIT = 4; //a.k.a B
    private static final String DEPTH_LIMIT_KEY = "DEPTH_LIMIT";
    
    private static final long BUCKET_REFRESH_TIME = 15 * 60* 1000; //15 minutes for now
    private static final String BUCKET_REFRESH_TIME_KEY = "BUCKET_REFRESH_TIME";
    
    public static final String ROUTETABLE_FILE = "RouteTable.pat";
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(RouteTableSettings.class);
    
    private RouteTableSettings() {}
    
    public static int getMaxCacheSize() {
        return SETTINGS.getInt(MAX_CACHE_SIZE_KEY, MAX_CACHE_SIZE);
    }
    
    public static void setMaxCacheSize(int maxCacheSize) {
        SETTINGS.putInt(MAX_CACHE_SIZE_KEY, maxCacheSize);
    }
    
    public static boolean getSkipStale() {
        return SETTINGS.getBoolean(SKIP_STALE_KEY, SKIP_STALE);
    }
    
    public static void setSkipStale(boolean skipStale) {
        SETTINGS.putBoolean(SKIP_STALE_KEY, skipStale);
    }
    
    public static int getMaxLiveNodeFailures() {
        return SETTINGS.getInt(MAX_LIVE_NODE_FAILURES_KEY, MAX_LIVE_NODE_FAILURES);
    }
    
    public static void setMaxLiveNodeFailures(int maxFailures) {
        SETTINGS.putInt(MAX_LIVE_NODE_FAILURES_KEY, Math.max(0, maxFailures));
    }
    
    public static int getMaxUnknownNodeFailures() {
        return SETTINGS.getInt(MAX_UNKNOWN_NODE_FAILURES_KEY, MAX_UNKNOWN_NODE_FAILURES);
    }
    
    public static void setMaxUnknownNodeFailures(int maxFailures) {
        SETTINGS.putInt(MAX_UNKNOWN_NODE_FAILURES_KEY, Math.max(0, maxFailures));
    }
    
    public static int getDepthLimit() {
        return SETTINGS.getInt(DEPTH_LIMIT_KEY, DEPTH_LIMIT);
    }
    
    public static void setDepthLimit(int symbolSize) {
        SETTINGS.putInt(DEPTH_LIMIT_KEY,Math.max(0, symbolSize));
    }
    
    public static long getBucketRefreshTime() {
        return SETTINGS.getLong(BUCKET_REFRESH_TIME_KEY, BUCKET_REFRESH_TIME);
    }
    
    public static void setBucketRefreshTime(long time) {
        SETTINGS.putLong(BUCKET_REFRESH_TIME_KEY,time);
    }
}
