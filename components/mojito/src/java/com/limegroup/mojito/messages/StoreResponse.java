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

import java.util.Collection;
import java.util.Map.Entry;

import com.limegroup.mojito.KUID;

/**
 * An interface for StoreResponse implementations
 */
public interface StoreResponse extends ResponseMessage {

    /**
     * The Status tells whether or not the remote 
     * Node was able to store a DHTValue
     */
    public static enum Status {
        
        /**
         * The remote Node was NOT able to store the DHTValue
         */
        FAILED(0x01),
        
        /**
         * The remote Node was able to store the DHTValue
         */
        SUCCEEDED(0x02);
        
        private int status;
        
        private Status(int status) {
            this.status = status;
        }
        
        public int toByte() {
            return status;
        }
        
        public String toString() {
            return name() + " (" + toByte() + ")";
        }
        
        private static Status[] STATUS;
        
        static {
            Status[] values = values();
            STATUS = new Status[values.length];
            for (Status s : values) {
                int index = s.status % STATUS.length;
                if (STATUS[index] != null) {
                    // Check the enums for duplicate states
                    throw new IllegalStateException("Status collision: index=" + index 
                            + ", STATUS=" + STATUS[index] + ", s=" + s);
                }
                STATUS[index] = s;
            }
        }
        
        public static Status valueOf(int status) throws MessageFormatException {
            Status s = STATUS[status % STATUS.length];
            if (s != null && s.status == status) {
                return s;
            }
            throw new MessageFormatException("Unknown StoreStatus: " + status);
        }
    }
    
    /**
     * Returns a Collection of KUID-Status Tuples.
     */
    public Collection<? extends Entry<KUID, Status>> getStatus();
}
