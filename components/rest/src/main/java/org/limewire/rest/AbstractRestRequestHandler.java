package org.limewire.rest;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;

/**
 * Base class for REST service request handlers.
 */
abstract class AbstractRestRequestHandler extends SimpleNHttpRequestHandler {

    /**
     * Default implementation always returns null.
     */
    @Override
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }
}
