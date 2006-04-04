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

import java.net.SocketAddress;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ContextSettings {
    
    private static final String LOCAL_NODE_ID_KEY = "LOCAL_NODE_ID";
    
    private static final String NODE_ID_TIMEOUT_KEY = "NODE_ID_TIMEOUT";
    
    private static final long NODE_ID_TIMEOUT = 30L * 60L * 1000L; // 30 Minutes
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(ContextSettings.class);
    
    private ContextSettings() {}
    
    public static byte[] getLocalNodeID(SocketAddress address) {
        String key = (address != null) ? address.toString() : "null";
        try {
            if (SETTINGS.nodeExists(key)) {
                Preferences node = SETTINGS.node(key);
                
                long time = node.getLong(NODE_ID_TIMEOUT_KEY, System.currentTimeMillis());
                if ((System.currentTimeMillis() - time) < NODE_ID_TIMEOUT) {
                    return node.getByteArray(LOCAL_NODE_ID_KEY, null);
                }
            }
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    
    public static void setLocalNodeID(SocketAddress address, byte[] localId) {
        String key = (address != null) ? address.toString() : "null";
        
        Preferences node = SETTINGS.node(key);
        node.putByteArray(LOCAL_NODE_ID_KEY, localId);
        node.putLong(NODE_ID_TIMEOUT_KEY, System.currentTimeMillis());
    }
}
