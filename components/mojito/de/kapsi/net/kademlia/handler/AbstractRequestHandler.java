/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler;

import de.kapsi.net.kademlia.Context;

public abstract class AbstractRequestHandler extends MessageHandler 
        implements RequestHandler {
    
    public AbstractRequestHandler(Context context) {
        super(context);
    }
}
