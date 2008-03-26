package org.limewire.geocode;

import java.io.InputStream;

import org.limewire.geocode.SuccessOrFailureCallback;
import org.limewire.geocode.SuccessOrFailureCallbackConsumer;


/**
 * A very simple implementation of
 * {@link SuccessOrFailureCallbackConsumer<String>} that takes a single String
 * as the String to return as soon as it consumes the callback and never calls
 * {@link  SuccessOrFailureCallback#setInvalid(Throwable)}.
 */
public final class DefaultStreamSuccessOrFailureCallbackConsumer implements
        SuccessOrFailureCallbackConsumer<InputStream> {
    
    private final InputStream s;
    
    DefaultStreamSuccessOrFailureCallbackConsumer(InputStream s) {
        this.s = s;
    }
    
    public void consume(SuccessOrFailureCallback<InputStream> callback) {
        callback.process(s);
    }
}
