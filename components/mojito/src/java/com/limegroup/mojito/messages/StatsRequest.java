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
 
package com.limegroup.mojito.messages;

/**
 * An interface for StatsRequest implementations
 */
public interface StatsRequest extends RequestMessage, DHTSecureMessage {

    /**
     * Various types of statistic requests
     */
    public static enum Type {
        STATISTICS(0x01),
        DATABASE(0x02),
        ROUTETABLE(0x03);
        
        private int type;
        
        private Type(int type) {
            this.type = type;
        }
        
        public int toByte() {
            return type;
        }
        
        private static final Type[] TYPES;
        
        static {
            Type[] types = values();
            TYPES = new Type[types.length];
            for (Type t : types) {
                int index = t.type % TYPES.length;
                if (TYPES[index] != null) {
                    throw new IllegalStateException("Type collision: index=" + index 
                            + ", TYPE=" + TYPES[index] + ", t=" + t);
                }
                TYPES[index] = t;
            }
        }
        
        public static Type valueOf(int type) throws MessageFormatException {
            int index = type % TYPES.length;
            Type t = TYPES[index];
            if (t.type == type) {
                return t;
            }
            
            throw new MessageFormatException("Unknown type: " + type);
        }
    }
    
    /**
     * Returns the Type of the request
     */
    public Type getType();
}
