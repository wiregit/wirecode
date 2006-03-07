/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.settings.NetworkSettings;

public abstract class AbstractResponseHandler extends MessageHandler 
        implements ResponseHandler {
    
    private long timeout;
    
    public AbstractResponseHandler(Context context) {
        this(context, NetworkSettings.getTimeout());
    }
    
    public AbstractResponseHandler(Context context, long timeout) {
        super(context);
        
        if (timeout < 0L) {
            throw new IllegalArgumentException("Timeout must be >= 0");
        }
        
        this.timeout = timeout;
    }
    
    public long timeout() {
        return timeout;
    }
}
