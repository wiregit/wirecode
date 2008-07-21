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
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.http.SwarmExecutionContext;
import org.limewire.swarm.http.SwarmHttpUtils;
import org.limewire.swarm.http.SwarmSource;
import org.limewire.swarm.http.listener.SwarmContentListener;
import org.limewire.swarm.http.listener.ResponseContentListener;

public class SwarmFileExecutionHandler implements ExecutionHandler {
    private static final Log LOG = LogFactory.getLog(SwarmFileExecutionHandler.class);

    private final SwarmCoordinator fileCoordinator;

    public SwarmFileExecutionHandler(SwarmCoordinator fileCoordinator) {
        this.fileCoordinator = fileCoordinator;
    }

    public void finalizeContext(HttpContext context) {
        // Explicitly close content listener, if it was set.
        ResponseContentListener contentListener = (ResponseContentListener) context
                .getAttribute(SwarmExecutionContext.SWARM_RESPONSE_LISTENER);
        if (contentListener != null) {
            contentListener.finished();
            context.setAttribute(SwarmExecutionContext.SWARM_RESPONSE_LISTENER, null);
        }
    }

    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
    }

    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            Range range = (Range) context.getAttribute(SwarmExecutionContext.SWARM_RANGE);
            ResponseContentListener responseContentListener = new SwarmContentListener(
                    fileCoordinator, range);
            context.setAttribute(SwarmExecutionContext.SWARM_RESPONSE_LISTENER, responseContentListener);
            return new ConsumingNHttpEntityTemplate(response.getEntity(), responseContentListener);
        } else {
            return null;
        }
    }

    public HttpRequest submitRequest(HttpContext context) {
        SwarmSource source = (SwarmSource) context
        .getAttribute(SwarmExecutionContext.HTTP_SWARM_SOURCE);
        
        Range range = source.getRange(); 

        range = fileCoordinator.lease(range);

        if (range == null) {
            LOG.debug("No range available to lease.");
            SwarmHttpUtils.closeConnectionFromContext(context);
            return null;
        }

        HttpRequest request = new BasicHttpRequest("GET", source.getPath());
        request.addHeader(new BasicHeader("Range", "bytes=" + range.getLow() + "-"
                + (range.getHigh())));
        
        context.setAttribute(SwarmExecutionContext.SWARM_RANGE, range);
        return request;
    }
}
