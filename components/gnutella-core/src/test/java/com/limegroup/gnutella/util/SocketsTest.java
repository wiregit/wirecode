package com.limegroup.gnutella.util;

import com.limegroup.gnutella.settings.*;
import junit.framework.Test;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Tests the Sockets utility class.
 */
public class SocketsTest extends BaseTestCase {

    public SocketsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SocketsTest.class);
    }  

    public void testNIOConnect() throws Exception {
        System.out.println("SocketsTest::testNIOConnect"); 
        final int PORT = 9000;
        ConnectionSettings.USE_NIO.setValue(true);
        
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(true);
        ssc.socket().bind(new InetSocketAddress(PORT));
        ServerSocket server = ssc.socket();

        Thread socketThread = new Thread() {
                public void run() {
                    try {
                        Socket sock1 = Sockets.connect("localhost", PORT, 2000);
                        Socket sock2 = Sockets.connect("localhost", PORT, 2000);
                        Socket sock3 = Sockets.connect("localhost", PORT, 2000);
                        Socket sock4 = Sockets.connect("localhost", PORT, 2000);
                        assertTrue("should be connected", sock1.isConnected());
                        assertTrue("should be connected", sock2.isConnected());
                        assertTrue("should be connected", sock3.isConnected());
                        assertTrue("should be connected", sock4.isConnected());
                    } catch(IOException e) {
                        fail("should have been able to connect");
                    }
                }
            };

        socketThread.setDaemon(true);
        socketThread.start();
        Socket client = server.getChannel().accept().socket();
        assertTrue("client should be connected", client.isConnected());
        client.close();
        client = server.getChannel().accept().socket();
        assertTrue("client should be connected", client.isConnected());
        client.close();
        client = server.getChannel().accept().socket();
        assertTrue("client should be connected", client.isConnected());
        client.close();
        client = server.getChannel().accept().socket();
        assertTrue("client should be connected", client.isConnected());
        client.close();
    }

}
