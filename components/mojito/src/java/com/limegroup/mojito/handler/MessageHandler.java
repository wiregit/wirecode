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
 
package com.limegroup.mojito.handler;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.routing.RouteTable;

/**
 * An abstract MessageHandler
 */
public abstract class MessageHandler {
    
    protected final Context context;
    
    public MessageHandler(Context context) {
        this.context = context;
    }
    
    protected MessageDispatcher getMessageDispatcher() {
        return context.getMessageDispatcher();
    }
    
    protected RouteTable getRouteTable() {
        return context.getRouteTable();
    }
    
    protected MessageFactory getMessageFactory() {
        return context.getMessageFactory();
    }
}
