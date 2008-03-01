package com.limegroup.gnutella.geocode;

import com.limegroup.gnutella.SuccessOrFailureCallback;
import com.limegroup.gnutella.SuccessOrFailureCallbackConsumer;

/**
 * A very simple implementation of
 * {@link SuccessOrFailureCallbackConsumer<String>} that takes a single String
 * as the String to return as soon as it consumes the callback and never calls
 * {@link  SuccessOrFailureCallback#setInvalid(Throwable)}.
 */
final class SimpleStringSuccessOrFailureCallbackConsumer implements
        SuccessOrFailureCallbackConsumer<String> {
    
    private final String s;
    
    SimpleStringSuccessOrFailureCallbackConsumer(String s) {
        this.s = s;
    }
    
    public void consume(SuccessOrFailureCallback<String> callback) {
        callback.process(s);
    }
}
