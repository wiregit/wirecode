/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.util.Collection;
import java.util.Map;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.FindValueListener;
import de.kapsi.net.kademlia.messages.Message;

public class FindValueResponseHandler extends LookupResponseHandler {
    
    private FindValueListener l;

    public FindValueResponseHandler(Context context, 
            KUID lookup, FindValueListener l) {
        super(context, lookup);
        this.l = l;
    }
    
    protected boolean isValueLookup() {
        return true;
    }
    
    protected Message createMessage(KUID lookup) {
        return context.getMessageFactory().createFindValueRequest(lookup);
    }

    protected void finishValueLookup(final KUID lookup, final Collection keyValues, final long time) {
        if (l != null) {
            context.getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.foundValue(lookup, keyValues, time);
                }
            });
        }
    }

    protected void finishNodeLookup(KUID lookup, Collection nodes, Map queryKeys, long time) {
        throw new RuntimeException("This handler is responsible for FIND_VALUE responses");
    }
}
