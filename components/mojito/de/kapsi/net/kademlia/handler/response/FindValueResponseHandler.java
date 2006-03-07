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
    
    //private static final Log LOG = LogFactory.getLog(FindValueResponseHandler.class);
    
    //private boolean found = false;
    
    private FindValueListener l;

    public FindValueResponseHandler(Context context, 
            KUID lookup, FindValueListener l) {
        super(context, lookup, true);
        this.l = l;
    }
    
    /*public void handleResponse(KUID nodeId, SocketAddress src, Message message, long time) throws IOException {
        
        // TODO not sure about this!? The idea is to
        // store responses that arrive after the first
        // one...
        boolean isFindValueResponse 
            = (message instanceof FindValueResponse);
        
        if (found && !isFindValueResponse) {
            return;
        }

        if (isFindValueResponse) {
            Collection values = ((FindValueResponse) message).getResult();
            
            // TODO make sure the Node isn't sending us some
            // bull! The 1st KeyValue must be equal to the
            // lookup Key and the others must be 'close' to
            // our NodeID!
            try {
                context.getDatabase().addAll(values);
            // TODO handle Errors
            } catch (SignatureException err) {
            } catch (InvalidKeyException err) {
            }
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(Node.toString(nodeId, src) 
                        + " returned Values for " 
                        + lookup + " after " 
                        + round + " rounds and " 
                        + queried.size() + " queried Nodes");
            }
            
            if (!found) {
                fireEvent(lookup, values, time()+time);
            }
            
            found = true;
        } else {
            super.handleResponse(nodeId, src, message, time);
        }
    }
    
    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) throws IOException {
        if (found) {
            return;
        }
        
        super.handleTimeout(nodeId, dst, time);
    }*/
    
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
    
    /*private void fireEvent(final KUID key, final Collection values, final long time) {
        if (l != null) {
            context.getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.foundValue(key, values, time);
                }
            });
        }
    }*/
}
