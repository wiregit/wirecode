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
