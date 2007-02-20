/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.limewire.mojito.util.ArrayUtils;

/**
 * DHTValueType specifies the type of a DHTValue
 */
public final class DHTValueType implements Comparable<DHTValueType>, Serializable {
    
    private static final long serialVersionUID = -3662336008253896020L;
    
    private static final String UNKNOWN_NAME = "UNKNOWN";
    
    // NOTE: We cannot use enums for the DHTValueType! The simple
    // reason is that it's not possible to create new enums at
    // runtime which is a problem if somebody sends us a DHTValue
    // we don't understand. It'd be impossible to wrap the type
    // code into an enum Object.
    
    // --- BEGIN VALUETYPE DECLARATION BLOCK ---
    
    /**
     * An arbitrary type of value
     */
    public static final DHTValueType BINARY = new DHTValueType("BINARY", 0x00000000);
    //public static final ValueType LIME = new ValueType("LIME", parse("LIME"));
    
    /**
     * Type for UTF-8 encoded Strings
     */
    public static final DHTValueType TEXT = new DHTValueType("TEXT");
    
    /**
     * A value that is used for testing purposes
     */
    public static final DHTValueType TEST = new DHTValueType("TEST");
    
    // --- END VALUETYPE DECLARATION BLOCK ---
    
    /**
     * An arry of ValueTypes. It's initialized in the static
     * initializer.
     */
    private static final DHTValueType[] TYPES;
    
    static {
        List<DHTValueType> types = new ArrayList<DHTValueType>();
        Field[] fields = DHTValueType.class.getDeclaredFields();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            Class<?> type = field.getType();
            
            // Make sure it's a static field and of type ValueType
            if ((modifiers & Modifier.STATIC) != 0 
                    && type.isAssignableFrom(DHTValueType.class)) {
                
                try {
                    DHTValueType valueType = (DHTValueType)field.get(null);
                    if (valueType == null) {
                        throw new NullPointerException(
                                "The static ValueType field " + field.getName() 
                                + " is either really null or is declared after the"
                                + " static-initializer block");
                    }
                    
                    types.add(valueType);
                } catch (IllegalAccessException err) {
                    // This should never happen!
                    throw new RuntimeException(err);
                }
            }
        }
        
        TYPES = types.toArray(new DHTValueType[0]);
        
        // Sort the types by their type code so that we
        // can perform binary searches on the array
        Arrays.sort(TYPES, new Comparator<DHTValueType>() {
            public int compare(DHTValueType o1, DHTValueType o2) {
                int diff = o1.compareTo(o2);
                if (diff == 0) {
                    throw new IllegalArgumentException("The type code of " 
                            + o1 + " and " + o2 + " collide!");
                }
                return diff;
            }
        });
    }
    
    /** The Name of the value type */
    private String name;
    
    /** The type code of the value */
    private int type;
    
    private DHTValueType(String name) {
        this(name, ArrayUtils.toInteger(name));
    }
    
    private DHTValueType(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }
    
    public int toInt() {
        return type;
    }
    
    public boolean isUnknownType() {
        return Arrays.binarySearch(TYPES, this) >= 0;
    }

    public int compareTo(DHTValueType o) {
        return type - o.type;
    }

    public int hashCode() {
        return type;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof DHTValueType)) {
            return false;
        }
        
        DHTValueType other = (DHTValueType)o;
        return compareTo(other)==0 && name.equals(other.name);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (name.equals(UNKNOWN_NAME)) {
            buffer.append(toString(type)).append("/").append(name);
        } else {
            buffer.append(name);
        }
        buffer.append(" (0x").append(Long.toHexString((type & 0xFFFFFFFFL))).append(")");
        return buffer.toString();
    }
    
    public static DHTValueType[] values() {
        DHTValueType[] copy = new DHTValueType[TYPES.length];
        System.arraycopy(TYPES, 0, copy, 0, copy.length);
        return copy;
    }
    
    public static DHTValueType valueOf(int type) {
        DHTValueType unknown = new DHTValueType(UNKNOWN_NAME, type);
        int index = Arrays.binarySearch(TYPES, unknown);
        if (index < 0) {
            return unknown;
        } else {
            return TYPES[index];
        }
    }
    
    public static DHTValueType valueOf(String type) {
        return valueOf(ArrayUtils.toInteger(type));
    }
    
    private static String toString(int type) {
        byte[] name = new byte[] {
            (byte)((type >> 24) & 0xFF),
            (byte)((type >> 16) & 0xFF),
            (byte)((type >>  8) & 0xFF),
            (byte)((type      ) & 0xFF)
        };
        
        return new String(name);
    }
}
