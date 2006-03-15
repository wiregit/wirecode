/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.event.EventDispatcher;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.MessageFactory;
import de.kapsi.net.kademlia.routing.RoutingTable;

public abstract class MessageHandler {
    
    protected final Context context;
    
    public MessageHandler(Context context) {
        this.context = context;
    }
    
    protected MessageDispatcher getMessageDispatcher() {
        return context.getMessageDispatcher();
    }
    
    protected RoutingTable getRouteTable() {
        return context.getRouteTable();
    }
    
    protected EventDispatcher getEventDispatcher() {
        return context.getEventDispatcher();
    }
    
    protected MessageFactory getMessageFactory() {
        return context.getMessageFactory();
    }
}
