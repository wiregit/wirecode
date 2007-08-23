package org.limewire.service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple class that counts up how many exceptions it recieved.
 */
public class ErrorCallbackStub implements ErrorCallback {
    private final AtomicInteger exceptions = new AtomicInteger(0);
    
    public void error(Throwable t) {
        exceptions.getAndIncrement();
    }
    
    public void error(Throwable t, String detail) {
        exceptions.getAndIncrement();
    }
    
    public int getExceptionCount() {
        return exceptions.get();
    }
}   
