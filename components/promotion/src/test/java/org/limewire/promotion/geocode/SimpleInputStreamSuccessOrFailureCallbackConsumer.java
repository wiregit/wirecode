package org.limewire.promotion.geocode;

import java.io.InputStream;

import org.limewire.promotion.geocode.SuccessOrFailureCallback;
import org.limewire.promotion.geocode.SuccessOrFailureCallbackConsumer;


/**
 * A very simple implementation of
 * {@link SuccessOrFailureCallbackConsumer<String>} that takes a single String
 * as the String to return as soon as it consumes the callback and never calls
 * {@link  SuccessOrFailureCallback#setInvalid(Throwable)}.
 */
public final class SimpleInputStreamSuccessOrFailureCallbackConsumer implements
        SuccessOrFailureCallbackConsumer<InputStream> {
    
    private final InputStream s;
    
    SimpleInputStreamSuccessOrFailureCallbackConsumer(InputStream s) {
        this.s = s;
    }
    
    public void consume(SuccessOrFailureCallback<InputStream> callback) {
        callback.process(s);
    }
}
