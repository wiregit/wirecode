package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.KUID;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.message.ResponseMessage;

/**
 * 
 */
public interface ResponseHandler {
    
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