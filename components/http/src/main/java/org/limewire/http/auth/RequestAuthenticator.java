package org.limewire.http.auth;

import org.apache.http.protocol.HttpProcessor;
import org.apache.http.nio.protocol.NHttpRequestHandler;

public interface RequestAuthenticator extends HttpProcessor {
    NHttpRequestHandler guardedHandler(String url, NHttpRequestHandler handler);
}
