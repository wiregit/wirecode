package com.limegroup.gnutella.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.util.Enumeration;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.sun.java.util.collections.Arrays;

/**
 * Tests the class for performing non-blocking connection handshakes.
 */
public final class NIOHandshakerTest extends BaseTestCase {

    private static final int TEST_PORT = 6388;
    
    /** 
     * End of line for Gnutella 0.6. 
     */
    public static final String CRLF="\r\n";

    /**
     * Gnutella 0.6 connect string.
     */
    protected static String GNUTELLA_CONNECT_06 = "GNUTELLA CONNECT/0.6";
    
       
    /**
     * Creates a new test instance.
     * 
     * @param name
     */
    public NIOHandshakerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(NIOHandshakerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Test to make sure that non-blocking handshaking is working properly for
     * both incoming and outgoing connections.
     * 
     * @throws Exception if anything goes wrong
     */
    public void testHandshaking() throws Exception {
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(true);
        ConnectionSettings.USE_NIO.setValue(true);
        ServerSocket ss = null;

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(true);
        ssc.socket().bind(new InetSocketAddress(TEST_PORT));
        ss = ssc.socket();               

        final Connection outgoingConnection = 
            new Connection("127.0.0.1", TEST_PORT);
        Thread connInit = new Thread(new Runnable() {
            public void run() {
                try {
                    outgoingConnection.initialize();
                } catch (NoGnutellaOkException e) {
                    fail(e);
                } catch (BadHandshakeException e) {
                    fail(e);
                } catch (IOException e) {
                    fail(e);
                }
            }
        }, "connection init thread");
        
        connInit.setDaemon(true);
        connInit.start();
        
        Socket socket = ss.accept();
        ss.close();
        
        socket.getChannel().configureBlocking(false); 
        socket.setSoTimeout(3000);
        
        HandshakeResponder responder = new UltrapeerResponder();

        final Connection incomingConnection = new Connection(socket, responder);
         

        Thread incomingInitializer = new Thread(new Runnable() {
            public void run() {
                try {
                    incomingConnection.initialize();
                } catch (NoGnutellaOkException e) {
                    fail(e);
                } catch (BadHandshakeException e) {
                    fail(e);
                } catch (IOException e) {
                    fail(e);
                }
            }}, "incoming connection intializer");
        
        incomingInitializer.setDaemon(true);
        incomingInitializer.start();
        
        while(!incomingConnection.isInitialized()) {
            Thread.sleep(100);
        }    
        
        ConnectionSettings.IGNORE_KEEP_ALIVE.setValue(false);  
        
        
        assertTrue("should be connected", incomingConnection.isInitialized()); 
        assertTrue("should be connected", outgoingConnection.isInitialized()); 
        
        // check to make sure the connection headers have the expected values
        HandshakeResponse incomingHeaders = incomingConnection.headers();
        HandshakeResponse outgoingHeaders = outgoingConnection.headers();
        
        assertTrue("should be high degree", 
            incomingHeaders.isHighDegreeConnection()); 
        assertTrue("should be high degree", 
            outgoingHeaders.isHighDegreeConnection()); 
        assertTrue("should be dynamic query connection", 
            incomingHeaders.isDynamicQueryConnection()); 
        assertTrue("should be dynamic query connection", 
            outgoingHeaders.isDynamicQueryConnection()); 
        assertTrue("should be qrp enabled", 
            incomingHeaders.isQueryRoutingEnabled()); 
        assertTrue("should be qrp enabled", 
            outgoingHeaders.isQueryRoutingEnabled()); 
    }
    
    private static class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) throws IOException {
            Properties props = new UltrapeerHeaders("127.0.0.1"); 
            props.put(HeaderNames.X_DEGREE, "42");           
            return HandshakeResponse.createResponse(props);
        }
    }
    
    /**
     * Tests the method for building the outgoing connection request buffer.
     * 
     * @throws Exception if any unexpected exception happens in the test
     */
    public void testBuildRequestBuffer() throws Exception  {
        
        Properties props = new UltrapeerHeaders("localhost");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        byte[] connectBytes = (GNUTELLA_CONNECT_06+CRLF).getBytes();
        os.write(connectBytes);
        sendHeaders(props, os);
        
        byte[] standardBytes = os.toByteArray();
        
        ByteBuffer buffer = 
            (ByteBuffer)PrivilegedAccessor.invokeMethod(NIOHandshaker.class,
                "createBuffer", new Object[] {"GNUTELLA CONNECT/0.6", props}, 
                new Class[]  {String.class, Properties.class});
        
        buffer.flip();
        byte[] bytesToCompare = new byte[buffer.capacity()];
        
        buffer.get(bytesToCompare);
        
        assertEquals("lengths should be equal", standardBytes.length, 
            bytesToCompare.length);
            
//        for(int i=0; i<bytesToCompare.length; i++)  {
//            System.out.print((char)bytesToCompare[i]);
//        }
//        
//        for(int i=0; i<standardBytes.length; i++)  {
//            System.out.print((char)standardBytes[i]);
//        }
        assertTrue("byte arrays should be identical", 
            Arrays.equals(os.toByteArray(), bytesToCompare));
    }
    
    /**
     * Writes the properties in props to network, including the blank line at
     * the end.  Throws IOException if there are any problems.
     * @param props The headers to be sent. Note: null argument is 
     * acceptable, if no headers need to be sent (still the trailer will
     * be sent
     * @modifies network 
     */
    private void sendHeaders(Properties props, OutputStream out) 
        throws IOException {
        if(props != null) {
            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String key = (String)enum.nextElement();
                String value = props.getProperty(key);
                if (value==null)
                    value = "";
                writeLine(key+": "+value+CRLF, out);   
            }
        }
        //send the trailer
        writeLine(CRLF, out);
    }
    
    /**
     * Writes s to out, with no trailing linefeeds.  Called only from
     * initialize().  
     */
    private void writeLine(String s, OutputStream out) throws IOException {
        byte[] bytes = s.getBytes();     
        out.write(bytes);
        out.flush();
    }

}
