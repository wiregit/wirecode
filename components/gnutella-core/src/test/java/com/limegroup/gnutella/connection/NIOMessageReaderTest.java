package com.limegroup.gnutella.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.TestSocket;

/**
 * Test the class that performs non-blocking reads of Gnutella messages.
 */
public class NIOMessageReaderTest extends BaseTestCase {

    /**
     * @param name
     */
    public NIOMessageReaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(NIOMessageReaderTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Tests the createMessage method to make sure that it correctly creates
     * message from ByteBuffers.
     * 
     * @throws Exception in any unexpected exception is thrown, indicating an
     *  error
     */
    public void testCreateMessage() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String header = HeaderNames.USER_AGENT + ": "+
            CommonUtils.getHttpServer() + "\r\n";
        buffer.put(header.getBytes());
        PingRequest ping = new PingRequest((byte)3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ping.write(baos);
        } catch (IOException e) {
            fail("unexpected exception: "+e);
        }
        buffer.put(baos.toByteArray());    
        buffer.flip();
        
        Connection testConnection = new Connection(new TestSocket());
        NIOHeaderReader reader = 
            NIOHeaderReader.createReader(testConnection);
        Method read = 
            PrivilegedAccessor.getMethod(reader, "read", 
                new Class[]{ByteBuffer.class});    
        
        String headerRead = (String)read.invoke(reader, new Object[] {buffer});
        assertEquals("unexpected header read", header.trim(), headerRead);
        
        ByteBuffer headerBuffer = ByteBuffer.allocate(23);
        
        ByteBuffer payloadBuffer = 
            ByteBuffer.allocate(MessageSettings.MAX_LENGTH.getValue());
            
        Method createMessage =
            PrivilegedAccessor.getMethod(NIOMessageReader.class, 
                "createMessage", new Class[] {ByteBuffer.class, 
                    ByteBuffer.class, Connection.class, ByteBuffer.class});
        
        Object[] params = new Object[] {
            headerBuffer, payloadBuffer, testConnection, buffer,
        };        
        
        // we should be able to successfully read in the ping.    
        PingRequest pingRead = 
            (PingRequest)createMessage.invoke(NIOMessageReader.class, params);
            
        // make sure the GUID is the same as the one we wrote to the buffer
        assertEquals("should have same ttls", ping.getTTL(), pingRead.getTTL());
        assertEquals("should have same hops", ping.getHops(), 
            pingRead.getHops());
        assertEquals("should have same guids", new GUID(ping.getGUID()), 
            new GUID(pingRead.getGUID()));
    }


    /**
     * Helper class that allows test for random left over ByteBuffer from
     * handshaking -- useful for making sure that we properly take that
     * data and turn it into messages.
     */
    private static class TestHandshaker implements Handshaker {
        
        private final ByteBuffer BUFFER;

        TestHandshaker(ByteBuffer buffer) {
            BUFFER = buffer;
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#handshake()
         */
        public boolean handshake() throws IOException, NoGnutellaOkException {
            // TODO Auto-generated method stub
            return false;
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#readComplete()
         */
        public boolean readComplete() {
            // TODO Auto-generated method stub
            return false;
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#writeComplete()
         */
        public boolean writeComplete() {
            // TODO Auto-generated method stub
            return false;
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#getHeadersRead()
         */
        public HandshakeResponse getHeadersRead() {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#getHeadersWritten()
         */
        public Properties getHeadersWritten() {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#getHeaderWritten(java.lang.String)
         */
        public String getHeaderWritten(String string) {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#write()
         */
        public boolean write() throws IOException {
            // TODO Auto-generated method stub
            return false;
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#read()
         */
        public void read() throws IOException {
            // TODO Auto-generated method stub
            
        }

        /* (non-Javadoc)
         * @see com.limegroup.gnutella.connection.Handshaker#getRemainingData()
         */
        public ByteBuffer getRemainingData() {
            return BUFFER;
        }
        
    }
}
