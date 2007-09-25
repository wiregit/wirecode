package com.limegroup.gnutella.http;

import java.io.IOException;

import junit.framework.Test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.limewire.nio.NIODispatcher;

import com.limegroup.gnutella.bootstrap.TestBootstrapServer;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.SocketsManager;

/**
 * Tests various things of HttpClient / HttpClientManager.
 */
public class HttpClientManagerTest extends LimeTestCase {
    
    private TestBootstrapServer[] httpServers;
    private TestBootstrapServer[] tlsServers;
    private String[] httpUrls;
    private String[] tlsUrls;
    private String[] httpsUrls;
    
    private static final int HTTP_PORT=6700;
    private static final int TLS_PORT=7700;

    public HttpClientManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(HttpClientManagerTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        
         HttpClientManager.socketsManager = new SocketsManager();
        
        httpServers = new TestBootstrapServer[11];
        tlsServers = new TestBootstrapServer[11];
        httpUrls = new String[11];
        tlsUrls = new String[11];
        httpsUrls = new String[11];
        
        for(int i = 0; i < 11; i++) {
            httpServers[i] = new TestBootstrapServer(HTTP_PORT+i);
            tlsServers[i] = new TestBootstrapServer(TLS_PORT+i, true);
            httpUrls[i] = "http://127.0.0.1:" + (HTTP_PORT + i);
            tlsUrls[i] = "tls://127.0.0.1:" + (TLS_PORT + i);
            httpsUrls[i] = "https://127.0.0.1:" + (TLS_PORT + i);
        }
    }
    
    public void tearDown() throws Exception {
        for(int i = 0; i < 11; i++) {
            httpServers[i].shutdown();
            tlsServers[i].shutdown();
        }    
        Thread.sleep(100);
    }
    
    public void testTLStoNormalFails() throws Exception {
        // make sure execution fails if the server doesn't respond
        HttpClient client = HttpClientManager.getNewClient();
        GetMethod get = new GetMethod("tls://127.0.0.1:" + HTTP_PORT);
        try {
            client.executeMethod(get);
            fail("should have thrown exception");
        } catch(IOException expected) {}
    }
    
    public void testExecuteMethodRedirectingHTTP() throws Exception {
        doExecuteMethodRedirectingTest(httpUrls, httpServers);
    }
    
    public void testExecuteMethodRedirectingTLS() throws Exception {
        doExecuteMethodRedirectingTest(tlsUrls, tlsServers);
    }
    
    public void testExecuteMethodRedirectingHTTPS() throws Exception {
        doExecuteMethodRedirectingTest(httpsUrls, tlsServers);
    }
    
    private void doExecuteMethodRedirectingTest(String[] urls, TestBootstrapServer[] servers) throws Exception {
        HttpClient client = HttpClientManager.getNewClient();
        HttpMethod get = new GetMethod(urls[0]);
        
        servers[0].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[1]);
        servers[1].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[2]);
        
        servers[2].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[3]);
        servers[3].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[4]);
        servers[4].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[5]);
        servers[5].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[6]);
        servers[6].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[7]);
        servers[7].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[8]);
        servers[8].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[9]);
        servers[9].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[10]);
        
        try {
            HttpClientManager.executeMethodRedirecting(client, get, 2);
            fail("Should have thrown redirect failure");
        } catch(HttpException he) {
            assertNotNull(servers[0].getRequest());
            assertNotNull(servers[1].getRequest());
            assertNull(servers[2].getRequest()); // should have stopped after 1 & 2 tried.
            // expected.
        }
        
        HttpClientManager.executeMethodRedirecting(client, get, 9);
        assertNotNull(servers[2].getRequest());
        assertNotNull(servers[3].getRequest());
        assertNotNull(servers[4].getRequest());
        assertNotNull(servers[5].getRequest());
        assertNotNull(servers[6].getRequest());
        assertNotNull(servers[7].getRequest());
        assertNotNull(servers[8].getRequest());
        assertNotNull(servers[9].getRequest());
        assertNotNull(servers[10].getRequest());
    }
    
    public void testExecuteMethodRedirectingNoNIO() throws Exception {
        doExecuteMethodRedirectingNoNIOTest(httpUrls, httpServers);
    }
    
    private void doExecuteMethodRedirectingNoNIOTest(String[] urls, TestBootstrapServer[] servers) throws Exception {
        HttpClient client = HttpClientManager.getNewClient();
        HttpMethod get = new GetMethod(urls[0]);
        
        servers[0].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[1]);
        servers[1].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[2]);
        
        servers[2].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[3]);
        servers[3].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[4]);
        servers[4].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[5]);
        servers[5].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[6]);
        servers[6].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[7]);
        servers[7].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[8]);
        servers[8].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[9]);
        servers[9].setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+urls[10]);

        final Object nioLock = new Object();
        try {
            try {
                NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
                    public void run() {
                        synchronized(nioLock) {
                            try {
                                nioLock.wait();
                            } catch(InterruptedException ignored) {}
                        }
                    }
                });
                Thread.sleep(100);
                HttpClientManager.executeMethodRedirectingNoNIO(client, get, 2);
                fail("Should have thrown redirect failure");
            } finally {
                synchronized(nioLock) {
                    nioLock.notify();
                }
            }
        } catch(HttpException he) {
            assertNotNull(servers[0].getRequest());
            assertNotNull(servers[1].getRequest());
            assertNull(servers[2].getRequest()); // should have stopped after 1 & 2 tried.
            // expected.
        }
        
        try {
            NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
                public void run() {
                    synchronized(nioLock) {
                        try {
                            nioLock.wait();
                        } catch(InterruptedException ignored) {}
                    }
                }
            });
            Thread.sleep(100);
            HttpClientManager.executeMethodRedirectingNoNIO(client, get, 9);
        } finally {
            synchronized (nioLock) {
                nioLock.notify();
            }
        }
        assertNotNull(servers[2].getRequest());
        assertNotNull(servers[3].getRequest());
        assertNotNull(servers[4].getRequest());
        assertNotNull(servers[5].getRequest());
        assertNotNull(servers[6].getRequest());
        assertNotNull(servers[7].getRequest());
        assertNotNull(servers[8].getRequest());
        assertNotNull(servers[9].getRequest());
        assertNotNull(servers[10].getRequest());
    }
    
    public void testVariousHttpClientThings() throws Exception {        
        // Make sure a bad protocol throws an unexpected exception.
        try {
            new GetMethod("bad://127.0.0.1:80/file");
            fail("expected exception");
        } catch(IllegalStateException ise) {
            // expected.
        }            
        
        doUppercaseTest(httpUrls[0], httpServers[0]);
        doUppercaseTest(tlsUrls[0], tlsServers[0]);
        doUppercaseTest(httpsUrls[1], tlsServers[1]); // use a different server
        
        // Make sure we know what a malformed URL will give us.
        try {
            new GetMethod("http:asdofih");
            fail("expected exception");
        } catch(IllegalArgumentException iae) {
            // expected.
        }
    }
    
    private void doUppercaseTest(String url, TestBootstrapServer server) throws Exception {
        // Make sure we can deal with strange info such as uppercase
        // HTTP stuff.
        GetMethod get = new GetMethod(url.toUpperCase());
        HttpClient client = HttpClientManager.getNewClient();
        client.executeMethod(get);
        assertNotNull(server.getRequest());
    }
    
    /**
     * Tests that HttpClient correctly reuses an open connection
     * without explicitly telling it to.
     */
    public void testReuseConnection() throws Exception {
        doReuseConnectionTest(httpUrls[0], httpServers[0]);
        doReuseConnectionTest(tlsUrls[0], tlsServers[0]);
        doReuseConnectionTest(httpsUrls[1], tlsServers[1]); // use a different server
    }
    
    private void doReuseConnectionTest(String url, TestBootstrapServer server) throws Exception {
        String responseData = "this is response data";
        int length = responseData.length();
        server.setResponseData(responseData);
        server.setResponse("HTTP/1.1 200 OK\r\nContent-Length: " + length);
        server.setAllowConnectionReuse(true);
        HttpMethod get;
        HttpClient client;
        
        get = new GetMethod(url);
        client = HttpClientManager.getNewClient();
        try {
            client.executeMethod(get);
        } finally {
            get.releaseConnection();
        }
        
        get = new GetMethod(url);
        client = HttpClientManager.getNewClient();
        try {
            client.executeMethod(get);
        } finally {
            get.releaseConnection();
        }
        
        assertEquals("wrong connection attempts", 1, server.getConnectionAttempts());
        assertEquals("wrong request attempts", 2, server.getRequestAttempts());
    }
    
    /**
     * Tests that a connection correctly times out after a period idleness.
     */
    public void testConnectionClosesHTTP() throws Exception {
        doConnectionClosesTest(httpUrls[0], httpServers[0]);
    }
    
    public void testConnectionClosesTLS() throws Exception {
        doConnectionClosesTest(tlsUrls[0], tlsServers[0]);
    }
    
    public void testConnectionClosesHTTPS() throws Exception {
        doConnectionClosesTest(httpsUrls[0], tlsServers[0]);
    }
    
    private void doConnectionClosesTest(String url, TestBootstrapServer server) throws Exception {
        String responseData = "this is response data";
        int length = responseData.length();
        server.setResponseData(responseData);
        server.setResponse("HTTP/1.1 200 OK\r\nContent-Length: " + length);
        server.setAllowConnectionReuse(true);
        HttpMethod get;
        HttpClient client;
        
        get = new GetMethod(url);
        client = HttpClientManager.getNewClient();
        try {
            client.executeMethod(get);
        } finally {
            get.releaseConnection();
        }
        
        Thread.sleep(1000 * 70);
        
        get = new GetMethod(url);
        client = HttpClientManager.getNewClient();
        try {
            client.executeMethod(get);
        } finally {
            get.releaseConnection();
        }
        
        assertEquals("wrong connection attempts", 2, server.getConnectionAttempts());
        assertEquals("wrong request attempts", 2, server.getRequestAttempts());
    }        
}        
