package com.limegroup.gnutella.connection;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.*;


import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * This class tests the code for writing Gnutella message headers
 * without blocking.
 */
public class NIOHeaderWriterTest extends BaseTestCase {
    
    private static final int SERVER_PORT = 9000;

    public NIOHeaderWriterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NIOHeaderWriterTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        ConnectionSettings.USE_NIO.setValue(true);
    }


    public void testNonBufferedNIOHeaderWriting() throws Exception {
        
        ServerSocket server = new ServerSocket(SERVER_PORT);
        RouterService.connectToHostAsynchronously("127.0.0.1", SERVER_PORT);
        //Connection conn = new Connection("localhost", SERVER_PORT);
        //conn.initialize();
        
        Socket socket = server.accept();
        server.close();
        
        //socket.write();
        InputStream stream = socket.getInputStream();
        String word = IOUtils.readWord(stream, 8);
        System.out.println(word); 
        //char curChar = (char)stream.read();
        StringBuffer curHeader = new StringBuffer();
        for(char curChar = (char)stream.read(); 
            (curChar != '\n' && curChar != '\r'); ) {
            curHeader.append(curChar);
            curChar = (char)stream.read();
            System.out.println(curChar); 
        }

        System.out.println(curHeader); 
        //while(curChar != '\n' && curChar != '\r'){
        //  curChar = (char)stream.read();
        //  curHeader.append(curChar);
        //}                
    }

    //private static final class TestNIOConnection extends Connection {

        //TestNIOConnection() {
            
        //}

    // public TestNIOConnection(String ip, int port) {
    //      super(ip, port);
    //  }

        /**
         * Utility class for testing the NIO header writing code.
         */
    //  private static final class TestNIOSocket extends Socket {

            
            //private final SocketChannel CHANNEL = new TestNIOChannel();


            //public SocketChannel getChannel() {
            //  return CHANNEL;
            //}
    //        }

        /**
         * Utility class for testing the NIO header writing code.
         */
        /*
        private static final class TestNIOChannel extends SocketChannel {
            public int write(ByteBuffer buffer) 
                throws NotYetConnectedException, IOException {
                
            }

            public int write(ByteBuffer buffer, int start, int end) {
            }
        }
        */
    //}
}
