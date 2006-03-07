/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.util.Collection;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.messages.Message;

public class FindNodeResponseHandler extends LookupResponseHandler {
    
    private FindNodeListener l;
    
    public FindNodeResponseHandler(Context context, KUID lookup, FindNodeListener l) {
        super(context, lookup, false);
        this.l = l;
    }
    
    protected Message createMessage(KUID lookup) {
        return context.getMessageFactory().createFindNodeRequest(lookup);
    }
    
    protected void finish(final KUID lookup, final Collection result, final long time) {
        if (l != null) {
            context.getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.foundNodes(lookup, result, time);
                }
            });
        }
    }
}
