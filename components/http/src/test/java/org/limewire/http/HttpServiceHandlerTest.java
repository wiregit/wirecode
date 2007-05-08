package org.limewire.http;

import junit.framework.TestCase;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpProcessor;

public class HttpServiceHandlerTest extends TestCase {

    private HttpParams parms;

    private ConnectionReuseStrategy connStrategy;

    private HttpResponseFactory responseFactory;

    private HttpProcessor httpProcessor;

    private HttpServiceHandler serviceHandler;

    private DefaultHttpRequestFactory requestFactory;

    private DefaultServerIOEventDispatch eventDispatch;

    public HttpServiceHandlerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        parms = new BasicHttpParams();
        HttpConnectionParams.setSocketBufferSize(parms, 512);
        connStrategy = new DefaultConnectionReuseStrategy();
        responseFactory = new DefaultHttpResponseFactory();
        requestFactory = new DefaultHttpRequestFactory();
        httpProcessor = new BasicHttpProcessor();
        serviceHandler = new HttpServiceHandler(httpProcessor, responseFactory,
                connStrategy, parms);
        eventDispatch = new DefaultServerIOEventDispatch(serviceHandler, parms);
    }

    public void testRequestReceived() throws Exception {
        MockSocket socket = new MockSocket();
        HttpIOSession session = new HttpIOSession(socket);
        HttpChannel channel = new HttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        DefaultNHttpServerConnection conn = new DefaultNHttpServerConnection(session, requestFactory, parms);
        serviceHandler.connected(conn);
    }

}
