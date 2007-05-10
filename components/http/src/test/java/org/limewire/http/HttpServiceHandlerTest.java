package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.util.BufferUtils;

public class HttpServiceHandlerTest extends TestCase {

    private HttpParams parms;

    private ConnectionReuseStrategy connStrategy;

    private HttpResponseFactory responseFactory;

    private DefaultHttpRequestFactory requestFactory;

    private MockHttpNIOEntity nioEntity;

    private StringEntity stringEntity;

    private DefaultServerIOEventDispatch eventDispatch;

    private HttpServiceHandler serviceHandler;

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

        BasicHttpProcessor httpProcessor = new BasicHttpProcessor();
        HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
        stringEntity = new StringEntity("abc");
        registry.register("/get/string", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setEntity(stringEntity);
            }
        });
        nioEntity = new MockHttpNIOEntity("abc");
        registry.register("/get/nio", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setEntity(nioEntity);
            }
        });
        serviceHandler = new HttpServiceHandler(
                httpProcessor, responseFactory, connStrategy, parms);
        serviceHandler.setHandlerResolver(registry);
        eventDispatch = new DefaultServerIOEventDispatch(serviceHandler, parms);
    }

    public void testEventListener() throws Exception {
        MockHttpServiceEventListener listener = new MockHttpServiceEventListener();
        serviceHandler.setEventListener(listener);

        MockSocket socket = new MockSocket();
        MockIOSession session = new MockIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);
        assertTrue(listener.connectionOpened);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        assertTrue(listener.requestReceived);
        
        MockContentEncoder encoder = new MockContentEncoder();
        conn.setContentEncoder(encoder);
        conn.produceOutput(serviceHandler);
        assertFalse(listener.responseSent);
        conn.produceOutput(serviceHandler);
        conn.produceOutput(serviceHandler);
        assertEquals("abc", encoder.data.toString());
        assertTrue(listener.responseSent);
        assertEquals(EventMask.READ, session.getEventMask());
    }

    public void testEventListenerException() throws Exception {
        MockHttpServiceEventListener listener = new MockHttpServiceEventListener();
        serviceHandler.setEventListener(listener);

        MockSocket socket = new MockSocket();
        MockIOSession session = new MockIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);
        assertTrue(listener.connectionOpened);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        assertTrue(listener.requestReceived);
        
        MockContentEncoder encoder = new MockContentEncoder();
        nioEntity.exception = new IOException();
        conn.setContentEncoder(encoder);
        conn.produceOutput(serviceHandler);
        assertNotNull(listener.exception);
        assertTrue(session.closed);
        
        serviceHandler.closed(conn);
        assertTrue(listener.connectionClosed);
    }

    public void testGetRequestNIOEntity() throws Exception {
        MockSocket socket = new MockSocket();
        MockIOSession session = new MockIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        assertEquals(nioEntity, conn.getHttpResponse().getEntity());
        
        MockContentEncoder encoder = new MockContentEncoder();
        conn.setContentEncoder(encoder);
        conn.produceOutput(serviceHandler);
        assertEquals("a", encoder.data.toString());
        assertFalse(encoder.isCompleted());
        assertTrue(nioEntity.initialized);
        assertFalse(nioEntity.finished);
        conn.produceOutput(serviceHandler);
        conn.produceOutput(serviceHandler);
        assertEquals("abc", encoder.data.toString());
        assertTrue(encoder.isCompleted());
        assertTrue(nioEntity.finished);
    }

    public void testGetRequestNIOEntityThrowingException() throws Exception {
        MockSocket socket = new MockSocket();
        MockIOSession session = new MockIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        
        MockContentEncoder encoder = new MockContentEncoder();
        nioEntity.exception = new IOException();
        conn.setContentEncoder(encoder);
        conn.produceOutput(serviceHandler);
        assertTrue(session.closed);
        assertTrue(nioEntity.finished);
    }

    public void testGetRequestStringEntity() throws Exception {
        MockSocket socket = new MockSocket();
        MockIOSession session = new MockIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("GET", "/get/string");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        assertEquals(stringEntity, conn.getHttpResponse().getEntity());
        
        MockContentEncoder encoder = new MockContentEncoder();
        conn.setContentEncoder(encoder);
        conn.produceOutput(serviceHandler);
        assertEquals("abc", encoder.data.toString());
        assertTrue(encoder.isCompleted());
    }

    public void testHeadRequest() throws Exception {
        MockSocket socket = new MockSocket();
        MockIOSession session = new MockIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("HEAD", "/get/string");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        assertNull(conn.getHttpResponse());
    }

    public static class MockHttpChannel extends HttpChannel {

        private StringBuilder out = new StringBuilder();

        public MockHttpChannel(HttpIOSession session,
                IOEventDispatch eventDispatch) {
            super(session, eventDispatch);
        }

        @Override
        public int write(ByteBuffer buffer) throws IOException {
            return BufferUtils.transfer(buffer, out);
        }
        
    }
    
    public static class MockIOSession extends HttpIOSession {

        boolean closed;

        public MockIOSession(AbstractNBSocket socket) {
            super(socket);
        }

        @Override
        public void close() {
            this.closed = true;
        }
        
    }
    
}
