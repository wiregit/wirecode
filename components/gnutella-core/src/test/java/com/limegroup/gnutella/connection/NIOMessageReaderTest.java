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
import com.limegroup.gnutella.messages.Message;
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
     * Test to make sure the constructor works properly.  In particular, tests
     * to make sure that the constructor properly handles any leftover message
     * data from handshaking.
     * 
     * @throws Exception if any unexpected exception is thrown, indicating an
     *  error
     */
    public void testConstructor() throws Exception {
        PingRequest ping = new PingRequest((byte)3);
        String header = HeaderNames.USER_AGENT + ": "+
            CommonUtils.getHttpServer() + "\r\n";
        ByteBuffer buffer = createTestBuffer(ping, header);        
        Handshaker testHandshaker = new TestHandshaker(buffer);
    }
    

    
    /**
     * Tests the createMessage method to make sure that it correctly creates
     * message from ByteBuffers.
     * 
     * @throws Exception if any unexpected exception is thrown, indicating an
     *  error
     */
    public void testCreateMessage() throws Exception {
        PingRequest ping = new PingRequest((byte)3);
        String header = HeaderNames.USER_AGENT + ": "+
            CommonUtils.getHttpServer() + "\r\n";
        ByteBuffer buffer = createTestBuffer(ping, header);
        
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

        // make sure we can read in all of the pings in the buffer
        while(buffer.remaining() >= ping.getTotalLength()) {
            headerBuffer.clear();
            payloadBuffer.clear();      
            // we should be able to successfully read in the ping.    
            PingRequest pingRead = 
                (PingRequest)createMessage.invoke(NIOMessageReader.class, 
                    params);
                
            // make sure the GUID is the same as the one we wrote to the buffer
            assertEquals("should have same ttls", ping.getTTL(), 
                pingRead.getTTL());
            assertEquals("should have same hops", ping.getHops(), 
                pingRead.getHops());
            assertEquals("should have same guids", new GUID(ping.getGUID()), 
                new GUID(pingRead.getGUID()));
        }
        
        // Now, there should be 3 bytes left over in the buffer -- the partial
        // header for the next ping.  Make sure this is read into the header
        // correctly
        headerBuffer.clear();
        payloadBuffer.clear();    
          
        // we should be able to successfully read in the ping.    
        PingRequest pingRead = 
            (PingRequest)createMessage.invoke(NIOMessageReader.class, params);
        assertNull("should be null", pingRead);
        assertEquals("unexpected number of bytes remaining in the header",
            headerBuffer.remaining(), 20);
    }

    /**
     * Utility method that creates a test byte buffer loaded up with a Gnutella
     * message header, a bunch of messages, and a partial message.
     * 
     * @return a new <tt>ByteBuffer</tt> with header and message data for 
     *  testing
     */
    public ByteBuffer createTestBuffer(Message msg, String header) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        buffer.put(header.getBytes());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            msg.write(baos);
        } catch (IOException e) {
            fail("unexpected exception: "+e);
        }
        byte[] bytes = baos.toByteArray();
        int numPings = 0;
        while(buffer.remaining() > msg.getTotalLength()) {
            buffer.put(bytes);
            numPings++;
        }    
        
        // fill it up with a partial header...
        int remaining = buffer.remaining();
        for(int i=0; i<remaining; i++) {
            buffer.put(bytes[i]);
        }
        buffer.flip();
        return buffer;        
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
