package org.limewire.swarm.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.nio.NIODispatcher;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceEventListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.http.handler.SwarmCoordinatorHttpExecutionHandler;
import org.limewire.swarm.http.handler.SwarmHttpExecutionHandler;
import org.limewire.swarm.impl.ReconnectingSourceEventListener;

import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.util.LimeWireUtils;

public class SwarmHttpSourceHandler implements SwarmSourceHandler, NHttpRequestExecutionHandler {

    private static final Log LOG = LogFactory.getLog(SwarmHttpSourceHandler.class);

    private final SwarmSourceEventListener defaultSourceEventListener;

    private final LimeConnectingIOReactor ioReactor;

    private final IOEventDispatch eventDispatch;

    private final SwarmHttpExecutionHandler executionHandler;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final SwarmCoordinator swarmCoordinator;

    public SwarmHttpSourceHandler(SwarmCoordinator swarmCoordinator) {
        this(swarmCoordinator, new ReconnectingSourceEventListener());
    }

    public SwarmHttpSourceHandler(SwarmCoordinator swarmCoordinator,
            SwarmSourceEventListener defaultSourceEventListener) {
        this.swarmCoordinator = swarmCoordinator;
        if (defaultSourceEventListener == null) {
            this.defaultSourceEventListener = new ReconnectingSourceEventListener();
        } else {
            this.defaultSourceEventListener = defaultSourceEventListener;
        }

        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(
                CoreConnectionPNames.CONNECTION_TIMEOUT, 2000).setIntParameter(
                CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(
                CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setParameter(
                CoreProtocolPNames.USER_AGENT, LimeWireUtils.getHttpServer());

        this.ioReactor = new LimeConnectingIOReactor(params, NIODispatcher.instance()
                .getScheduledExecutorService(), new SocketsManagerImpl());

        this.executionHandler = new SwarmCoordinatorHttpExecutionHandler(swarmCoordinator);

        SwarmAsyncNHttpClientHandlerBuilder builder = new SwarmAsyncNHttpClientHandlerBuilder(
                params, this);

        AsyncNHttpClientHandler clientHandler = builder.get();

        eventDispatch = new DefaultClientIOEventDispatch(clientHandler, params);
    }

    public void addSource(SwarmSource source, SwarmSourceEventListener sourceEventListener) {
        SwarmSourceEventListener listener = defaultSourceEventListener;
        if (sourceEventListener != null) {
            listener = sourceEventListener;
        }
        SessionRequestCallback sessionRequestCallback = new SwarmHttpSessionRequestCallback(this,
                source, listener);
        ioReactor.connect(source.getAddress(), null, new HttpSourceInfo(source, listener),
                sessionRequestCallback);
    }

    public void addSource(SwarmSource source) {
        addSource(source, defaultSourceEventListener);
    }

    public boolean isComplete() {
        return swarmCoordinator.isComplete();
    }

    public void shutdown() throws IOException {
        started.set(false);
        ioReactor.shutdown();
    }

    public void start() throws IOException {
        started.set(true);
        ioReactor.execute(eventDispatch);
    }

    public boolean isActive() {
        return started.get();
    }

    public void initalizeContext(HttpContext context, Object attachment) {
        HttpSourceInfo info = (HttpSourceInfo) attachment;
        context.setAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE, info.getSource());
        context.setAttribute(SwarmHttpExecutionContext.SWARM_SOURCE_LISTENER, info
                .getSourceEventListener());
    }

    public void finalizeContext(HttpContext context) {
        SwarmSourceEventListener listener = (SwarmSourceEventListener) context
                .getAttribute(SwarmHttpExecutionContext.SWARM_SOURCE_LISTENER);
        SwarmSource source = (SwarmSource) context
                .getAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE);
        listener.connectionClosed(SwarmHttpSourceHandler.this, source);

        executionHandler.finalizeContext(context);
    }

    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
        if (isActive()) {
            SwarmSourceEventListener listener = (SwarmSourceEventListener) context
                    .getAttribute(SwarmHttpExecutionContext.SWARM_SOURCE_LISTENER);
            SwarmSource source = (SwarmSource) context
                    .getAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE);
            listener.responseProcessed(SwarmHttpSourceHandler.this, source,
                    new SwarmHttpSourceStatus(response.getStatusLine()));

            executionHandler.handleResponse(response, context);
        }
    }

    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {

        if (isActive()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Handling response: " + response.getStatusLine() + ", headers: "
                        + Arrays.asList(response.getAllHeaders()));
            }
            ConsumingNHttpEntity entity = executionHandler.responseEntity(response, context);
            return entity;
        } else {
            throw new IOException("Not active!");
        }
    }

    public HttpRequest submitRequest(HttpContext context) {
        if (isActive()) {
            HttpRequest request = executionHandler.submitRequest(context);

            if (LOG.isTraceEnabled() && request != null) {
                LOG.trace("Submitting request: " + request.getRequestLine());
            }
            if (request == null) {
                SwarmSource swarmSource = (SwarmSource) context
                        .getAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE);
                SwarmSourceEventListener sourceListener = (SwarmSourceEventListener) context
                        .getAttribute(SwarmHttpExecutionContext.SWARM_SOURCE_LISTENER);
                if (swarmSource != null && sourceListener != null) {
                    sourceListener.finished(SwarmHttpSourceHandler.this, swarmSource);
                }
            }
            return request;
        } else {
            SwarmHttpUtils.closeConnectionFromContext(context);
            return null;
        }
    }

    public float getMeasuredBandwidth(boolean downstream) {
        try {
            return ioReactor.getMeasuredBandwidth(downstream);
        } catch (InsufficientDataException ide) {
            return 0;
        }
    }
}
