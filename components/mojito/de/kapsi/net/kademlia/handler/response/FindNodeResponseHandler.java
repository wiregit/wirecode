/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.util.Collection;
import java.util.Map;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.messages.Message;

public class FindNodeResponseHandler extends LookupResponseHandler {
    
    private boolean storeLookup;
    private FindNodeListener l;
    
    public static FindNodeResponseHandler createDefaultHandler(Context context, KUID lookup) {
        return new FindNodeResponseHandler(context, lookup, null);
    }
    
    public static FindNodeResponseHandler createDefaultHandler(Context context, KUID lookup, FindNodeListener l) {
        return new FindNodeResponseHandler(context, lookup, l);
    }
    
    public static FindNodeResponseHandler createStoreForwardHandler(Context context, KUID lookup, FindNodeListener l) {
        if (l == null) {
            throw new NullPointerException("Listener cannot be null");
        }
        return new FindNodeResponseHandler(context, lookup, 1, l);
    }
    
    private FindNodeResponseHandler(Context context, KUID lookup, FindNodeListener l) {
        super(context, lookup);
        this.l = l;
    }
    
    private FindNodeResponseHandler(Context context, KUID lookup, int resultSize, FindNodeListener l) {
        super(context, lookup, resultSize);
        this.l = l;
    }
    
    protected Message createMessage(KUID lookup) {
        return context.getMessageFactory().createFindNodeRequest(lookup);
    }
    
    protected boolean isValueLookup() {
        return false;
    }
    
    protected void finishValueLookup(KUID lookup, Collection keyValues, long time) {
        throw new RuntimeException("This handler is responsible for FIND_NODE responses");
    }

    protected void finishNodeLookup(final KUID lookup, final Collection nodes, 
            final Map queryKeys, final long time) {
        
        if (l != null) {
            context.getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.foundNodes(lookup, nodes, queryKeys, time);
                }
            });
        }
    }
}
