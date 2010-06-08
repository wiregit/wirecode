package org.limewire.mojito.io;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.message.ResponseMessage;

/**
 * An interface that handles {@link ResponseMessage}s.
 */
public interface ResponseHandler {
    
    /**
     * Called if a {@link ResponseMessage} was received.
     */
    public void handleResponse(RequestHandle handle, 
            ResponseMessage response, 
            long time, TimeUnit unit) throws IOException;
    
    /**
     * Called if a timeout occurred.
     */
    public void handleTimeout(RequestHandle handle, 
            long time, TimeUnit unit) throws IOException;
    
    /**
     * Called if an {@link Exception} occurred.
     */
    public void handleException(RequestHandle handle, Throwable exception);
}