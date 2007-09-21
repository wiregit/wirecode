package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.Test;

import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.NIOSocketFactory;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ConnectObserverStub;

public class LimitedSocketControllerTest extends LimeTestCase {
    
    private static final int TIMEOUT = 30000;
    private static final NBSocketFactory FACTORY = new NIOSocketFactory();
    
    private int BAD_PORT = 9998;
    private InetSocketAddress BAD_ADDR = new InetSocketAddress("127.0.0.1", BAD_PORT);
    private int LISTEN_PORT = 9999;
    private InetSocketAddress LISTEN_ADDR = new InetSocketAddress("127.0.0.1", LISTEN_PORT);
    private int BAD_GOOGLE_PORT = 6666;
    private InetSocketAddress BAD_GOOGLE_ADDR = new InetSocketAddress("www.google.com", BAD_GOOGLE_PORT);
    
    private ServerSocket LISTEN_SOCKET;
    private LimitedSocketController CONTROLLER;
    
    public LimitedSocketControllerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimitedSocketControllerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        LISTEN_SOCKET = new ServerSocket(LISTEN_PORT);
        LISTEN_SOCKET.setReuseAddress(true);
        CONTROLLER = new LimitedSocketController(2);
    }
    
    public void tearDown() throws Exception {
        LISTEN_SOCKET.close();
    }
    
    public void testSimpleConnect() throws Exception {
        connect(false, true);
    }
    
    public void testSimpleConnectNB() throws Exception {
        connect(true, true);
    }
    
    public void testFailureConnect() throws Exception {
        connect(false, false);
    }
    
    public void testFailureConnectNB() throws Exception {
        connect(true, false);
    }
    
    public void testMaxConcurrent() throws Exception {
        assertEquals(2, CONTROLLER.getNumAllowedSockets());
    }
    
    public void testWaitsForTurn() throws Exception {
        ConnectObserverStub o1 = new ConnectObserverStub();
        ConnectObserverStub o2 = new ConnectObserverStub();
        CONTROLLER.connect(FACTORY, BAD_GOOGLE_ADDR, TIMEOUT, o1);
        CONTROLLER.connect(FACTORY, BAD_GOOGLE_ADDR, TIMEOUT, o2);
        // the above two will stall for awhile while trying to connect...
        long then = System.currentTimeMillis();
        connect(false, true);
        long now = System.currentTimeMillis();
        assertGreaterThan(10000, now - then); // make sure it took awhile
    }
    
    public void testWaitsForTurnNB() throws Exception {
        ConnectObserverStub o1 = new ConnectObserverStub();
        ConnectObserverStub o2 = new ConnectObserverStub();
        CONTROLLER.connect(FACTORY, BAD_GOOGLE_ADDR, TIMEOUT, o1);
        CONTROLLER.connect(FACTORY, BAD_GOOGLE_ADDR, TIMEOUT, o2);
        // the above two will stall for awhile while trying to connect...
        long then = System.currentTimeMillis();
        connect(true, true);
        long now = System.currentTimeMillis();
        assertGreaterThan(10000, now - then); // make sure it took awhile
    }
    
    public void testRemoveObserver() throws Exception {
        ConnectObserverStub o1 = new ConnectObserverStub();
        CONTROLLER.connect(FACTORY, LISTEN_ADDR, 0, o1);
        assertFalse(CONTROLLER.removeConnectObserver(o1));
        
        ConnectObserverStub o2 = new ConnectObserverStub();
        ConnectObserverStub o3 = new ConnectObserverStub();
        CONTROLLER.connect(FACTORY, BAD_GOOGLE_ADDR, TIMEOUT, o2);
        CONTROLLER.connect(FACTORY, BAD_GOOGLE_ADDR, TIMEOUT, o3);

        ConnectObserverStub o4 = new ConnectObserverStub();
        CONTROLLER.connect(FACTORY, LISTEN_ADDR, 0, o4);
        assertTrue(CONTROLLER.removeConnectObserver(o4));
        o3.waitForResponse(60000); // wait until o2 & o3 finish, make sure 4 didn't try
        Thread.sleep(1000);
        assertFalse(o4.isShutdown());
        assertNull(o4.getSocket());
        assertNull(o4.getIoException());
    }
    
    public void testRemoveObserverWithProxies() throws Exception {
        ConnectionSettings.PROXY_HOST.setValue("www.google.com");
        ConnectionSettings.PROXY_PORT.setValue(LISTEN_PORT);
        ConnectionSettings.CONNECTION_METHOD.setValue(ConnectionSettings.C_SOCKS5_PROXY);
        
        ConnectObserverStub o1 = new ConnectObserverStub();
        CONTROLLER.connect(FACTORY, LISTEN_ADDR, 0, o1);
        assertFalse(CONTROLLER.removeConnectObserver(o1));
        
        ConnectObserverStub o2 = new ConnectObserverStub();
        ConnectObserverStub o3 = new ConnectObserverStub();
        CONTROLLER.connect(FACTORY, BAD_GOOGLE_ADDR, TIMEOUT, o2);
        CONTROLLER.connect(FACTORY, BAD_GOOGLE_ADDR, TIMEOUT, o3);

        ConnectObserverStub o4 = new ConnectObserverStub();
        CONTROLLER.connect(FACTORY, LISTEN_ADDR, 0, o4);
        assertTrue(CONTROLLER.removeConnectObserver(o4));
        o3.waitForResponse(60000); // wait until o2 & o3 finish, make sure 4 didn't try
        Thread.sleep(1000);
        assertFalse(o4.isShutdown());
        assertNull(o4.getSocket());
        assertNull(o4.getIoException());       
    }
    
    private void connect(boolean nb, boolean success) throws Exception {
        if (success) {
            Socket s;
            if (!nb) {
                s = CONTROLLER.connect(FACTORY, LISTEN_ADDR, 0, null);
            } else {
                ConnectObserverStub o = new ConnectObserverStub();
                s = CONTROLLER.connect(FACTORY, LISTEN_ADDR, 0, o);
                o.waitForResponse(60000);
                assertEquals(s, o.getSocket());
                assertNull(o.getIoException());
                assertFalse(o.isShutdown());
            }
            // Must connect somewhere, NPE is an error
            s.close();
        } else {
            if (!nb) {
                try {
                    CONTROLLER.connect(FACTORY, BAD_ADDR, 0, null);
                    fail("acceptedConnection from a bad proxy server");
                } catch (IOException iox) {
                    // Good -- expected behaviour
                }
            } else {
                ConnectObserverStub o = new ConnectObserverStub();
                CONTROLLER.connect(FACTORY, BAD_ADDR, 0, o);
                o.waitForResponse(60000);
                assertNull(o.getSocket());
                assertNull(o.getIoException());
                assertTrue(o.isShutdown());
            }
        }
    }    

}
