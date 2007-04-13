package com.limegroup.gnutella.http;

import junit.framework.Test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.limewire.nio.NIODispatcher;

import com.limegroup.gnutella.bootstrap.TestBootstrapServer;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests various things of HttpClient / HttpClientManager.
 */
public class HttpClientManagerTest extends LimeTestCase {
    
    private TestBootstrapServer s1;
    private TestBootstrapServer s2;
    private TestBootstrapServer s3;
    private TestBootstrapServer s4;
    private TestBootstrapServer s5;
    private TestBootstrapServer s6;
    private TestBootstrapServer s7;
    private TestBootstrapServer s8;
    private TestBootstrapServer s9;
    private TestBootstrapServer s10;
    private TestBootstrapServer s11;
    
    private final static int PORT=6700;
    
    private final String url1 = "http://127.0.0.1:" + PORT;
    private final String url2 = "http://127.0.0.1:" + (PORT + 1);
    private final String url3 = "http://127.0.0.1:" + (PORT + 2);
    private final String url4 = "http://127.0.0.1:" + (PORT + 3);
    private final String url5 = "http://127.0.0.1:" + (PORT + 4);
    private final String url6 = "http://127.0.0.1:" + (PORT + 5);
    private final String url7 = "http://127.0.0.1:" + (PORT + 6);
    private final String url8 = "http://127.0.0.1:" + (PORT + 7);
    private final String url9 = "http://127.0.0.1:" + (PORT + 8);
    private final String url10 = "http://127.0.0.1:" + (PORT + 9);
    private final String url11 = "http://127.0.0.1:" + (PORT + 10);

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
        //Prepare servers.
        s1=new TestBootstrapServer(PORT);
        s2=new TestBootstrapServer(PORT+1);
        s3=new TestBootstrapServer(PORT+2);
        s4=new TestBootstrapServer(PORT+3);
        s5=new TestBootstrapServer(PORT+4);
        s6=new TestBootstrapServer(PORT+5);
        s7=new TestBootstrapServer(PORT+6);
        s8=new TestBootstrapServer(PORT+7);
        s9=new TestBootstrapServer(PORT+8);
        s10=new TestBootstrapServer(PORT+9);
        s11=new TestBootstrapServer(PORT+10);
    }
    
    public void tearDown() throws Exception {
        s1.shutdown();
        s2.shutdown();
        s3.shutdown();
        s4.shutdown();
        s5.shutdown();
        s6.shutdown();
        s7.shutdown();
        s8.shutdown();
        s9.shutdown();
        s10.shutdown();
        s11.shutdown();
        
        Thread.sleep(100);
    }
    
    public void testExecuteMethodRedirecting() throws Exception {
        HttpClient client = HttpClientManager.getNewClient();
        HttpMethod get = new GetMethod(url1);
        
        s1.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url2);
        s2.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url3);
        
        s3.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url4);
        s4.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url5);
        s5.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url6);
        s6.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url7);
        s7.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url8);
        s8.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url9);
        s9.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url10);
        s10.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url11);
        
        try {
            HttpClientManager.executeMethodRedirecting(client, get, 2);
            fail("Should have thrown redirect failure");
        } catch(HttpException he) {
            assertNotNull(s1.getRequest());
            assertNotNull(s2.getRequest());
            assertNull(s3.getRequest()); // should have stopped after 1 & 2 tried.
            // expected.
        }
        
        HttpClientManager.executeMethodRedirecting(client, get, 9);
        assertNotNull(s3.getRequest());
        assertNotNull(s4.getRequest());
        assertNotNull(s5.getRequest());
        assertNotNull(s6.getRequest());
        assertNotNull(s7.getRequest());
        assertNotNull(s8.getRequest());
        assertNotNull(s9.getRequest());
        assertNotNull(s10.getRequest());
        assertNotNull(s11.getRequest());
    }
    
    public void testExecuteMethodRedirectingNoNIO() throws Exception {
        HttpClient client = HttpClientManager.getNewClient();
        HttpMethod get = new GetMethod(url1);
        
        s1.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url2);
        s2.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url3);
        
        s3.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url4);
        s4.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url5);
        s5.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url6);
        s6.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url7);
        s7.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url8);
        s8.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url9);
        s9.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url10);
        s10.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url11);

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
            assertNotNull(s1.getRequest());
            assertNotNull(s2.getRequest());
            assertNull(s3.getRequest()); // should have stopped after 1 & 2 tried.
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
        assertNotNull(s3.getRequest());
        assertNotNull(s4.getRequest());
        assertNotNull(s5.getRequest());
        assertNotNull(s6.getRequest());
        assertNotNull(s7.getRequest());
        assertNotNull(s8.getRequest());
        assertNotNull(s9.getRequest());
        assertNotNull(s10.getRequest());
        assertNotNull(s11.getRequest());
    }
    
    public void testVariousHttpClientThings() throws Exception {
        HttpMethod get;
        HttpClient client;
        
        // Make sure a bad protocol throws an unexpected exception.
        try {
            get = new GetMethod("bad://127.0.0.1:80/file");
            fail("expected exception");
        } catch(IllegalStateException ise) {
            // expected.
        }            
        
        // Make sure we can deal with strange info such as uppercase
        // HTTP stuff.
        get = new GetMethod(url1.toUpperCase());
        client = HttpClientManager.getNewClient();
        client.executeMethod(get);
        
        // Make sure we know what a malformed URL will give us.
        try {
            get = new GetMethod("http:asdofih");
            fail("expected exception");
        } catch(IllegalArgumentException iae) {
            // expected.
        }
    }
    
    /**
     * Tests that HttpClient correctly reuses an open connection
     * without explicitly telling it to.
     */
    public void testReuseConnection() throws Exception {
        String responseData = "this is response data";
        int length = responseData.length();
        s1.setResponseData(responseData);
        s1.setResponse("HTTP/1.1 200 OK\r\nContent-Length: " + length);
        s1.setAllowConnectionReuse(true);
        HttpMethod get;
        HttpClient client;
        
        get = new GetMethod(url1);
        client = HttpClientManager.getNewClient();
        try {
            client.executeMethod(get);
        } finally {
            get.releaseConnection();
        }
        
        get = new GetMethod(url1);
        client = HttpClientManager.getNewClient();
        try {
            client.executeMethod(get);
        } finally {
            get.releaseConnection();
        }
        
        assertEquals("wrong connection attempts", 1, s1.getConnectionAttempts());
        assertEquals("wrong request attempts", 2, s1.getRequestAttempts());
    }
    
    /**
     * Tests that a connection correctly times out after a period idleness.
     */
    public void testConnectionCloses() throws Exception {
        String responseData = "this is response data";
        int length = responseData.length();
        s1.setResponseData(responseData);
        s1.setResponse("HTTP/1.1 200 OK\r\nContent-Length: " + length);
        s1.setAllowConnectionReuse(true);
        HttpMethod get;
        HttpClient client;
        
        get = new GetMethod(url1);
        client = HttpClientManager.getNewClient();
        try {
            client.executeMethod(get);
        } finally {
            get.releaseConnection();
        }
        
        Thread.sleep(1000 * 70);
        
        get = new GetMethod(url1);
        client = HttpClientManager.getNewClient();
        try {
            client.executeMethod(get);
        } finally {
            get.releaseConnection();
        }
        
        assertEquals("wrong connection attempts", 2, s1.getConnectionAttempts());
        assertEquals("wrong request attempts", 2, s1.getRequestAttempts());
    }        
}        
