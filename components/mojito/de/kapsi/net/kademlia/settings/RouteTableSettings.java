/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.settings;

import java.util.prefs.Preferences;

public final class RouteTableSettings {
    
    private static final int MAX_CACHE_SIZE = 1024;
    private static final String MAX_CACHE_SIZE_KEY = "MAX_CACHE_SIZE";
    
    private static final boolean SKIP_STALE = true;
    private static final String SKIP_STALE_KEY = "SKIP_STALE";
    
    private static final int MAX_NODE_FAILURES = 5;
    private static final String MAX_NODE_FAILURES_KEY = "MAX_NODE_FAILURES";
    
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
    
    public static int getMaxNodeFailures() {
        return SETTINGS.getInt(MAX_NODE_FAILURES_KEY, MAX_NODE_FAILURES);
    }
    
    public static void setMaxNodeFailures(int maxFailures) {
        SETTINGS.putInt(MAX_NODE_FAILURES_KEY, Math.max(0, maxFailures));
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
