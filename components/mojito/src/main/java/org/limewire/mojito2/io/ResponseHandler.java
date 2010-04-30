package org.limewire.mojito2.io;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.message.ResponseMessage;

/**
 * 
 */
public interface ResponseHandler {
    
    /**
     * 
     */
    public void handleResponse(RequestHandle handle, 
            ResponseMessage response, 
            long time, TimeUnit unit) throws IOException;
    
    /**
     * 
     */
    public void handleTimeout(RequestHandle handle, 
            long time, TimeUnit unit) throws IOException;
    
    /**
     * 
     */
    public void handleException(RequestHandle handle, Throwable exception);
}