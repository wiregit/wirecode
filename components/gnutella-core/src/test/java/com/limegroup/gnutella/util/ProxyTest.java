package com.limegroup.gnutella.util;

import junit.framework.*;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.*;

public class ProxyTester extends BaseTestCase {
    
    private final static byte V4 = (byte)4;
    private final static byte V5 = (byte)5;
    private final static String  HTTP_STR = "CONNECT";
    
    private static final int PROXY_PORT = 9990;
    private static final int DEST_PORT = 9999;

    final static int SOCKS4 = 0;
    final static int SOCKS5 = 1;
    final static int HTTP = 2;
    final static int NONE = -1;

    private static FakeProxyServer fps;

    public ProxyTester(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ProxyTester.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        RouterService rs = new RouterService(new ActivityCallbackStub());
        fps = new FakeProxyServer(9990,9999);
    }

    public void setUp() {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    }

    public static void globalTeardown() {
        fps.killServers();
    }
    

    /**
     * If Proxy is off we should connect directly
     */ 
    public void testConnectionsWithProxyOff() throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(
                                                ConnectionSettings.C_NO_PROXY);
        fps.setProxyOn(false);
        fps.setAuthentication(false);
        fps.setProxyVersion(NONE);

        Socket s = Sockets.connect("localhost",DEST_PORT,0);
        //we should be connected to something, NPE is an error
        s.close();
    }
    
    /**
     * connect with socks 5 
     */
    public void testSOCKS5Connection() throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(
                                           ConnectionSettings.C_SOCKS5_PROXY);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.PROXY_HOST.setValue("127.0.0.1");
        ConnectionSettings.PROXY_PORT.setValue(PROXY_PORT);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(SOCKS5);
        Socket s = Sockets.connect("localhost", DEST_PORT, 0);
        //Must connect somewhere, NPE is an error
        s.close();
    }

    /**
     * connect with socks 5 connection fails with bad code.
     */
    public void testSOCKS5ConnectionWithError() throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(
                                           ConnectionSettings.C_SOCKS5_PROXY);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.PROXY_HOST.setValue("127.0.0.1");
        ConnectionSettings.PROXY_PORT.setValue(PROXY_PORT);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(SOCKS5);
        fps.setMakeError(true);
        try {
            Socket s = Sockets.connect("localhost", DEST_PORT, 0);
            fail("acceptedConnection from a bad proxy server");
        } catch (IOException iox) {
            //Good -- expected behaviour
        }
        fps.setMakeError(false);
    }


    /**
     * connect with socks5 with auth
     */
    public void testSOCKS5WithAuth() throws Exception {
        ConnectionSettings.CONNECTION_METHOD.setValue(
                                           ConnectionSettings.C_SOCKS5_PROXY);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.PROXY_HOST.setValue("127.0.0.1");
        ConnectionSettings.PROXY_PORT.setValue(PROXY_PORT);
        ConnectionSettings.PROXY_AUTHENTICATE.setValue(true);
        ConnectionSettings.PROXY_USERNAME.setValue(FakeProxyServer.USER);
        ConnectionSettings.PROXY_PASS.setValue(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(SOCKS5);

        Socket s = Sockets.connect("localhost",DEST_PORT,0); //NPE is error
        s.close();
    }
        
    //connect with sock4

    //connect with HTTP

    //connect sock sock4 with auth

    //connect using httpclient with socks4

    //connect using httpclient with socks4 with auth

    //connect using httpclient with socks 5

    //connect using httpclient with socks 5 with auth
    
}
