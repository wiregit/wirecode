package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps another IOState, allowing it to process only
 * if the AtomicBoolean says so.
 */
public class PossibleIOState implements IOState {
    
    private final AtomicBoolean allower;
    private final IOState delegate;
    
    public PossibleIOState(AtomicBoolean allower, IOState delegate) {
        this.allower = allower;
        this.delegate = delegate;
    }

    public long getAmountProcessed() {
        return delegate.getAmountProcessed();
    }

    public boolean isReading() {
        return delegate.isReading();
    }

    public boolean isWriting() {
        return delegate.isWriting();
    }

    public boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        if(allower.get())
            return delegate.process(channel, buffer);
        else
            return false;
    }
    
    

}
