/*
 * Mojito Distributed Hash Table (Mojito DHT)
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

package org.limewire.mojito.routing;

import java.io.Serializable;

/**
 * 
 */
public class Version implements Serializable {

    private static final long serialVersionUID = -4652316695244961502L;

    public static final Version UNKNOWN = new Version(0);
    
    private final int version;
    
    public Version(int version) {
        if ((version & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException("Version is out of range: " + version);
        }
        
        this.version = version;
    }
    
    public Version(int major, int minor) {
        if ((major & 0xFFFFFF00) != 0) {
            throw new IllegalArgumentException("Major version is out of range: " + major);
        }
        
        if ((minor & 0xFFFFFF00) != 0) {
            throw new IllegalArgumentException("Minor version is out of range: " + minor);
        }
        
        this.version = ((major & 0xFF) << 8) | (minor & 0xFF);
    }
    
    public int getMajor() {
        return (version >> 8) & 0xFF;
    }
    
    public int getMinor() {
        return version & 0xFF;
    }
    
    public int getVersion() {
        return version;
    }
    
    public int hashCode() {
        return getVersion();
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Version)) {
            return false;
        }
        
        return version == ((Version)o).version;
    }
    
    public String toString() {
        return getMajor() + "." + getMinor();
    }
}
