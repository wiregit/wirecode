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
import java.io.UnsupportedEncodingException;

/**
 * 
 */
public class Vendor implements Serializable, Comparable<Vendor> {
    
    private static final long serialVersionUID = 1607453128714814318L;
    
    public static final Vendor UNKNOWN = new Vendor(0);
    
    private final int vendorId;
    
    private Vendor(int vendorId) {
        this.vendorId = vendorId;
    }
    
    public int getVendor() {
        return vendorId;
    }
    
    public int hashCode() {
        return vendorId;
    }
    
    public int compareTo(Vendor o) {
        return vendorId - o.vendorId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Vendor)) {
            return false;
        }
        
        return vendorId == ((Vendor)o).vendorId;
    }
    
    public String toString() {
        return toString(vendorId);
    }
    
    /** 
     * An array of cached Vendors. Make it bigger if necessary.
     */
    private static final Vendor[] VENDORS = new Vendor[10];
    
    /**
     * Returns a Vendor object for the given vendor ID
     */
    public static synchronized Vendor valueOf(int vendorId) {
        int index = (vendorId & Integer.MAX_VALUE) % VENDORS.length;
        Vendor vendor = VENDORS[index];
        if (vendor == null || vendor.vendorId != vendorId) {
            vendor = new Vendor(vendorId);
            VENDORS[index] = vendor;
        }
        return vendor;
    }
    
    /**
     * Returns a Vendor object for the given vendor ID
     */
    public static Vendor valueOf(String vendorId) {
        return valueOf(parse(vendorId));
    }
    
    /**
     * Check the cache and replace this instance with the cached instance
     * if one exists. The main goal is to pre-initialize the VENDORS
     * array.
     */
    private Object readResolve() {
        synchronized (getClass()) {
            int index = (vendorId & Integer.MAX_VALUE) % VENDORS.length;
            Vendor vendor = VENDORS[index];
            if (vendor == null || vendor.vendorId != vendorId) {
                vendor = this;
                VENDORS[index] = vendor;
            }
            return vendor;
        }
    }
    
    /**
     * A helper method to convert a 4 character ASCII String
     * into an Interger
     */
    public static int parse(String vendorId) {
        if (vendorId == null) {
            throw new NullPointerException("VendorID is null");
        }
        
        char[] chars = vendorId.toCharArray();
        if (chars.length != 4) {
            throw new IllegalArgumentException("VendorID must be 4 characters");
        }
        
        int id = 0;
        for(char c : chars) {
            id = (id << 8) | (c & 0xFF);
        }
        return id;
    }
    
    /**
     * A helper method to convert each of vendorId's 4 bytes
     * into an ASCII character and to return them as String
     */
    public static String toString(int vendorId) {
        try {
            byte[] name = new byte[]{
                (byte)((vendorId >> 24) & 0xFF),
                (byte)((vendorId >> 16) & 0xFF),
                (byte)((vendorId >>  8) & 0xFF),
                (byte)((vendorId      ) & 0xFF)
            };
            return new String(name, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
