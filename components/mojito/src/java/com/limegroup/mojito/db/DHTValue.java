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

package com.limegroup.mojito.db;

import java.io.Serializable;
import java.util.Arrays;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.Contact;

/**
 * The DHTValue class represents a <key, value> tuple that
 * is stored on the DHT. Besides the actual <key, value> tuple 
 * it's also storing the originator of the DHTValue as well as
 * the sender of the DHTValue.
 */
public interface DHTValue {
    
    /**
     * An empty value
     */
    public static final byte[] EMPTY_DATA = new byte[0];
    
    /**
     * ValueType specifies the type of a DHTValue
     */
    public static final class ValueType implements Comparable<ValueType>, Serializable {
        
        private static final long serialVersionUID = -3662336008253896020L;
        
        // NOTE: We cannot use enums for the ValueType! The simple
        // reason is that it's not possible to create new enums at
        // runtime which is a problem if somebody sends us a DHTValue
        // we don't understand. It'd be impossible to wrap the type
        // code into an enum.
        
        public static final ValueType BINARY = new ValueType("BINARY", 0x00000000);
        //public static final ValueType LIME = new ValueType("LIME", parse("LIME"));
        
        private static final ValueType[] TYPES = new ValueType[] {
            BINARY
        };
        
        static {
            Arrays.sort(TYPES);
        }
        
        private String name;
        
        private int type;
        
        private ValueType(String name, int type) {
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

        public int compareTo(ValueType o) {
            return type - o.type;
        }

        public int hashCode() {
            return type & Integer.MAX_VALUE;
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof ValueType)) {
                return false;
            }
            
            ValueType other = (ValueType)o;
            return compareTo(other)==0 && name.equals(other.name);
        }
        
        public String toString() {
            return name + " (0x" + Long.toHexString((type & 0xFFFFFFFFL)) + ")";
        }
        
        public static ValueType[] values() {
            ValueType[] copy = new ValueType[TYPES.length];
            System.arraycopy(TYPES, 0, copy, 0, copy.length);
            return copy;
        }
        
        public static ValueType valueOf(int type) {
            ValueType unknown = new ValueType("UNKNOWN", type);
            int index = Arrays.binarySearch(TYPES, unknown);
            if (index < 0) {
                return unknown;
            } else {
                return TYPES[index];
            }
        }
        
        /*private static int parse(String type) {
            char[] chars = type.toCharArray();
            if (chars.length != 4) {
                throw new IllegalArgumentException();
            }
            
            int id = 0;
            for(char c : chars) {
                id = (id << 8) | (int)(c & 0xFF);
            }
            return id;
        }*/
    }

    /** 
     * Returns the ValueID
     */
    public KUID getValueID();
    
    /**
     * Returns the Type of the Value
     */
    public ValueType getValueType();
    
    /** 
     * Returns the Value. Beware: The returned byte array is 
     * <b>NOT</b> a copy!
     */
    public byte[] getData();
    
    /** 
     * Returns the size of the value 
     */
    public int size();
    
    /** 
     * Returns whether or not the value is empty
     */
    public boolean isEmpty();
    
    /** 
     * Returns the creator of the value 
     */
    public Contact getCreator();
    
    /**
     * Returns the Node ID of the value creator
     */
    public KUID getCreatorID();
    
    /** 
     * Returns the sender of the value 
     */
    public Contact getSender();
    
    /** 
     * Returns the creationTime of this DHTValue object 
     */ 
    public long getCreationTime();
    
    /**
     * Returns the time when this DHTValue was republished
     */
    public long getPublishTime();
    
    /** 
     * Returns whether or not the originator and sender 
     * of the DHTValue are the same
     */
    public boolean isDirect();
    
    /** 
     * Returns whether or not this is a local DHTValue 
     */
    public boolean isLocalValue();
    
    /**
     * Returns true if this DHTValue requires republishing. Returns
     * always false if this is a non-local value.
     */
    public boolean isRepublishingRequired();
    
    /**
     * Sets the number of locations where this DHTValue was stored and 
     * the lastRepublishingTime to the current System time
     */
    public void setLocationCount(int locationCount);
    
    /**
     * Returns the number of locations where this DHTValue was stored
     */
    public int getLocationCount();
    
}
