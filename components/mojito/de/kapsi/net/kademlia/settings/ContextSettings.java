package de.kapsi.net.kademlia.settings;

import java.net.SocketAddress;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ContextSettings {
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(ContextSettings.class);
    
    private ContextSettings() {}
    
    public static byte[] getLocalNodeID(SocketAddress address) {
        String key = (address != null) ? address.toString() : "null";
        try {
            if (SETTINGS.nodeExists(key)) {
                return SETTINGS.node(key).getByteArray("LOCAL_NODE_ID", null);
            }
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    
    public static void setLocalNodeID(SocketAddress address, byte[] localId) {
        String key = (address != null) ? address.toString() : "null";
        SETTINGS.node(key).putByteArray("LOCAL_NODE_ID", localId);
    }
}
