package org.limewire.swarm.http.gnutella;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.limewire.swarm.http.handler.ExecutionHandler;

/**
 * An {@link ExecutionHandler} that wraps another handler,
 * respecting queue statuses.
 * 
 * That is, if a prior request had returned a 503 Queued,
 * this will prevent further requests from being sent until
 * the queue time has elapsed.
 */
public class QueuableExecutionHandler implements ExecutionHandler {
        
    private final ExecutionHandler delegateHandler;
    private final QueueController queueController;
    
    public QueuableExecutionHandler(ExecutionHandler delegateHandler,
            QueueController queueController) {
        this.delegateHandler = delegateHandler;
        this.queueController = queueController;
    }

    public void finalizeContext(HttpContext context) {
        delegateHandler.finalizeContext(context);
        
        QueueInfo qInfo = (QueueInfo)context.getAttribute(GnutellaExecutionContext.QUEUE_INFO);
        if(qInfo != null) {
            queueController.removeFromQueue(qInfo);
            context.setAttribute(GnutellaExecutionContext.QUEUE_INFO, null);
        }
    }

    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
        delegateHandler.handleResponse(response, context);

        QueueInfo oldQueueInfo = (QueueInfo)context.getAttribute(GnutellaExecutionContext.QUEUE_INFO);
        context.setAttribute(GnutellaExecutionContext.QUEUE_INFO, null);
        
        String qHeader = null;
        if(response.getStatusLine().getStatusCode() == 503) {
            Header header = response.getFirstHeader("X-Queue");
            if(header != null && header.getValue() != null) {
                qHeader = header.getValue();
            }
        }
        
        QueueInfo qInfo = null;
        if(qHeader != null) {
            try {
                qInfo = queueController.addToQueue(qHeader, (IOControl)context.getAttribute(ExecutionContext.HTTP_CONNECTION));
                if(qInfo != null) {
                    context.setAttribute(GnutellaExecutionContext.QUEUE_INFO, qInfo);
                }
            } catch(ProtocolException ignored) {
                // Just pretend we got a normal busy response.
            }
        }
        
        // If we used to be queued, but no longer are,
        // update the queue status.
        if(qInfo == null && oldQueueInfo != null) {
            queueController.removeFromQueue(oldQueueInfo);
        }
    }

    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        return delegateHandler.responseEntity(response, context);
    }

    public HttpRequest submitRequest(HttpContext context) {
        QueueInfo qInfo = (QueueInfo)context.getAttribute(GnutellaExecutionContext.QUEUE_INFO);
        if(qInfo != null && qInfo.isQueued()) {
            qInfo.enqueue();
            return null;
        } else {
            return delegateHandler.submitRequest(context);
        }
    }

}
