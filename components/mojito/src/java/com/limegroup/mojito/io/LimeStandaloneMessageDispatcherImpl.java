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

package com.limegroup.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.impl.DefaultMessageFactory;

/**
 * An implementation of MessageDispatcher for debugging purposes. 
 * It allows us to read/write Messages in LimeDHTMessage format 
 * which means you can start an instance of the LimeWire core and 
 * an arbitary number of DHT Nodes that are using this implementation.
 */
public class LimeStandaloneMessageDispatcherImpl 
        extends MessageDispatcherImpl {

    private static final int MESSAGE_HEADER = 23;
    
    public LimeStandaloneMessageDispatcherImpl(Context context) {
        super(context);
        
        context.setMessageFactory(new LimeStandaloneMessageFactory());
    }

    private static class LimeStandaloneMessageFactory extends DefaultMessageFactory {
        public DHTMessage createMessage(SocketAddress src, ByteBuffer data) 
                throws MessageFormatException, IOException {
            
            // Skip the Gnutella Header
            data.position(MESSAGE_HEADER);
            data.mark();
            
            return super.createMessage(src, data);
        }        
    }
}
