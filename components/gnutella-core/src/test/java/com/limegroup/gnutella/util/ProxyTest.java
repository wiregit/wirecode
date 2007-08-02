package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import junit.framework.Test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.StubConnectObserver;

public class ProxyTest extends LimeTestCase {

    private static final int PROXY_PORT = 9990;

    static final int DEST_PORT = 9999;

    final static int SOCKS4 = 0;

    final static int SOCKS5 = 1;

    final static int HTTP = 2;

    final static int NONE = -1;

    private static FakeProxyServer fps;

    public ProxyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ProxyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        new RouterService(new ActivityCallbackStub());
    }

    @Override
    public void setUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.PROXY_HOST.setValue("127.0.0.1");
        ConnectionSettings.PROXY_PORT.setValue(PROXY_PORT);
        
        fps = new FakeProxyServer(9990, 9999);
        fps.setMakeError(false);
    }

    @Override
    public void tearDown() throws Exception {
        fps.killServers();
    }

    /**
     * If Proxy is off we should connect directly
     */
    public void testConnectionsWithProxyOff() throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_NO_PROXY);
        fps.setProxyOn(false);
        fps.setAuthentication(false);
        fps.setProxyVersion(NONE);

        Socket s = ProviderHacks.getSocketsManager().connect(new InetSocketAddress("localhost", DEST_PORT), 0);
        // we should be connected to something, NPE is an error
        s.close();
    }

    /**
     * connect with socks 5
     */
    public void testSOCKS5Connection() throws Exception {
        runSOCKS5Connection(false);
    }

    public void testSOCKS5ConnectionNB() throws Exception {
        runSOCKS5Connection(true);
    }

    private void runSOCKS5Connection(boolean nb) throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS5_PROXY);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(SOCKS5);
        connect(nb, true);
    }

    /**
     * connect with socks 5 connection fails with bad code.
     */
    public void testSOCKS5ConnectionWithError() throws Exception {
        runSOCKS5ConnectionWithError(false);
    }

    public void testSOCKS5ConnectionWithErrorNB() throws Exception {
        runSOCKS5ConnectionWithError(true);
    }

    private void runSOCKS5ConnectionWithError(boolean nb) throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS5_PROXY);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(SOCKS5);
        fps.setMakeError(true);
        connect(nb, false);
        fps.setMakeError(false);
    }

    /**
     * connect with socks5 with auth
     */
    public void testSOCKS5WithAuth() throws Exception {
        runSOCKS5WithAuth(false);
    }

    public void testSOCKS5WithAuthNB() throws Exception {
        runSOCKS5WithAuth(true);
    }

    private void runSOCKS5WithAuth(boolean nb) throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS5_PROXY);
        ConnectionSettings.PROXY_AUTHENTICATE.setValue(true);
        ConnectionSettings.PROXY_USERNAME.setValue(FakeProxyServer.USER);
        ConnectionSettings.PROXY_PASS.setValue(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(SOCKS5);
        connect(nb, true);
    }

    public void testSOCKS4Connection() throws Exception {
        runSOCKS4Connection(false);
    }

    public void testSOCKS4ConnectionNB() throws Exception {
        runSOCKS4Connection(true);
    }

    private void runSOCKS4Connection(boolean nb) throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS4_PROXY);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(SOCKS4);
        connect(nb, true);
    }

    public void testSOCKS4ConnectionWithError() throws Exception {
        runSOCKS4ConnectionWithError(false);
    }

    public void testSOCKS4ConnectionWithErrorNB() throws Exception {
        runSOCKS4ConnectionWithError(true);
    }

    private void runSOCKS4ConnectionWithError(boolean nb) throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS4_PROXY);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(SOCKS4);
        fps.setMakeError(true);
        connect(nb, false);
        fps.setMakeError(false);
    }

    public void testSOCKS4WithAuth() throws Exception {
        runSOCKS4WithAuth(false);
    }

    public void testSOCKS4WithAuthNB() throws Exception {
        runSOCKS4WithAuth(true);
    }

    private void runSOCKS4WithAuth(boolean nb) throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS4_PROXY);
        ConnectionSettings.PROXY_AUTHENTICATE.setValue(true);
        ConnectionSettings.PROXY_USERNAME.setValue(FakeProxyServer.USER);
        ConnectionSettings.PROXY_PASS.setValue(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(SOCKS4);
        connect(nb, true);
    }

    /**
     * Tests that HTTP proxies are used correctly
     */
    public void testHTTPProxy() throws Exception {
        runHTTPProxy(false);
    }

    public void testHTTPProxyNB() throws Exception {
        runHTTPProxy(true);
    }

    private void runHTTPProxy(boolean nb) throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_HTTP_PROXY);

        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(HTTP);
        connect(nb, true);
    }

    public void testHTTPProxyWithError() throws Exception {
        runHTTPProxyWithError(false);
    }

    public void testHTTPProxyWithErrorNB() throws Exception {
        runHTTPProxyWithError(true);
    }

    private void runHTTPProxyWithError(boolean nb) throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_HTTP_PROXY);

        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(HTTP);
        fps.setMakeError(true);
        connect(nb, false);
    }

    private void connect(boolean nb, boolean success) throws Exception {
        if (success) {
            Socket s;
            if (!nb) {
                s = ProviderHacks.getSocketsManager().connect(new InetSocketAddress("localhost", DEST_PORT), 0);
            } else {
                StubConnectObserver o = new StubConnectObserver();
                s = ProviderHacks.getSocketsManager().connect(new InetSocketAddress("localhost", DEST_PORT), 0, o);
                o.waitForResponse(5000);
                assertEquals(s, o.getSocket());
                assertNull(o.getIoException());
                assertFalse(o.isShutdown());
            }
            // Must connect somewhere, NPE is an error
            s.close();
        } else {
            if (!nb) {
                try {
                    ProviderHacks.getSocketsManager().connect(new InetSocketAddress("localhost", DEST_PORT), 0);
                    fail("acceptedConnection from a bad proxy server");
                } catch (IOException iox) {
                    // Good -- expected behaviour
                }
            } else {
                StubConnectObserver o = new StubConnectObserver();
                ProviderHacks.getSocketsManager().connect(new InetSocketAddress("localhost", DEST_PORT), 0, o);
                o.waitForResponse(5000);
                assertNull(o.getSocket());
                assertNull(o.getIoException());
                assertTrue(o.isShutdown());
            }
        }
    }
    
    public void testHTTPClientWithProxyOff() throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_NO_PROXY);
        fps.setProxyOn(false);
        fps.setAuthentication(false);
        fps.setProxyVersion(NONE);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile.txt";
        HttpClient client = HttpClientManager.getNewClient();
        HttpMethod get = new GetMethod(connectTo);
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
        client.executeMethod(get);
        byte[] resp = get.getResponseBody();
        assertEquals("invalid response from server", "hello", new String(resp));
        get.abort();
    }

    public void testHTTPClientProxiesWithHttpProxy() throws Exception {
        // ConnectionSettings.PROXY_SIMPLE_HTTP_CONNECTIONS.setValue(true);
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_HTTP_PROXY);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(HTTP);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile2.txt";
        HttpMethod get = new GetMethod(connectTo);
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
        HttpClient client = HttpClientManager.getNewClient();
        // release the connections.
        client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        client.executeMethod(get);
        String resp = new String(get.getResponseBody());
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

    public void testHTTPClientProxiesWithSocks4() throws Exception {
        // ConnectionSettings.PROXY_SIMPLE_HTTP_CONNECTIONS.setValue(true);
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS4_PROXY);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(SOCKS4);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile3.txt";
        HttpMethod get = new GetMethod(connectTo);
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
        HttpClient client = HttpClientManager.getNewClient();
        // release the connections.
        client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        client.executeMethod(get);
        String resp = new String(get.getResponseBody());
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

    public void testHTTPClientProxiesWithSocks5() throws Exception {
        // ConnectionSettings.PROXY_SIMPLE_HTTP_CONNECTIONS.setValue(true);
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS5_PROXY);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(SOCKS5);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile4.txt";
        HttpMethod get = new GetMethod(connectTo);
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
        HttpClient client = HttpClientManager.getNewClient();
        // release the connections.
        client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        client.executeMethod(get);
        String resp = new String(get.getResponseBody());
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

    public void testHTTPClientProxiesWithAuthSocks4() throws Exception {
        // ConnectionSettings.PROXY_SIMPLE_HTTP_CONNECTIONS.setValue(true);
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS4_PROXY);
        ConnectionSettings.PROXY_AUTHENTICATE.setValue(true);
        ConnectionSettings.PROXY_USERNAME.setValue(FakeProxyServer.USER);
        ConnectionSettings.PROXY_PASS.setValue(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(SOCKS4);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile3.txt";
        HttpMethod get = new GetMethod(connectTo);
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
        HttpClient client = HttpClientManager.getNewClient();
        // release the connections.
        client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        client.executeMethod(get);
        String resp = new String(get.getResponseBody());
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

    public void testHTTPClientProxiesWithAuthSocks5() throws Exception {
        // ConnectionSettings.PROXY_SIMPLE_HTTP_CONNECTIONS.setValue(true);
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS5_PROXY);
        ConnectionSettings.PROXY_AUTHENTICATE.setValue(true);
        ConnectionSettings.PROXY_USERNAME.setValue(FakeProxyServer.USER);
        ConnectionSettings.PROXY_PASS.setValue(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(SOCKS5);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile4.txt";
        HttpMethod get = new GetMethod(connectTo);
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");
        HttpClient client = HttpClientManager.getNewClient();
        // release the connections.
        client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        client.executeMethod(get);
        String resp = new String(get.getResponseBody());
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

}
