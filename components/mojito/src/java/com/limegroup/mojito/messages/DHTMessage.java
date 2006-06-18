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

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;

/**
 * This is an abstract base class for all DHT messages.
 */
public interface DHTMessage {
    
    /**
     * The opcodes of our Messages
     */
    public static enum OpCode {
        
        PING_REQUEST(0x01),
        PING_RESPONSE(0x02),
        
        STORE_REQUEST(0x03),
        STORE_RESPONSE(0x04),
        
        FIND_NODE_REQUEST(0x05),
        FIND_NODE_RESPONSE(0x06),
        
        FIND_VALUE_REQUEST(0x07),
        FIND_VALUE_RESPONSE(0x08),
        
        STATS_REQUEST(0x09),
        STATS_RESPONSE(0x0A);
        
        private int opcode;
            
        private OpCode(int opcode) {
            this.opcode = opcode;
        }
    
        public int getOpCode() {
            return opcode;
        }
        
        public String toString() {
            return name() + "(" + getOpCode() + ")";
        }
        
        /**
         * Returns the OpCode enum for the integer. Throws an
         * IllegalArgumentException if opcode is unknown!
         */
        public static OpCode valueOf(int opcode) throws IllegalArgumentException {
            for(OpCode o : values()) {
                if (o.opcode == opcode) {
                    return o;
                }
            }
            
            throw new IllegalArgumentException("Unknown opcode: " + opcode);
        }
    }
    
    /** Returns the opcode (type) of the Message */
    public OpCode getOpCode();
    
    /** Returns the sender of this Message */
    public ContactNode getContactNode();
    
    /** Returns the Message ID of the Message */
    public KUID getMessageID();
    
    /** Writes this Message to the OutputStream */
    public void write(OutputStream out) throws IOException;
}
