package org.limewire.swarm.http.gnutella;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.http.SwarmHttpExecutionContext;
import org.limewire.swarm.http.SwarmHttpUtils;

public class GnutellaDownloadReuseStrategy implements ConnectionReuseStrategy {
    
    private final ConnectionReuseStrategy keepAliveStrategy = new DefaultConnectionReuseStrategy();
    
    public boolean keepAlive(HttpResponse response, HttpContext context) {
        // Never keep something alive that wants to close.
        if(!keepAliveStrategy.keepAlive(response, context)) {
            return false;
        }
        
        int code = response.getStatusLine().getStatusCode();
        
        // If it was a valid response, let it continue.
        if(code >= 200 && code < 300) {
            return true;
        }
        
        // Otherwise, we have a little more trouble.
        switch(code) {
        case 416: return isNoRangeAvailableValid(response, context);
        case 503: return isQueued(response, context);
        }
        
        // Any other code is a flat out "DO NOT CONTINUE".
        return false;
    }
    
    private boolean isQueued(HttpResponse response, HttpContext context) {
        QueueInfo qInfo = (QueueInfo)context.getAttribute(GnutellaExecutionContext.QUEUE_INFO);
        return qInfo != null && qInfo.isQueued();
    }

    /**
     * Returns true if the response is a valid 'no range available' response.
     * Normally, we'd just try again with another range, but we need to prevent
     * a malformed uploader sending this over and over...
     */
    private boolean isNoRangeAvailableValid(HttpResponse response, HttpContext context) {
        IntervalSet availableRanges = (IntervalSet) context
                .getAttribute(SwarmHttpExecutionContext.HTTP_AVAILABLE_RANGES);
        if (availableRanges == null || availableRanges.isEmpty()) {
            return false;
        }

        HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
        assert request != null;
        Header rangeRequest = request.getFirstHeader("Range");
        if (rangeRequest == null || rangeRequest.getValue() == null) {
            return false;
        }

        Range requestedRange = SwarmHttpUtils.rangeForRequest(rangeRequest.getValue());
        assert requestedRange != null; // We built this ourselves, it should always exist.
        for (Range availableRange : availableRanges) {
            if (requestedRange.isSubrange(availableRange))
                return false;
        }

        return true;
    }

}
