package org.limewire.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.KUID;
import org.limewire.mojito.message2.RequestMessage;
import org.limewire.mojito.message2.ResponseMessage;

/**
 * 
 */
public interface ResponseHandler2 {
    
    /**
     * 
     */
    public void handleResponse(RequestMessage request, 
            ResponseMessage response, 
            long time, TimeUnit unit) throws IOException;
    
    /**
     * 
     */
    public void handleTimeout(KUID contactId, SocketAddress dst,
            RequestMessage request, long time, TimeUnit unit) throws IOException;
    
    /**
     * 
     */
    public void handleException(RequestMessage request, Throwable exception);
}