package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.ErrorCallback;

/**
 * Simple class that counts up how many exceptions it recieved.
 */
public class ErrorCallbackStub implements ErrorCallback {
    public int exceptions = 0;
    
    public void error(Throwable t) {
        exceptions++;
    }
}   