package org.limewire.http;

import java.util.Map;

import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;

public class SynchronizedHttpRequestHandlerRegistry extends
        HttpRequestHandlerRegistry {

    @Override
    public synchronized HttpRequestHandler lookup(String requestURI) {
        return super.lookup(requestURI);
    }

    @Override
    protected synchronized boolean matchUriRequestPattern(String pattern, String requestUri) {
        return super.matchUriRequestPattern(pattern, requestUri);
    }

    @Override
    public synchronized void register(String pattern, HttpRequestHandler handler) {
        super.register(pattern, handler);
    }

    @Override
    public synchronized void setHandlers(Map map) {
        super.setHandlers(map);
    }

    @Override
    public synchronized void unregister(String pattern) {
        super.unregister(pattern);
    }

}
