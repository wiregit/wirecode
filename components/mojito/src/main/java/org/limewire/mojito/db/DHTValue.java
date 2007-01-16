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

package org.limewire.mojito.db;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.ArrayUtils;


/**
 * A DHTValue is a type, version and value triple
 */
public class DHTValue implements Serializable {
    
    private static final long serialVersionUID = -7381830963268622187L;

    /**
     * An empty byte array
     */
    private static final byte[] EMPTY = new byte[0];
    
    /**
     * An empty value
     */
    public static final DHTValue EMPTY_VALUE = new DHTValue(DHTValueType.BINARY, Version.UNKNOWN, EMPTY);
    
    /**
     * The type of the value
     */
    private final DHTValueType valueType;
    
    /**
     * The version of the value
     */
    private final Version version;
    
    /**
     * The actual value
     */
    private final byte[] value;
    
    /**
     * The hash code of this value
     */
    private final int hashCode;
    
    public DHTValue(DHTValueType valueType, 
            Version version, byte[] value) {
        this.valueType = valueType;
        this.version = version;
        
        if (value == null || value.length == 0) {
            value = EMPTY;
        }
        
        this.value = value;
        
        this.hashCode = Arrays.hashCode(value);
    }

    /**
     * Returns the type of the value
     */
    public DHTValueType getValueType() {
        return valueType;
    }
    
    /**
     * Returns the version of the value
     */
    public Version getVersion() {
        return version;
    }
    
    /**
     * 
     */
    public void writeValue(OutputStream out) throws IOException {
        out.write(value, 0, value.length);
    }
    
    /**
     * Returns the actual value (a copy)
     */
    public byte[] getValue() {
        return getValue(new byte[value.length], 0, value.length);
    }
    
    /**
     * Returns the actual value (a copy)
     */
    public byte[] getValue(byte[] dst, int offset, int length) {
        System.arraycopy(value, 0, dst, 0, value.length);
        return dst;
    }
    
    /**
     * Returns the size of the value
     */
    public int size() {
        return value.length;
    }
    
    /**
     * Returns true if this is an empty value
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof DHTValue)) {
            return false;
        }
        
        DHTValue other = (DHTValue)o;
        return valueType.equals(other.valueType)
                    && version.equals(other.version)
                    && Arrays.equals(value, other.value);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("DHTValueType: ").append(getValueType()).append("\n");
        buffer.append("Version: ").append(getVersion()).append("\n");
        
        buffer.append("Value: ");
        if (isEmpty()) {
            buffer.append("This is an empty value (REMOVE operation)");
        } else {
            try {
                if (valueType.equals(DHTValueType.TEXT) 
                        || valueType.equals(DHTValueType.TEST)) {
                    buffer.append(new String(getValue(), "UTF-8")).append("\n");
                } else {
                    buffer.append(ArrayUtils.toHexString(getValue())).append("\n");
                }
            } catch (UnsupportedEncodingException err) {
                throw new RuntimeException(err);
            }
        }
        
        return buffer.toString();
    }
}
