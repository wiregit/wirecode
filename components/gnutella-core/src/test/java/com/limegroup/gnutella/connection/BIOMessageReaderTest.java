package com.limegroup.gnutella.connection;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.util.BaseTestCase;

import java.io.*;

public class BIOMessageReaderTest extends BaseTestCase {
    
    public BIOMessageReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BIOMessageReaderTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Tests to make sure that bad packet exceptions are properly thrown for 
     * bad data.
     * 
     * @throws Exception if anything goes wrong
     */
    public void testBadPacketException() throws Exception {
        Message m1 = new PingRequest((byte)3);
        Message m2 = new PingRequest((byte)3);
        m2.setPriority(5);
        assertGreaterThan(0, m1.compareTo(m2));
        assertLessThan(0, m2.compareTo(m1));
        assertEquals(0, m2.compareTo(m2));
        //Test for null payload with Ping
        
        byte[] bytes = new byte[23];
        byte[] g = GUID.makeGuid();
        for(int i=0;i<16;i++) 
            bytes[i] = g[i];
        bytes[16] = Message.F_PING_REPLY;
        bytes[17] = (byte) 2;//ttl
        bytes[18] = (byte) 2; //hops
        bytes[19] = (byte)0;
        bytes[20] = (byte)0;
        bytes[21] = (byte)0;
        bytes[22] = (byte)0;
        InputStream is = new ByteArrayInputStream(bytes);
        try {
            BIOMessageReader.read(is);
            fail("bpe should have been thrown.");
        } catch(BadPacketException bpe) {
        }
    }
}
