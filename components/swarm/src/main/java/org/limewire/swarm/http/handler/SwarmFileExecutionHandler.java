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
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.file.SwarmFile;
import org.limewire.swarm.http.SwarmExecutionContext;
import org.limewire.swarm.http.SwarmHttpUtils;
import org.limewire.swarm.http.SwarmSource;
import org.limewire.swarm.http.listener.ResponseContentListener;
import org.limewire.swarm.http.listener.SwarmContentListener;

public class SwarmFileExecutionHandler implements ExecutionHandler {
    private static final Log LOG = LogFactory.getLog(SwarmFileExecutionHandler.class);

    private static final String RESPONSE_LISTENER = "swarm.http.fileswarmer.internal.responseListener";

    private final SwarmCoordinator fileCoordinator;

    public SwarmFileExecutionHandler(SwarmCoordinator fileCoordinator) {
        this.fileCoordinator = fileCoordinator;
    }

    public void finalizeContext(HttpContext context) {
        // Explicitly close content listener, if it was set.
        ResponseContentListener contentListener = (ResponseContentListener) context
                .getAttribute(RESPONSE_LISTENER);
        if (contentListener != null) {
            contentListener.finished();
            context.setAttribute(RESPONSE_LISTENER, null);
        }
    }

    public void handleResponse(HttpResponse response, HttpContext context) throws IOException {
    }

    public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
            throws IOException {
        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            ResponseContentListener listener = (ResponseContentListener) context
                    .getAttribute(RESPONSE_LISTENER);
            listener.initialize(response);
            context.setAttribute(RESPONSE_LISTENER, null);
            return new ConsumingNHttpEntityTemplate(response.getEntity(), listener);
        } else {
            ResponseContentListener listener = (ResponseContentListener) context
                    .getAttribute(RESPONSE_LISTENER);
            listener.finished();
            context.setAttribute(RESPONSE_LISTENER, null);
            return null;
        }
    }

    public HttpRequest submitRequest(HttpContext context) {
        Range range;

        SwarmSource source = (SwarmSource) context
                .getAttribute(SwarmExecutionContext.HTTP_SWARM_SOURCE);
        IntervalSet availableRanges = source.getAvailableRanges();

        range = fileCoordinator.leasePortion(availableRanges);

        if (range == null) {
            LOG.debug("No range available to lease.");
            SwarmHttpUtils.closeConnectionFromContext(context);
            return null;
        }

        SwarmFile swarmFile = fileCoordinator.getFirstSwarmFile(range);
        long fileEndByte = swarmFile.getEndByte();

        if (range.getHigh() > fileEndByte) {
            Range oldRange = range;
            range = Range.createRange(range.getLow(), fileEndByte);
            range = fileCoordinator.release(oldRange, range);
        }

        long startRange = range.getLow();
        long endRange = range.getHigh();

        String path = source.getPath().trim();
        if (path.charAt(path.length() - 1) == '/') {
            path += swarmFile.getPath();
        }

        HttpRequest request = new BasicHttpRequest("GET", path);
        request.addHeader(new BasicHeader("Range", "bytes=" + startRange + "-" + (endRange)));
        context.setAttribute(RESPONSE_LISTENER, new SwarmContentListener(fileCoordinator,
                swarmFile, range));
        return request;
    }
}
