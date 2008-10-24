package org.limewire.http.auth;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.nio.protocol.NHttpRequestHandler;

public interface RequestAuthenticator extends HttpRequestInterceptor {
    NHttpRequestHandler guardedHandler(String url, NHttpRequestHandler handler);
}
