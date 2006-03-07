/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.settings;

import java.util.prefs.Preferences;

public final class LookupSettings {
    
    private static final int MAX_LOOKUP_FAILURES = 3;
    private static final String MAX_LOOKUP_FAILURES_KEY = "MAX_LOOKUP_FAILURES";
    
    private static final long LOOKUP_TIMEOUT = 5000L;
    private static final String LOOKUP_TIMEOUT_KEY = "LOOKUP_TIMEOUT";
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(LookupSettings.class);

    private LookupSettings() {}
    
    public static int getK() {
        return KademliaSettings.getReplicationParameter();
    }
    
    public static int getA() {
        return KademliaSettings.getLookupParameter();
    }
    
    public static int getMaxLookupFailures() {
        return SETTINGS.getInt(MAX_LOOKUP_FAILURES_KEY, MAX_LOOKUP_FAILURES);
    }
    
    public static void setMaxFailureRounds(int maxFailures) {
        SETTINGS.putInt(MAX_LOOKUP_FAILURES_KEY, Math.max(0, maxFailures));
    }
    
    public static long getTimeout() {
        return SETTINGS.getLong(LOOKUP_TIMEOUT_KEY, LOOKUP_TIMEOUT);
    }
    
    public static void setTimeout(long timeout) {
        SETTINGS.putLong(LOOKUP_TIMEOUT_KEY, Math.max(0L, timeout));
    }
}
