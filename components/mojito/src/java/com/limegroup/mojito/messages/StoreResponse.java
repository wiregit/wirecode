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

public interface StoreResponse extends ResponseMessage {

    public static enum Status {
        FAILED(0x00),
        SUCCEEDED(0x01);
        
        private int status;
        
        private Status(int status) {
            this.status = status;
        }
        
        public int getStatus() {
            return status;
        }
        
        private static Status[] STATUS;
        
        static {
            Status[] values = values();
            STATUS = new Status[values.length];
            for (Status s : values) {
                int index = s.status % STATUS.length;
                if (STATUS[index] != null) {
                    // Check the enums for duplicate states
                    throw new IllegalStateException("StoreStatus collision: index=" + index 
                            + ", STORESTATUS=" + STATUS[index] + ", o=" + s);
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
    
    public Collection<Entry<KUID, Status>> getStatus();
}
