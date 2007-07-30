package com.limegroup.gnutella.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import junit.framework.Test;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.HttpRequestExecutionHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.http.HttpIOReactor;
import org.limewire.http.SessionRequestCallbackAdapter;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ConnectionAcceptor;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.SocketsManager;

public class HttpIOReactorTest extends BaseTestCase {

    private static final int ACCEPTOR_PORT = 9999;

    private static Acceptor acceptor;

    private HttpRequestFactory requestFactory = new DefaultHttpRequestFactory();

    private BasicHttpParams params;

    public HttpIOReactorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HttpIOReactorTest.class);
    }

    public static void globalSetUp() throws Exception {
        new RouterService(new ActivityCallbackStub());

        acceptor = new Acceptor();
        acceptor.start();
        acceptor.setListeningPort(ACCEPTOR_PORT);

        // Give thread time to find and open it's sockets. This race condition
        // is tolerated in order to speed up LimeWire startup
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
    }

    public static void globealTearDown() throws Exception {
        acceptor.setListeningPort(0);
    }

    @Override
    protected void setUp() throws Exception {
        params = new BasicHttpParams();
        params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, 2222)
               .setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 1111)
               .setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)
               .setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, false)
               .setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true)
               .setParameter(HttpProtocolParams.USER_AGENT, "TEST-SERVER/1.1");

    }
    
    public void testAcceptConnection() throws Exception {
        HttpTestServer server = new HttpTestServer(params);
        server.execute(null);
        HttpIOReactor reactor = server.getReactor();
        
        Socket socket = SocketsManager.getSharedManager().connect(new InetSocketAddress("localhost", ACCEPTOR_PORT), 500);
        try {
            DefaultNHttpServerConnection conn = reactor.acceptConnection(null, socket);
            assertNotNull(conn.getContext().getAttribute(HttpIOReactor.IO_SESSION_KEY));
            assertEquals(2222, socket.getSoTimeout());
        } finally {
            socket.close();
        }
    }
    
    // disabled, see {@link HttpIOReactor#connect}
    public void disabledTestGetFromAcceptor() throws Exception {
        final HttpTestServer server = new HttpTestServer(params);
        server.registerHandler("*", new HttpRequestHandler() {
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setEntity(new ByteArrayEntity("foobar".getBytes()));
            }
        });
        server.execute(new MyEventListener());
        RouterService.getConnectionDispatcher().addConnectionAcceptor(
                new ConnectionAcceptor() {
                    public void acceptConnection(String word, Socket socket) {
                        server.getReactor().acceptConnection(word + " ", socket);
                    }
                }, false, false, "GET", "HEAD", "POST" );

        final HttpTestClient client = new HttpTestClient();
        MyHttpRequestExecutionHandler executionHandler = new MyHttpRequestExecutionHandler();
        client.execute(executionHandler);

        client.connect(new InetSocketAddress("localhost", ACCEPTOR_PORT), null,
                new SessionRequestCallbackAdapter());

        synchronized (HttpIOReactorTest.this) {
            HttpIOReactorTest.this.wait(1000);
        }
        assertNotNull(executionHandler.response);
        assertEquals(HttpVersion.HTTP_1_1, executionHandler.response
                .getStatusLine().getHttpVersion());
        assertEquals(HttpStatus.SC_OK, executionHandler.response
                .getStatusLine().getStatusCode());
        assertEquals("foobar", executionHandler.responseContent);
    }

    private class MyHttpRequestExecutionHandler implements
            HttpRequestExecutionHandler {

        HttpRequest request;

        HttpResponse response;

        String responseContent;

        public void handleResponse(HttpResponse response, HttpContext context) {
            this.response = response;
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                this.responseContent = out.toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            synchronized (HttpIOReactorTest.this) {
                HttpIOReactorTest.this.notify();
            }
        }

        public void initalizeContext(HttpContext context, Object attachment) {
        }

        public HttpRequest submitRequest(HttpContext context) {
            if (this.request != null) {
                // request has been sent already;
                return null;
            }

            try {
                this.request = requestFactory.newHttpRequest("GET", "/");
            } catch (MethodNotSupportedException e) {
                throw new RuntimeException(e);
            }
            return request;
        }

    }

    private class MyEventListener implements  EventListener {
        
        public void connectionClosed(NHttpConnection conn) {
        }

        public void connectionOpen(NHttpConnection conn) {
        }

        public void connectionTimeout(NHttpConnection conn) {
        }

        public void fatalIOException(IOException ex, NHttpConnection conn) {
            throw new RuntimeException(ex);
        }

        public void fatalProtocolException(HttpException ex,
                NHttpConnection conn) {
            throw new RuntimeException(ex);
        }
        
    }
}
