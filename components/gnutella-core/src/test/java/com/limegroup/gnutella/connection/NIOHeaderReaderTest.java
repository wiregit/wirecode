package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.UltrapeerHandshakeResponder;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.http.HTTPHeader;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Tests the class that performs non-blocking reading of Gnutella message
 * headers.
 */
public class NIOHeaderReaderTest extends BaseTestCase {

    private static final int SERVER_PORT = 9000;
    
    private static String[] TEST_HEADERS =  {
        "Content-Length: 4378943\r\n",  
        "Content-Type: application/octet-stream\r\n",
        "X-Alt-Locs: 4378943\r\n",
        "Something-Else: 4378943\r\n",
        "Different: 4378943\r\n",       
    };
    
    public NIOHeaderReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NIOHeaderReaderTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }


    public void testHeaderReadingWithLeftoverHeaders() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        
        for(int i=0; i<TEST_HEADERS.length; i++)  {
            buffer.put(TEST_HEADERS[i].getBytes());
        }
        
        buffer.flip();
        
        
        Socket sock = createLegitSocket();
        NIOHeaderReader reader = 
            NIOHeaderReader.createReader(new Connection(sock));
        
        PrivilegedAccessor.setValue(reader, "_headerByteBuffer", buffer);
        
        // get rid of the first header to trick our little reader...
        PrivilegedAccessor.invokeMethod(reader, "read", new Object[] {buffer},
            new Class[]  {ByteBuffer.class});
        
        //reader.read(buffer);
        
        for(int i=1; i<TEST_HEADERS.length; i++)  {
            assertEquals("unexpected header", 
                TEST_HEADERS[i].trim(), reader.readHeader().toString());
        }

        
        // now, we'll feed headers from our fake channel and make sure that
        // they're read correctly
        reader = 
            NIOHeaderReader.createReader(new Connection(new TestSocket()));

        for(int i=0; i<TEST_HEADERS.length; i++)  {
            assertEquals("unexpected header", 
                TEST_HEADERS[i].trim(), reader.readHeader().toString());
        } 
    }
    
    /**
     * Helper class that creates a socket that returns a "dummy" socket and
     * channel -- useful for testing.
     */
    private static final class TestSocket extends Socket  {
        public SocketChannel getChannel()   {
            return new TestChannel();
        } 
        
        public InetAddress getInetAddress()   {
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                // this should not happen
                return null;
            }
        } 
    }
    
    /**
     * "Dummy" test channel useful to avoid having to set up real sockets for
     * testing.
     */
    private static final class TestChannel extends SocketChannel  {

        public TestChannel()  {
            super(null);
        }
        
        // stub method
        public boolean finishConnect() throws IOException {
            return false;
        }

        // stub method
        public boolean isConnected() {
            return false;
        }

        // stub method
        public boolean isConnectionPending() {
            return false;
        }

        // stub method
        public Socket socket() {
            return null;
        }

        // stub method
        public boolean connect(SocketAddress arg0) throws IOException {
            return false;
        }

        // stub method
        public int read(ByteBuffer buf) throws IOException {
            
            int numRead = 0;
            for(int i=0; i<TEST_HEADERS.length; i++)  {
                byte[] bytes = TEST_HEADERS[i].getBytes();
                numRead += bytes.length;
                buf.put(bytes);
            }
            
            return numRead;
        }

        // stub method
        public int write(ByteBuffer arg0) throws IOException {
            return 0;
        }

        // stub method
        public long read(ByteBuffer[] arg0, int arg1, int arg2) 
            throws IOException {
            return 0;
        }

        // stub method
        public long write(ByteBuffer[] arg0, int arg1, int arg2) 
            throws IOException {
            return 0;
        }

        // stub method
        protected void implCloseSelectableChannel() throws IOException {       
        }

        // stub method
        protected void implConfigureBlocking(boolean arg0) throws IOException { 
        }
        
    }
    
    private static Socket createLegitSocket()  throws Exception {
        
        // do a little work to get a real socket
        //ServerSocket ss = new ServerSocket(SERVER_PORT);

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(true);
        ssc.socket().bind(new InetSocketAddress(SERVER_PORT));
        ServerSocket ss = ssc.socket();
        

        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        final Connection conn = 
            new Connection("localhost", SERVER_PORT, 
                new UltrapeerHeaders("localhost"),
                new UltrapeerHandshakeResponder("localhost"));


         // start a connection that will write it's Gnutella headers using
         // non-blocking writes.
         Thread connectThread = new Thread(new Runnable() {
             public void run() {
                 try {
                     conn.initialize();
                 } catch(IOException e) {
                     // don't care about this exception -- this can happen when
                     // apparently when the test thread dies and the socket 
                     // closes??
                 }
             }
         });

         connectThread.setDaemon(true);
         connectThread.start();

         Socket sock = ss.getChannel().accept().socket();  
         return sock ;  
    }
   

    /**
     * Tests the class for reading individual Gnutella connection headers.
     */
    public void testHttpHeaderClass() throws Exception   {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        String[] headers =  {
            "Content-Length: 4378943\r\n",  
            "Content-Type: application/octet-stream\r\n",
            "X-Alt-Locs: 4378943\r\n",
            "Something-Else: 4378943\r\n",
            "Different: 4378943\r\n",       
        };
        
        for(int i=0; i<headers.length; i++)  {
            buffer.put(headers[i].getBytes());
        }


        buffer.flip();
        //NIOHeaderReader.HttpHeader reader = new NIOHeaderReader.HttpHeader();
        
        NIOHeaderReader reader = 
            NIOHeaderReader.createReader(new Connection(new TestSocket()));
        
        // get rid of the first header to trick our little reader...
        //PrivilegedAccessor.invokeMethod(reader, "read", new Object[] {buffer});
        
        for(int i=0; i<headers.length; i++)  {
            assertEquals("headers should be equal", headers[i].trim(),
                (String)PrivilegedAccessor.invokeMethod(reader, 
                    "read", new Object[] {buffer}, 
                    new Class[]  {ByteBuffer.class})); 
                //reader.read(buffer));
        }
    } 
    
    /**
     * Tests to make sure that an IO exception is correctly thrown when a
     * header is too long.
     */
    public void testHttpHeaderClassForTooLongHeaders() throws Exception   {
        ByteBuffer buffer = ByteBuffer.allocate(2048);

        for(int i=0; i<60; i++)  {
            buffer.put("making headers even longer...".getBytes());
        }

        buffer.flip();

        
        assertGreaterThan("should be larger than max size", 1024,
            buffer.remaining());
            
        NIOHeaderReader reader = 
            NIOHeaderReader.createReader(new Connection(new TestSocket()));
        //NIOHeaderReader.HttpHeader reader = new NIOHeaderReader.HttpHeader();
        
        
        try {
            PrivilegedAccessor.invokeMethod(reader, 
                "read", new Object[] {buffer});
            //reader.read(buffer);
            fail("should have thrown an IO exception");
        } catch(Exception e)  {
            // this is expected because the header is too long
        }
    }
}
