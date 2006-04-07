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


public final class NetworkSettings extends LimeDHTProps {
    
    private NetworkSettings() {}
    
    public static final IntSetting PORT
        = FACTORY.createIntSetting("PORT", 31337);
    
    public static final LongSetting TIMEOUT
        = FACTORY.createSettableLongSetting("TIMEOUT", 5000L, "timeout", 5000L, 30000L);
    
    public static final IntSetting MAX_ERRORS
        = FACTORY.createSettableIntSetting("MAX_ERRORS", 3, "max_errors", 0, 10);
}
