/**
 * 
 */
package org.limewire.swarm.http;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.entity.ContentListener;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

class SwarmNHttpRequestExecutionHandlerImpl<E> implements SwarmNHttpRequestExecutionHandler<E> {
    
    private static final Log LOG = LogFactory.getLog(SwarmNHttpRequestExecutionHandlerImpl.class);
    
    private static final String ATT = "limewire.swarm.attachment";

    public void finalizeContext(HttpContext context) {
        Attachment<E> att = getLocalAttachment(context);
        att.swarmer.cleanup(att.object);
    }

    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
        Attachment<E> att = getLocalAttachment(context);
        att.swarmer.responseProcessed(response, att.object);
    }

    public void initalizeContext(HttpContext context, Object attachment) {
        context.setAttribute(ATT, attachment);
        Attachment<E> att = getLocalAttachment(context);
        att.swarmer.connectionEstablished(att.object);
    }

    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Got response with statusline: " + response.getStatusLine() + ", headers: " + Arrays.asList(response.getAllHeaders()));
        
        int code = response.getStatusLine().getStatusCode();
        if(code == HttpStatus.SC_OK || code == HttpStatus.SC_PARTIAL_CONTENT) {
            Attachment<E> att = getLocalAttachment(context);
            ContentListener contentListener = att.swarmer.getContentListener(response, att.object);
            return new ConsumingNHttpEntityTemplate(response.getEntity(), contentListener);
        } else {
            return null;
        }
    }

    public HttpRequest submitRequest(HttpContext context) {
        Attachment<E> att = getLocalAttachment(context);
        try {
            return att.swarmer.getHttpRequest(att.object);
        } catch(IOException iox) {
            LOG.warn("IOException generating http request", iox);
            close(context);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private Attachment<E> getLocalAttachment(HttpContext context) {
        return (SwarmNHttpRequestExecutionHandlerImpl.Attachment)context.getAttribute(ATT);
    }
    
    private HttpConnection getConnection(HttpContext context) {
        return (HttpConnection)context.getAttribute(ExecutionContext.HTTP_CONNECTION);
    }
    
    private void close(HttpContext context) {
        try {
            getConnection(context).close();
        } catch(IOException iox) {
            LOG.warn("Error closing connection", iox);
        }
    }

    /**
     * Creates an object that can be attached to the given swarmer.
     * The attachment can later be retrieved with an HttpContext by calling
     * {@link #getAttachmentForContext(HttpContext)}.
     */
    public Object createAttachment(HttpSwarmHandler<E> swarmer, E attachment) {
        return new Attachment<E>(swarmer, attachment);
    }
    
    private static class Attachment<T> {
        final T object;
        final HttpSwarmHandler<T> swarmer;
        
        public Attachment(HttpSwarmHandler<T> swarmer, T object) {
            this.object = object;
            this.swarmer = swarmer;
        }
        
        @Override
        public String toString() {
            return "LocalAttachment for swarmer: " + swarmer + ", object: " + object;
        }
    }
}