/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.settings;

import java.util.prefs.Preferences;

public final class NetworkSettings {
    
    private static final int PORT = 31337;
    private static final String PORT_KEY = "PORT";
    
    private static final long TIMEOUT = 10L * 1000L;
    private static final String TIMEOUT_KEY = "TIMEPUT";
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(NetworkSettings.class);
    
    private NetworkSettings() {}
    
    public static int getPort() {
        return SETTINGS.getInt(PORT_KEY, PORT);
    }
    
    public static void setPort(int port) {
        SETTINGS.putInt(PORT_KEY, Math.max(0, Math.min(port, 0xFFFF)));
    }
    
    public static long getTimeout() {
        return SETTINGS.getLong(TIMEOUT_KEY, TIMEOUT);
    }
    
    public static void setTimeout(long timeout) {
        SETTINGS.putLong(TIMEOUT_KEY, Math.max(0L, timeout));
    }
}
