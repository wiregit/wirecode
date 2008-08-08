package org.limewire.swarm.http.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.protocol.HttpContext;

/**
 * An {@link SwarmHttpExecutionHandler} that iterates through a list of other
 * execution handlers, allowing each the opportunity to submit a request.
 * When one does have a request to submit, all subsequent states
 * associated with that request flow through to that specific handler.
 */
public class OrderedSwarmHttpExecutionHandler implements SwarmHttpExecutionHandler {
    
    private static final String LAST_HANDLER = "swarm.http.oeh.internal.lasthandler";
    
    private final List<SwarmHttpExecutionHandler> handlers;
    
    public OrderedSwarmHttpExecutionHandler(SwarmHttpExecutionHandler... handlers) {
        this.handlers = Arrays.asList(handlers);
    }

    public void finalizeContext(HttpContext context) {
        SwarmHttpExecutionHandler handler = (SwarmHttpExecutionHandler)context.getAttribute(LAST_HANDLER);
        if(handler != null) {
            handler.finalizeContext(context);
            context.setAttribute(LAST_HANDLER, null);
        }
    }

    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
        SwarmHttpExecutionHandler handler = (SwarmHttpExecutionHandler)context.getAttribute(LAST_HANDLER);
        handler.handleResponse(response, context);
    }

    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        SwarmHttpExecutionHandler handler = (SwarmHttpExecutionHandler)context.getAttribute(LAST_HANDLER);
        return handler.responseEntity(response, context);
    }

    public HttpRequest submitRequest(HttpContext context) {
        for(SwarmHttpExecutionHandler handler : handlers) {
            HttpRequest request = handler.submitRequest(context);
            if(request != null) {
                context.setAttribute(LAST_HANDLER, handler);
                return request;
            }
        }
        
        context.setAttribute(LAST_HANDLER, null);
        return null;
    }

    public void shutdown() {
        // TODO Auto-generated method stub
    }
    
    

}
