package org.limewire.swarm.http;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.limewire.swarm.http.handler.ExecutionHandler;

public class ExecutionHandlerAdapter implements ExecutionHandler {

    public void finalizeContext(HttpContext context) {
        // TODO Auto-generated method stub
        
    }

    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
        // TODO Auto-generated method stub
        
    }

    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public HttpRequest submitRequest(HttpContext context) {
        // TODO Auto-generated method stub
        return null;
    }

}
