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

public final class LookupSettings {
    
    private static final long LOOKUP_TIMEOUT = 5000L;
    private static final String LOOKUP_TIMEOUT_KEY = "LOOKUP_TIMEOUT";
    
    private static final Preferences SETTINGS 
        = Preferences.userNodeForPackage(LookupSettings.class);

    private LookupSettings() {}
    
    public static long getTimeout() {
        return SETTINGS.getLong(LOOKUP_TIMEOUT_KEY, LOOKUP_TIMEOUT);
    }
    
    public static void setTimeout(long timeout) {
        SETTINGS.putLong(LOOKUP_TIMEOUT_KEY, Math.max(0L, timeout));
    }
}
