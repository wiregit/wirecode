package org.limewire.core.impl.rest.handler;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;

/**
 * Request handler for Search services.
 */
class SearchRequestHandler extends SimpleNHttpRequestHandler {

    @Inject
    public SearchRequestHandler() {
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
    }

    @Override
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }

}
