package com.limegroup.gnutella.geocode;

import java.io.InputStream;

import com.limegroup.gnutella.SuccessOrFailureCallback;
import com.limegroup.gnutella.SuccessOrFailureCallbackConsumer;

/**
 * A very simple implementation of
 * {@link SuccessOrFailureCallbackConsumer<String>} that takes a single String
 * as the String to return as soon as it consumes the callback and never calls
 * {@link  SuccessOrFailureCallback#setInvalid(Throwable)}.
 */
final class SimpleInputStreamSuccessOrFailureCallbackConsumer implements
        SuccessOrFailureCallbackConsumer<InputStream> {
    
    private final InputStream s;
    
    SimpleInputStreamSuccessOrFailureCallbackConsumer(InputStream s) {
        this.s = s;
    }
    
    public void consume(SuccessOrFailureCallback<InputStream> callback) {
        callback.process(s);
    }
}
