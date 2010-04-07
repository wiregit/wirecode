package org.limewire.mojito.handler.response2;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.KUID;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;

/**
 * 
 */
public interface ResponseHandler {

    /**
     * 
     */
    public void handleResponse(ResponseMessage message, long time, TimeUnit unit) throws IOException;
    
    /**
     * 
     */
    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) throws IOException;
    
    /**
     * 
     */
    public void handleError(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e);
}
