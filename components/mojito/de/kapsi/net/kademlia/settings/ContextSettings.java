package de.kapsi.net.kademlia.settings;

import java.net.SocketAddress;
import java.util.prefs.Preferences;

public class ContextSettings {
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(ContextSettings.class);
    
    private ContextSettings() {}
    
    public static byte[] getLocalNodeID(SocketAddress address) {
        return SETTINGS.getByteArray("LOCAL_NODE_ID", null);
    }
    
    public static void setLocalNodeID(byte[] localId) {
        SETTINGS.putByteArray("LOCAL_NODE_ID", localId);
    }
}
