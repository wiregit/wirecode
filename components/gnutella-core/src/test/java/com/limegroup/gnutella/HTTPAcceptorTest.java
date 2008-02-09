package com.limegroup.gnutella;

import java.io.IOException;

import junit.framework.Test;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.cybergarage.http.HTTPStatus;
import org.limewire.http.HttpAcceptorListener;
import org.limewire.http.HttpClientUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;
import org.limewire.net.SocketAcceptor;
import org.limewire.util.BaseTestCase;

//ITEST
public class HTTPAcceptorTest extends BaseTestCase {

    private static final int PORT = 6668;

    private HTTPAcceptor httpAcceptor;

    private HttpClient client;

    private SocketAcceptor socketAcceptor;

    public HTTPAcceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPAcceptorTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        ConnectionDispatcher connectionDispatcher = new ConnectionDispatcherImpl(new SimpleNetworkInstanceUtils(false));
        socketAcceptor = new SocketAcceptor(connectionDispatcher);
        httpAcceptor = new HTTPAcceptor(null);
        connectionDispatcher.addConnectionAcceptor(httpAcceptor, false, httpAcceptor.getHttpMethods());
        
        socketAcceptor.bind(PORT);

        client = new DefaultHttpClient();
    }

    @Override
    protected void tearDown() throws Exception {
        httpAcceptor.stop();
        socketAcceptor.unbind();
    }

    public void testGetRequest() throws Exception {
        httpAcceptor.start();

        HttpGet method = new HttpGet("http://localhost:" + PORT + "/");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HTTPStatus.BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpGet("http://localhost:" + PORT + "/update.xml");
        try {
            response = client.execute(method);
            assertEquals(HTTPStatus.BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testAddRemoveAcceptorListener() throws Exception {
        MyHTTPAcceptorListener listener = new MyHTTPAcceptorListener();
        httpAcceptor.addAcceptorListener(listener);
        httpAcceptor.start();
        assertFalse(listener.opened);
        assertFalse(listener.closed);


        HttpGet method = new HttpGet("http://localhost:" + PORT + "/");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HTTPStatus.BAD_REQUEST, response.getStatusLine().getStatusCode());
            LimeTestUtils.waitForNIO();
            assertTrue(listener.opened);
            // bad request, so connection should have been closed
            assertTrue(listener.closed);
            assertNotNull(listener.request);
            assertEquals("GET", listener.request.getRequestLine().getMethod());
            assertEquals("/", listener.request.getRequestLine().getUri());
            assertEquals(HttpVersion.HTTP_1_1, listener.request.getRequestLine().getProtocolVersion());
            LimeTestUtils.waitForNIO();
            LimeTestUtils.waitForNIO();
            assertNotNull(listener.response);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        listener.opened = false;
        listener.closed = false;
        listener.request = null;
        listener.response = null;

        httpAcceptor.removeAcceptorListener(listener);
        method = new HttpGet("http://localhost:" + PORT + "/");
        try {
            client.execute(method);
            assertFalse(listener.opened);
            assertFalse(listener.closed);
            assertNull(listener.request);
            assertNull(listener.response);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testRegisterUnregisterHandler() throws Exception {
        HttpRequestHandler handler = new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                               HttpContext context) throws org.apache.http.HttpException,
                    IOException {
                response.setStatusCode(HttpStatus.SC_ACCEPTED);
            }

        };
        httpAcceptor.start();

        httpAcceptor.registerHandler("/", handler);
        HttpGet method = new HttpGet("http://localhost:" + PORT + "/");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        httpAcceptor.unregisterHandler("/");
        method = new HttpGet("http://localhost:" + PORT + "/");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    private class MyHTTPAcceptorListener implements HttpAcceptorListener {

        boolean closed;

        boolean opened;

        NHttpConnection conn;

        HttpResponse response;

        HttpRequest request;

        public void connectionClosed(NHttpConnection conn) {
            this.conn = conn;
            this.closed = true;
        }

        public void connectionOpen(NHttpConnection conn) {
            this.conn = conn;
            this.opened = true;
        }

        public void requestReceived(NHttpConnection conn, HttpRequest request) {
            this.conn = conn;
            this.request = request;
        }

        public void responseSent(NHttpConnection conn, HttpResponse response) {
            this.conn = conn;
            this.response = response;
        }

    }

}
