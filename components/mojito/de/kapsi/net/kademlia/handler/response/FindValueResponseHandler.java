/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.util.Collection;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.FindValueListener;
import de.kapsi.net.kademlia.messages.Message;

public class FindValueResponseHandler extends LookupResponseHandler {
    
    private FindValueListener l;

    public FindValueResponseHandler(Context context, 
            KUID lookup, FindValueListener l) {
        super(context, lookup, true);
        this.l = l;
    }
    
    protected Message createMessage(KUID lookup) {
        return context.getMessageFactory().createFindValueRequest(lookup);
    }

    protected void finish(final KUID lookup, final Collection values, final long time) {
        if (l != null) {
            context.getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.foundValue(lookup, values, time);
                }
            });
        }
    }
}
