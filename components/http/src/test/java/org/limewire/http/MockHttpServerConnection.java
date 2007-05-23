/**
 * 
 */
package org.limewire.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.params.HttpParams;

public class MockHttpServerConnection extends DefaultNHttpServerConnection {

    public MockHttpServerConnection(IOSession session,
            HttpRequestFactory requestFactory, HttpParams params) {
        super(session, requestFactory, params);
    }

    public void setHttpRequest(HttpRequest request) {
        this.request = request;
    }

    public void setHttpResponse(HttpResponse response) {
        this.response = response;
    }

    public void setContentEncoder(ContentEncoder encoder) {
        this.contentEncoder = encoder;
    }

}