package com.limegroup.gnutella.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.sun.java.util.collections.Arrays;

/**
 * Tests the class for performing non-blocking connection handshakes.
 */
public final class NIOHandshakerTest extends BaseTestCase {

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
                "createRequestBuffer", new Object[] {props}, 
                new Class[]  {Properties.class});
        
        buffer.flip();
        byte[] bytesToCompare = new byte[buffer.capacity()];
        
        buffer.get(bytesToCompare);
        
        assertEquals("lengths should be equal", standardBytes.length, 
            bytesToCompare.length);
            
        for(int i=0; i<bytesToCompare.length; i++)  {
            System.out.print((char)bytesToCompare[i]);
        }
        
        for(int i=0; i<standardBytes.length; i++)  {
            System.out.print((char)standardBytes[i]);
        }
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
