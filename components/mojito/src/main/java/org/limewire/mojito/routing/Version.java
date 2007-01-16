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

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * 
 */
public class Version implements Serializable, Comparable<Version> {

    private static final long serialVersionUID = -4652316695244961502L;

    public static final Version UNKNOWN = new Version(0);
    
    private final int version;
    
    private Version(int version) {
        this.version = version;
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
    
    public int compareTo(Version o) {
        return version - o.version;
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
    
    /** 
     * An array of cached Versions. Make it bigger if necessary.
     */
    private static final Version[] VERSIONS = new Version[10];
    
    /**
     * Returns a Version object for the given version number
     */
    public static synchronized Version valueOf(int version) {
        if ((version & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException("Version is out of range (0x0000 - 0xFFFF): " + version);
        }
        
        int index = version % VERSIONS.length;
        Version vers = VERSIONS[index];
        if (vers == null || vers.version != version) {
            vers = new Version(version);
            VERSIONS[index] = vers;
        }
        return vers;
    }
    
    /**
     * Returns a Version object for the given major and minor version number
     */
    public static Version valueOf(int major, int minor) {
        return valueOf((major << 8) | minor);
    }
    
    /**
     * Check the cache and replace this instance with the cached instance
     * if one exists. The main goal is to pre-initialize the VERSIONS
     * array.
     */
    private Object readResolve() throws ObjectStreamException {
        synchronized (getClass()) {
            int index = version % VERSIONS.length;
            Version vers = VERSIONS[index];
            if (vers == null || vers.version != version) {
                vers = this;
                VERSIONS[index] = vers;
            }
            return vers;
        }
    }
}
