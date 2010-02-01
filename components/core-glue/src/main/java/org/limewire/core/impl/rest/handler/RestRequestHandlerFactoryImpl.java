package org.limewire.core.impl.rest.handler;

import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;

/**
 * Default implementation for RestRequestHandlerFactory.
 */
public class RestRequestHandlerFactoryImpl implements RestRequestHandlerFactory {

    @Inject
    public RestRequestHandlerFactoryImpl() {
    }
    
    @Override
    public NHttpRequestHandler createRequestHandler(RestTarget restTarget) {
        switch (restTarget) {
        case HELLO:
            return new HelloRequestHandler();
        case LIBRARY:
            // TODO implement library handler
            return new UnknownRequestHandler();
        default:
            return new UnknownRequestHandler();
        }
    }

    /**
     * Request handler for "hello world" service.
     */
    private static class HelloRequestHandler extends SimpleNHttpRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            System.out.println("handler.handle() thread=" + Thread.currentThread().getName());//TODO
            System.out.println("-> request  = " + request.getRequestLine().getUri());
            System.out.println("-> response = " + response.getProtocolVersion());
            
            // Set entity and status in response.
            Date dateTime = new Date(System.currentTimeMillis());
            NStringEntity entity = new NStringEntity(dateTime.toString() + ": Hello world!");
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);
            System.out.println("-> entity   = " + entity.getContentType());//TODO
        }

        @Override
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            System.out.println("handler.entityRequest()");//TODO
            return null;
        }
    }
    
    /**
     * Request handler for an unknown service.
     */
    private static class UnknownRequestHandler extends SimpleNHttpRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Override
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
    }
}
