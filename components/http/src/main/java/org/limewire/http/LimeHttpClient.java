package org.limewire.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.params.HttpParams;

/**
 * An extension to the <code>org.apache.http.client.HttpClient</code> interface to provide
 * helper methods.
 */
public interface LimeHttpClient extends HttpClient {
    /**
     * Sets <code>HttpParams<code>
     * @param params the params to use
     */
    public void setParams(HttpParams params);

    /**
     * Does any necessary cleanup to allow 
     * all underlying connections to be able to be reused or closed.
     * 
     * @param request the request to cleanup
     * @param response the response to cleanup
     */
    public void releaseConnection(HttpRequest request, HttpResponse response);
}
