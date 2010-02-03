package org.limewire.core.impl.rest.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HttpCoreUtils;

/**
 * Base class for REST service request handlers.
 */
abstract class AbstractRestRequestHandler extends SimpleNHttpRequestHandler {

    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String POST = "POST";
    public static final String DELETE = "DELETE";

    @Override
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }
    
    /**
     * Parses the URI in the specified request, and returns URI target.  The
     * target is the piece between the prefix and the query parameters.  For
     * example, if the URI is "http://localhost/remote/library/files?type=audio"
     * and the prefix is "/library", then the target is "/files". 
     */
    protected String getUriTarget(HttpRequest request, String uriPrefix) throws IOException {
        // Get uri string.
        String uriStr = request.getRequestLine().getUri();
        
        // Strip off uri prefix.
        int pos = uriStr.indexOf(uriPrefix);
        if (pos < 0) throw new IOException("Invalid URI");
        String uriTarget = uriStr.substring(pos + uriPrefix.length());
        
        // Strip off query parameters.
        pos = uriTarget.indexOf("?");
        return (pos < 0) ? uriTarget : uriTarget.substring(0, pos);
    }
    
    /**
     * Returns a map of name/value pairs corresponding to the query parameters
     * in the specified request.
     */
    protected Map<String, String> getQueryParams(HttpRequest request) throws IOException {
        try {
            // Get uri string.
            String uriStr = request.getRequestLine().getUri();
            
            // Get query parameters.
            URI uri = new URI(uriStr);
            return HttpCoreUtils.parseQuery(uri, null);
            
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
