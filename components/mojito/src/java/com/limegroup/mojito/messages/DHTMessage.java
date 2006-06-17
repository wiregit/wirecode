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
    
    /*private static final int PING_REQUEST = 0x01;
    private static final int PING_RESPONSE = 0x02;
    
    private static final int STORE_REQUEST = 0x03;
    private static final int STORE_RESPONSE = 0x04;
    
    private static final int FIND_NODE_REQUEST = 0x05;
    private static final int FIND_NODE_RESPONSE = 0x06;
    
    private static final int FIND_VALUE_REQUEST = 0x07;
    private static final int FIND_VALUE_RESPONSE = 0x08;
    
    private static final int STATS_REQUEST = 0x09;
    private static final int STATS_RESPONSE = 0x0A;*/
    
    public static enum OpCode {
        
        UNKNOWN(0x00),
        
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
        
        private final int opcode;
            
        private OpCode(int opcode) {
            this.opcode = opcode;
        }
    
        public int getOpCode() {
            return opcode;
        }
        
        public String toString() {
            return "OpCode(" + opcode + ")";
        }
        
        public static OpCode valueOf(int opcode) {
            switch(opcode) {
                case 0x01: return PING_REQUEST;
                case 0x02: return PING_RESPONSE;
                case 0x03: return STORE_REQUEST;
                case 0x04: return STORE_RESPONSE;
                case 0x05: return FIND_NODE_REQUEST;
                case 0x06: return FIND_NODE_RESPONSE;
                case 0x07: return FIND_VALUE_REQUEST;
                case 0x08: return FIND_VALUE_RESPONSE;
                case 0x09: return STATS_REQUEST;
                case 0x0A: return STATS_RESPONSE;
                default: return UNKNOWN;
            }
        }
    }
    
    /** Returns the opcode (type) of the Message */
    public OpCode getOpCode();
    
    /** Returns the Vendor of the Message */
    public int getVendor();

    /** Returns the Version of the Message */
    public int getVersion();

    /** Returns the sender of this Message */
    public ContactNode getContactNode();
    
    /** Returns the Message ID of the Message */
    public KUID getMessageID();
    
    /** Writes this Message to the OutputStream */
    public void write(OutputStream out) throws IOException;
}
