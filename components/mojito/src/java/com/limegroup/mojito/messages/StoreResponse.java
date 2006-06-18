/*
 * Mojito Distributed Hash Tabe (DHT)
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

import com.limegroup.mojito.KUID;

public interface StoreResponse extends ResponseMessage {

    public static final int FAILED = 0x00;
    public static final int SUCCEEDED = 0x01;

    public static enum StoreStatus {
        FAILED(0x00),
        SUCCEEDED(0x01);
        
        private int status;
        
        private StoreStatus(int status) {
            this.status = status;
        }
        
        public int getStatus() {
            return status;
        }
        
        public static StoreStatus valueOf(int status) throws MessageFormatException {
            for(StoreStatus s : values()) {
                if (s.status == status) {
                    return s;
                }
            }
            
            throw new MessageFormatException("Unknown StoreStatus: " + status);
        }
    }
    
    public KUID getValueID();

    public StoreStatus getStatus();
}
