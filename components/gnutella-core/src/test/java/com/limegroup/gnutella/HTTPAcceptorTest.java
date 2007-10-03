package com.limegroup.gnutella;

import java.io.IOException;

import junit.framework.Test;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.cybergarage.http.HTTPStatus;
import org.limewire.http.HttpAcceptorListener;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;
import org.limewire.net.SocketAcceptor;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;

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
        // make sure local connections are accepted
        LocalSocketAddressService.setSocketAddressProvider(new LocalSocketAddressProviderStub());

        ConnectionDispatcher connectionDispatcher = new ConnectionDispatcherImpl();
        socketAcceptor = new SocketAcceptor(connectionDispatcher);
        httpAcceptor = new HTTPAcceptor();
        connectionDispatcher.addConnectionAcceptor(httpAcceptor, false, httpAcceptor.getHttpMethods());
        
        socketAcceptor.bind(PORT);
        
        client = new HttpClient();
        HostConfiguration config = new HostConfiguration();
        config.setHost("localhost", PORT);
        client.setHostConfiguration(config);
    }

    @Override
    protected void tearDown() throws Exception {
        httpAcceptor.stop();
        socketAcceptor.unbind();
    }

    public void testGetRequest() throws Exception {
        httpAcceptor.start();

        GetMethod method = new GetMethod("/");
        try {
            int response = client.executeMethod(method);
            assertEquals(HTTPStatus.BAD_REQUEST, response);
        } finally {
            method.releaseConnection();
        }

        method = new GetMethod("/update.xml");
        try {
            int response = client.executeMethod(method);
            assertEquals(HTTPStatus.BAD_REQUEST, response);
        } finally {
            method.releaseConnection();
        }
    }

    public void testAddRemoveAcceptorListener() throws Exception {
        MyHTTPAcceptorListener listener = new MyHTTPAcceptorListener();
        httpAcceptor.addAcceptorListener(listener);
        httpAcceptor.start();
        assertFalse(listener.opened);
        assertFalse(listener.closed);
        
        GetMethod method = new GetMethod("/");
        try{ 
            int response = client.executeMethod(method);
            assertEquals(HTTPStatus.BAD_REQUEST, response);
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
            method.releaseConnection();
        }
        
        listener.opened = false;
        listener.closed = false;
        listener.request = null;
        listener.response = null;
        
        httpAcceptor.removeAcceptorListener(listener);
        method = new GetMethod("/");
        try{ 
            client.executeMethod(method);
            assertFalse(listener.opened);
            assertFalse(listener.closed);
            assertNull(listener.request);
            assertNull(listener.response);
        } finally {
            method.releaseConnection();
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
        GetMethod method = new GetMethod("/");
        try{ 
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_ACCEPTED, response);
        } finally {
            method.releaseConnection();
        }

        httpAcceptor.unregisterHandler("/");
        method = new GetMethod("/");
        try{ 
            int response = client.executeMethod(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response);
        } finally {
            method.releaseConnection();
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
