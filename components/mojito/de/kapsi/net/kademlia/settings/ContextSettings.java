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

public class ContextSettings extends LimeDHTProps {
    
    private static final String LOCAL_NODE_ID_KEY = "LOCAL_NODE_ID";
    private static final String NODE_ID_TIMEOUT_KEY = "NODE_ID_TIMEOUT";
    
    private static final long NODE_ID_TIMEOUT = 30L * 60L * 1000L; // 30 Minutes
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(ContextSettings.class);
    
    private ContextSettings() {}
    
    public static final LongSetting DISPATCH_EVENTS_EVERY
        = FACTORY.createLongSetting("DISPATCH_EVENTS_EVERY", 50L);
    
    public static final LongSetting ESTIMATE_NETWORK_SIZE_EVERY
        = FACTORY.createLongSetting("ESTIMATE_NETWORK_SIZE_EVERY", /*60L **/ 1000L);
    
    public static final IntSetting MAX_LOCAL_HISTORY_SIZE
        = FACTORY.createIntSetting("MAX_LOCAL_HISTORY_SIZE", 20);
    
    public static final IntSetting MAX_REMOTE_HISTORY_SIZE
        = FACTORY.createIntSetting("MAX_REMOTE_HISTORY_SIZE", 10);
    
    public static final BooleanSetting COUNT_REMOTE_SIZE
        = FACTORY.createSettableBooleanSetting("COUNT_REMOTE_SIZE", true, "count_remote_size");
    
    public static final LongSetting SYNC_GET_VALUE_TIMEOUT
        = FACTORY.createLongSetting("SYNC_GET_VALUE_TIMEOUT", 60L * 1000L);
    
    public static final LongSetting SYNC_BOOTSTRAP_TIMEOUT
        = FACTORY.createLongSetting("SYNC_BOOTSTRAP_TIMEOUT", 3L * 60L * 1000L);
    
    public static void deleteNodeID(SocketAddress address) {
        String key = (address != null) ? address.toString() : "null";
        try {
            if (SETTINGS.nodeExists(key)) {
                Preferences node = SETTINGS.node(key);
                node.removeNode();
            }
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }
    
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
