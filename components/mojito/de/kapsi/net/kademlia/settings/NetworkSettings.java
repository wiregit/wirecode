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

public final class NetworkSettings {
    
    private static final int PORT = 31337;
    private static final String PORT_KEY = "PORT";
    
    private static final long TIMEOUT = 10L * 1000L;
    private static final String TIMEOUT_KEY = "TIMEPUT";
    
    private static final int MAX_ERRORS = 3;
    private static final String MAX_ERRORS_KEY = "MAX_ERRORS";
    
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
    
    public static int getMaxErrors() {
        return SETTINGS.getInt(MAX_ERRORS_KEY, MAX_ERRORS);
    }
    
    public static void setMaxErrors(int maxErrors) {
        SETTINGS.putInt(MAX_ERRORS_KEY, Math.max(0, maxErrors));
    }
}
