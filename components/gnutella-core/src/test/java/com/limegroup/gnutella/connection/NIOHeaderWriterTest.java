package com.limegroup.gnutella.connection;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.handshaking.*;


import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.lang.reflect.*;

/**
 * This class tests the code for writing Gnutella message headers
 * without blocking.
 */
public class NIOHeaderWriterTest extends BaseTestCase {
    
    private static final int SERVER_PORT = 9000;

    private static final int LIME_PORT = 6669;

    private static Field[] _headers;

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
        // make sure we use NIO
        ConnectionSettings.USE_NIO.setValue(true);
        
        // make sure local connections are allowed
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);

        _headers = HeaderNames.class.getDeclaredFields();
    }        


    /**
     * Tests NIO header writing code with no buffering.  These headers will all
     * be completely written on each call to SocketChannel.write.  We'll later
     * test the buffering code that kicks in when TCP output buffers are
     * full and writes only send partial messages
     */
    public void testNonBufferedNIOHeaderWriting() throws Exception {        
         ServerSocket ss = new ServerSocket(SERVER_PORT);

         final Connection conn = new Connection("127.0.0.1", SERVER_PORT);


         // start a connection that will write it's Gnutella headers using
         // non-blocking writes.
         Thread connectThread = new Thread(new Runnable() {
                 public void run() {
                     try {
                         conn.initialize();
                     } catch(IOException e) {
                         fail(e);
                     }
                 }
             });

         connectThread.setDaemon(true);
         connectThread.start();

         Socket socket = ss.accept();
         ss.close();
         
         InputStream in = socket.getInputStream();
                  
         //String word = IOUtils.readWord(in, 8);    
         
         //System.out.println(word); 

         ByteReader br = new ByteReader(in);
         br.readLine();

         int numHeaders = 0;
         while(true) {
             String curHeader = br.readLine();
             numHeaders++;
             if(curHeader.equals("")) break;
             assertTrue("should be a header we know about: "+curHeader, isKnownHeader(curHeader));
         }

         assertGreaterThan("should have received reasonable number of headers", 5, numHeaders);
    }


    /**
     * Utility class to make sure the header name we're reading is a real header --
     * helps to make sure any partial writes aren't losing data.
     */
    private boolean isKnownHeader(String header) throws Exception {
        String headerName = header.substring(0, header.indexOf(":"));
        boolean foundHeader = false;
        for(int i=0; i<_headers.length; i++) {
            Object value = _headers[i].get(HeaderNames.class);
            if(value instanceof String) {
                String declaredHeaderName = (String)value;
                if(headerName.equals(declaredHeaderName)) {
                    // we've found a matching header!! it's valid so we're all set
                    foundHeader = true;
                    break;
                }
            }
        }

        return foundHeader;
        
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
