/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.settings;

import java.util.prefs.Preferences;

public final class KademliaSettings {
    
    private static final int REPLICATION_PARAMETER = 20; // a.k.a. K
    private static final String REPLICATION_PARAMETER_KEY = "REPLICATION_PARAMETER";
    
    private static final int LOOKUP_PARAMETER = 3; // a.k.a. A
    private static final String LOOKUP_PARAMETER_KEY = "LOOKUP_PARAMETER";
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(KademliaSettings.class);
    
    private KademliaSettings() {}
    
    public static int getReplicationParameter() {
        return SETTINGS.getInt(REPLICATION_PARAMETER_KEY, REPLICATION_PARAMETER);
    }
    
    public static void setReplicationParameter(int replicationParameter) {
        SETTINGS.putInt(REPLICATION_PARAMETER_KEY, Math.max(0, replicationParameter));
    }
    
    public static int getLookupParameter() {
        return SETTINGS.getInt(LOOKUP_PARAMETER_KEY, LOOKUP_PARAMETER);
    }
    
    public static void setLookupParameter(int lookupParameter) {
        SETTINGS.putInt(LOOKUP_PARAMETER_KEY, 
                Math.max(0, Math.min(lookupParameter, getReplicationParameter())));
    }
}
