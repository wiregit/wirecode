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
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class HTTPAcceptorTest extends BaseTestCase {

    private static final int PORT = 6668;

    private static Acceptor acceptor;

    private HTTPAcceptor httpAcceptor;

    private HttpClient client;

    public HTTPAcceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPAcceptorTest.class);
    }

    public static void globalSetUp() throws Exception {
        LimeTestUtils.setActivityCallBack(new ActivityCallbackStub());

        doSettings();

        // TODO acceptor shutdown in globalTearDown()
        acceptor = RouterService.getAcceptor();
        acceptor.init();
        acceptor.start();
    }

    private static void doSettings() {
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
    }

    @Override
    protected void setUp() throws Exception {
        if (acceptor == null) {
            globalSetUp();
        }
        
        doSettings();

        client = new HttpClient();
        HostConfiguration config = new HostConfiguration();
        config.setHost("localhost", PORT);
        client.setHostConfiguration(config);
        httpAcceptor = new HTTPAcceptor();
    }

    @Override
    protected void tearDown() throws Exception {
        httpAcceptor.stop(RouterService.getConnectionDispatcher());
    }

    public void testGetRequest() throws Exception {
        httpAcceptor.start(RouterService.getConnectionDispatcher());

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
        httpAcceptor.start(RouterService.getConnectionDispatcher());
        assertFalse(listener.opened);
        assertFalse(listener.closed);
        
        GetMethod method = new GetMethod("/");
        try{ 
            client.executeMethod(method);
            assertTrue(listener.opened);
            assertFalse(listener.closed);
            assertNotNull(listener.request);
            assertEquals("GET", listener.request.getRequestLine().getMethod());
            assertEquals("/", listener.request.getRequestLine().getUri());
            assertEquals(HttpVersion.HTTP_1_1, listener.request.getRequestLine().getHttpVersion());
            LimeTestUtils.waitForNIO();
            assertNotNull(listener.response);
        } finally {
            method.releaseConnection();
        }
        
        LimeTestUtils.waitForNIO();        
        assertTrue(listener.closed);
        
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
        httpAcceptor.start(RouterService.getConnectionDispatcher());

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

    private class MyHTTPAcceptorListener implements HTTPAcceptorListener {

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
