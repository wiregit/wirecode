/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.settings;

import java.util.prefs.Preferences;

public final class DatabaseSettings {
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(DatabaseSettings.class);
    
    private static final int MAX_SIZE = 16384;
    private static final String MAX_SIZE_KEY = "MAX_SIZE";
    
    private static final int MAX_VALUES = 5;
    private static final String MAX_VALUES_KEY = "MAX_VALUES";
    
    //public static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L; // 24 horus
    public static final long MILLIS_PER_DAY = 60L * 60L * 1000L; // 1 hour
    
    public static final long MILLIS_PER_HOUR = MILLIS_PER_DAY / 24L;
    
    public static final long REPUBLISH_INTERVAL = 3L * 60L * 1000L; // 3 minutes
    
    public static final String DATABASE_FILE = "Database.pat";
    
    private DatabaseSettings() {}
    
    public static int getMaxSize() {
        return SETTINGS.getInt(MAX_SIZE_KEY, MAX_SIZE);
    }
    
    public static void setMaxSize(int maxSize) {
        SETTINGS.putInt(MAX_SIZE_KEY, maxSize);
    }
    
    public static int getMaxValues() {
        return SETTINGS.getInt(MAX_VALUES_KEY, MAX_VALUES);
    }
    
    public static void setMaxValues(int maxValues) {
        SETTINGS.putInt(MAX_VALUES_KEY, maxValues);
    }
}
