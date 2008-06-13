package org.limewire.swarm.http.handler;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.file.FileCoordinator;
import org.limewire.swarm.http.SwarmExecutionContext;
import org.limewire.swarm.http.SwarmHttpUtils;
import org.limewire.swarm.http.SwarmSource;
import org.limewire.swarm.http.listener.FileContentListener;
import org.limewire.swarm.http.listener.ResponseContentListener;

public class SwarmFileExecutionHandler implements ExecutionHandler {
    private static final Log LOG = LogFactory.getLog(SwarmFileExecutionHandler.class);
    
    private static final String RESPONSE_LISTENER = "swarm.http.fileswarmer.internal.responseListener";
    
    private final FileCoordinator fileCoordinator;
    
    public SwarmFileExecutionHandler(FileCoordinator fileCoordinator) {
        this.fileCoordinator = fileCoordinator;
    }
    
    public void finalizeContext(HttpContext context) {
        // Explicitly close content listener, if it was set.
        ResponseContentListener contentListener = (ResponseContentListener)context.getAttribute(RESPONSE_LISTENER);
        if(contentListener != null) {
            contentListener.finished();
            context.setAttribute(RESPONSE_LISTENER, null);
        }
    }
    
    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
    }
    
    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        ResponseContentListener listener =  (ResponseContentListener)context.getAttribute(RESPONSE_LISTENER);
        listener.initialize(response);
        return new ConsumingNHttpEntityTemplate(response.getEntity(), listener);
    }
    
    public HttpRequest submitRequest(HttpContext context) {
        Range range;
        
        SwarmSource source = (SwarmSource)context.getAttribute(SwarmExecutionContext.HTTP_SWARM_SOURCE);
        if(source.isRangeRequestSupported()) {
            range = fileCoordinator.leasePortion(getAvailableRanges(context));
        } else {
            range = fileCoordinator.lease();
        }
        
        if(range == null) {
            LOG.debug("No range available to lease.");
            SwarmHttpUtils.closeConnectionFromContext(context);
            return null;
        }
        
        HttpRequest request = new BasicHttpRequest("GET", source.getUri());
        request.addHeader(new BasicHeader("Range", "bytes=" + range.getLow() + "-" + (range.getHigh())));
        context.setAttribute(RESPONSE_LISTENER, new FileContentListener(fileCoordinator, range));
        return request;
    }

    private IntervalSet getAvailableRanges(HttpContext context) {
        return (IntervalSet)context.getAttribute(SwarmExecutionContext.HTTP_AVAILABLE_RANGES);
    }
    
}
