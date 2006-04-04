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
