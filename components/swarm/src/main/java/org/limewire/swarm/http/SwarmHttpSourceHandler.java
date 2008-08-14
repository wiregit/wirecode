package org.limewire.swarm.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.nio.NIODispatcher;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.http.listener.ResponseContentListener;
import org.limewire.swarm.http.listener.SwarmHttpContentListener;
import org.limewire.util.Objects;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * The SwarmHttpSource handler is responsible for processing http source
 * connections.
 * 
 * It will attempt to lease a range of bytes to download from the given
 * SwarmCoordinator, then it will attempt to download those bytes by submitting
 * a Partial Content http request to the connections server. Upon a successful
 * download it will write the bytes in batch asynchronously as they come in on
 * the reading connection.
 * 
 * When finished writing the bytes it has read from the connection it will try
 * to lease more bytes and begin the process anew.
 * 
 * When there are no more bytes left to lease, the swarm sources state will be
 * marked as finished, and the connection to it will be closed.
 * 
 * Additionally the swarmSource can be checked to see if it is finished by use
 * of the isFinished() flag. This can be used to override teh default behavior,
 * which is to download until there are no more bytes to lease.
 * 
 */
public class SwarmHttpSourceHandler implements SwarmSourceHandler, NHttpRequestExecutionHandler {

    private static final Log LOG = LogFactory.getLog(SwarmHttpSourceHandler.class);

    private final LimeConnectingIOReactor ioReactor;

    private final IOEventDispatch eventDispatch;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final SwarmCoordinator swarmCoordinator;

    private final List<SwarmSource> sources;

    private final List<SwarmSource> badSources;

    private final SwarmSourceStats stats;

    public SwarmHttpSourceHandler(SwarmCoordinator swarmCoordinator,
            LimeConnectingIOReactor ioReactor, IOEventDispatch ioEventDispatch) {
        this.swarmCoordinator = swarmCoordinator;
        this.ioReactor = ioReactor;
        this.eventDispatch = ioEventDispatch;
        this.sources = Collections.synchronizedList(new ArrayList<SwarmSource>());
        this.badSources = Collections.synchronizedList(new ArrayList<SwarmSource>());
        this.stats = new SwarmSourceStats();
    }

    public SwarmHttpSourceHandler(SwarmCoordinator swarmCoordinator) {
        this.swarmCoordinator = Objects.nonNull(swarmCoordinator, "swarmCoordinator");
        this.sources = Collections.synchronizedList(new ArrayList<SwarmSource>());
        this.badSources = Collections.synchronizedList(new ArrayList<SwarmSource>());
        this.stats = new SwarmSourceStats();

        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(
                CoreConnectionPNames.CONNECTION_TIMEOUT, 2000).setIntParameter(
                CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(
                CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setParameter(
                CoreProtocolPNames.USER_AGENT, LimeWireUtils.getHttpServer());

        this.ioReactor = new LimeConnectingIOReactor(params, NIODispatcher.instance()
                .getScheduledExecutorService(), new SocketsManagerImpl());

        SwarmAsyncNHttpClientHandlerBuilder builder = new SwarmAsyncNHttpClientHandlerBuilder(
                params, this);

        AsyncNHttpClientHandler clientHandler = builder.get();

        eventDispatch = new DefaultClientIOEventDispatch(clientHandler, params);

    }

    public void addSource(SwarmSource source) {
        LOG.trace("Adding source: " + source);
        stats.incrementNumberOfSources();

        SessionRequestCallback sessionRequestCallback = new SwarmHttpSessionRequestCallback(source);

        LOG.trace("Connecting source to ioReactor: " + source);

        ioReactor.connect(source.getAddress(), null, new HttpSourceAttachment(source),
                sessionRequestCallback);
    }

    public List<SwarmSource> getSources() {
        return new ArrayList<SwarmSource>(sources);
    }

    public boolean isComplete() {
        return swarmCoordinator.isComplete();
    }

    public void shutdown() throws IOException {
        LOG.trace("Shutting down: " + this);
        started.set(false);
        LOG.trace("Shutting ioReactor: " + ioReactor);
        ioReactor.shutdown();
    }

    public void start() throws IOException {
        LOG.trace("Starting: " + this);
        started.set(true);
        ioReactor.execute(eventDispatch);
    }

    public boolean isActive() {
        return started.get();
    }

    public void initalizeContext(HttpContext context, Object attachment) {
        LOG.trace("initalizeContext: " + this);
        HttpSourceAttachment info = (HttpSourceAttachment) attachment;
        context.setAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE, info.getSource());
    }

    public void finalizeContext(HttpContext context) {
        LOG.trace("finalizeContext: " + this);
        SwarmSource source = getSwarmSource(context);
        source.connectionClosed(SwarmHttpSourceHandler.this);
        closeContentListener(context);
    }

    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
        LOG.trace("handleResponse: " + this);
        stats.incrementNumberOfResponses();
        if (isActive()) {
            SwarmSource source = getSwarmSource(context);
            source.responseProcessed(SwarmHttpSourceHandler.this, new SwarmHttpSourceStatus(
                    response.getStatusLine()));

            LOG.trace(SwarmHttpUtils.logReponse("response", response));

            int code = response.getStatusLine().getStatusCode();
            if (!(code >= 200 && code < 300)) {
                closeConnection(context);
            }
        }

    }

    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        LOG.trace(this);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Handling response: " + response.getStatusLine() + ", headers: "
                    + Arrays.asList(response.getAllHeaders()));
        }
        ResponseContentListener listener = (ResponseContentListener) context
                .getAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER);
        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            listener.initialize(response);
            context.setAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER, null);
            return new ConsumingNHttpEntityTemplate(response.getEntity(), listener);
        } else {
            listener.finished();
            context.setAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER, null);
            return null;
        }

    }

    public HttpRequest submitRequest(HttpContext context) {
        LOG.trace(this);

        if (isActive()) {
            SwarmSource swarmSource = getSwarmSource(context);
            HttpRequest request = null;
            if (swarmSource.isFinished() || isComplete()) {
                LOG.trace("swarmSource.isFinished(): " + swarmSource.isFinished()
                        + " isComplete(): " + isComplete());
            } else {
                request = buildRequest(context);
                LOG.trace(SwarmHttpUtils.logRequest("submitRequest", request));
            }

            if (request == null) {

                LOG.debug("No more data to request for this swarm source: " + swarmSource
                        + " finishing all listeners then closing connection from context.");
                closeConnection(context);
            } else {
                stats.incrementNumberOfRequests();
            }
            return request;
        } else {
            LOG.warn("submitRequest called while not active!");
            closeConnection(context);
            return null;
        }
    }

    private SwarmSource getSwarmSource(HttpContext context) {
        SwarmSource swarmSource = (SwarmSource) context
                .getAttribute(SwarmHttpExecutionContext.HTTP_SWARM_SOURCE);
        return swarmSource;
    }

    private HttpRequest buildRequest(HttpContext context) {
        HttpRequest request = null;
        SwarmSource source = getSwarmSource(context);
        RequestParameters requestParameters = buildRequestParameters(source);

        if (requestParameters != null) {
            SwarmFile swarmFile = requestParameters.getSwarmFile();
            Range leaseRange = requestParameters.getLeaseRange();
            Range downloadRange = requestParameters.getDownloadRange();
            stats.incrementNumberOfBytesRequested(downloadRange.getLength());

            context.setAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER,
                    new SwarmHttpContentListener(swarmCoordinator, swarmFile, leaseRange));

            request = new BasicHttpRequest("GET", requestParameters.getPath());
            request.addHeader(new BasicHeader("Range", "bytes=" + requestParameters.getLow() + "-"
                    + (requestParameters.getHigh())));
        }
        return request;
    }

    public float getMeasuredBandwidth(boolean downstream) {
        return ioReactor.getMeasuredBandwidth(downstream);
    }

    private void closeContentListener(HttpContext context) {
        LOG.trace("closing content listener: " + this, new Exception("debugging stack trace"));
        ResponseContentListener contentListener = (ResponseContentListener) context
                .getAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER);
        if (contentListener != null) {
            contentListener.finished();
            context.setAttribute(SwarmHttpExecutionContext.SWARM_RESPONSE_LISTENER, null);
        }
    }

    private void closeConnection(HttpContext context) {
        LOG.trace("closing connection: " + this, new Exception("debugging stack trace"));
        closeContentListener(context);
        closeSwarmSource(context);
        SwarmHttpUtils.closeConnectionFromContext(context);
    }

    private void closeSwarmSource(HttpContext context) {
        LOG.trace("closing swarmSource: " + this, new Exception("debugging stack trace"));
        SwarmSource swarmSource = getSwarmSource(context);
        if (swarmSource != null) {
            swarmSource.finished(SwarmHttpSourceHandler.this);
        }
    }

    private RequestParameters buildRequestParameters(SwarmSource source) {
        IntervalSet availableRanges = source.getAvailableRanges();

        Range leaseRange = swarmCoordinator.leasePortion(availableRanges);

        if (leaseRange == null) {
            LOG.debug("No range available to lease.");
            return null;
        }

        SwarmFile swarmFile = swarmCoordinator.getSwarmFile(leaseRange);
        long fileEndByte = swarmFile.getEndByte();

        if (leaseRange.getHigh() > fileEndByte) {
            Range oldRange = leaseRange;
            leaseRange = Range.createRange(leaseRange.getLow(), fileEndByte);
            leaseRange = swarmCoordinator.renewLease(oldRange, leaseRange);
        }

        long downloadRangeStart = leaseRange.getLow() - swarmFile.getStartByte();
        long downloadRangeEnd = leaseRange.getHigh() - swarmFile.getStartByte();

        Range downloadRange = Range.createRange(downloadRangeStart, downloadRangeEnd);

        RequestParameters requestParameters = new RequestParameters(swarmFile, source.getPath(),
                leaseRange, downloadRange);
        return requestParameters;
    }

    private class RequestParameters {
        private final SwarmFile swarmFile;

        private final String path;

        private final Range leaseRange;

        private final Range downloadRange;

        public RequestParameters(SwarmFile swarmFile, String path, Range leaseRange,
                Range downloadRange) {
            super();
            this.swarmFile = Objects.nonNull(swarmFile, "swarmFile");
            this.path = Objects.nonNull(path, "path");
            this.leaseRange = Objects.nonNull(leaseRange, "leaseRange");
            this.downloadRange = Objects.nonNull(downloadRange, "downloadRange");
        }

        public String getPath() {
            String path = this.path;
            if (path.charAt(path.length() - 1) == '/') {
                path += swarmFile.getPath();
            }
            return path;
        }

        public long getLow() {
            return downloadRange.getLow();
        }

        public long getHigh() {
            return downloadRange.getHigh();
        }

        public SwarmFile getSwarmFile() {
            return swarmFile;
        }

        public Range getLeaseRange() {
            return leaseRange;
        }

        public Range getDownloadRange() {
            return downloadRange;
        }
    }

    private class SwarmHttpSessionRequestCallback implements SessionRequestCallback {
        private final SwarmSource source;

        public SwarmHttpSessionRequestCallback(SwarmSource source) {
            this.source = source;
        }

        public void cancelled(SessionRequest request) {
            sources.remove(source);
            source.connectFailed(SwarmHttpSourceHandler.this);
        };

        public void completed(SessionRequest request) {
            sources.add(source);
            source.connected(SwarmHttpSourceHandler.this);
        };

        public void failed(SessionRequest request) {
            sources.remove(source);
            badSources.add(source);// TODO
            source.connectFailed(SwarmHttpSourceHandler.this);
        };

        public void timeout(SessionRequest request) {
            sources.remove(source);
            badSources.add(source);// TODO
            source.connectFailed(SwarmHttpSourceHandler.this);
        };

    }

    public boolean hasSource(SwarmSource source) {
        return sources.contains(source);
    }

    public boolean isBadSource(SwarmSource source) {
        return badSources.contains(source);
    }
}
