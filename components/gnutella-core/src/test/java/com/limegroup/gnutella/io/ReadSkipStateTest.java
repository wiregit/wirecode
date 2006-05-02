package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Random;

import junit.framework.Test;

import com.limegroup.gnutella.connection.ReadBufferChannel;
import com.limegroup.gnutella.io.IOState;
import com.limegroup.gnutella.util.BaseTestCase;

public class ReadSkipStateTest extends BaseTestCase {
    
    private ByteBuffer BUFFER = ByteBuffer.allocate(1024);
    
    public ReadSkipStateTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ReadSkipStateTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private byte[] data(int len) {
        Random rnd = new Random();
        byte b[] = new byte[len];
        rnd.nextBytes(b);
        return b;
    }
    
    public void testSimpleProcess() throws Exception {
        byte[] data = data(100);
        ByteBuffer dbuf = ByteBuffer.wrap(data); 
        ReadBufferChannel channel = new ReadBufferChannel(dbuf);
        ReadSkipState state = new ReadSkipState(53);
        
        assertFalse(state.process(channel, BUFFER));
        assertFalse(dbuf.hasRemaining());
        assertEquals(47, BUFFER.flip().remaining());
        assertEquals(data, 53, 47, BUFFER.array(), 0, 47);
    }
    /*
    public void testComplexProcess() throws Exception {
        String testString = "FIRST LINE\r\n" +
                            "Header1: Value1\r\n" +
                            "Header2: Value2\r\n" +
                            "UnknownData\r\n" +
                            "\r\n" +
                            "Extra Data Leftover";
        ByteBuffer buffer = ByteBuffer.wrap(testString.getBytes());
        ReadBufferChannel channel = new ReadBufferChannel(buffer);

        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        ReadHandshakeTester tester = new ReadHandshakeTester(support);
        
        buffer.limit("FIRS".length());
        assertTrue(tester.process(channel, scratch));
        assertFalse(tester.isProcessedConnectLine());
        assertFalse(tester.isProcessedHeaders());
        assertEquals("FIRS", tester.getCurrentHeader());
        assertFalse(tester.isDoneConnect());
        
        buffer.limit("FIRST LINE\r\n".length());
        assertTrue(tester.process(channel, scratch));
        assertTrue(tester.isProcessedConnectLine());
        assertFalse(tester.isProcessedHeaders());
        assertEquals("", tester.getCurrentHeader());
        assertEquals("FIRST LINE", tester.getConnectLine());
        assertTrue(tester.isDoneConnect());
        
        buffer.limit("FIRST LINE\r\nHeader1: Value1".length());
        assertTrue(tester.process(channel, scratch));
        assertFalse(tester.isProcessedHeaders());
        assertEquals("Header1: Value1", tester.getCurrentHeader());
        assertEquals(0, support.getReadHandshakeResponse().props().size());
        
        buffer.limit("FIRST LINE\r\nHeader1: Value1\r\n".length());
        assertTrue(tester.process(channel, scratch));
        assertFalse(tester.isProcessedHeaders());
        assertEquals("", tester.getCurrentHeader());
        assertEquals(1, support.getReadHandshakeResponse().props().size());
        assertEquals("Value1", support.getReadHandshakeResponse().props().get("Header1"));
        
        buffer.limit("FIRST LINE\r\nHeader1: Value1\r\nHeader2: Value2\r\nUnknownData\r\n".length());
        assertTrue(tester.process(channel, scratch));
        assertFalse(tester.isProcessedHeaders());
        assertEquals("", tester.getCurrentHeader());
        assertEquals(2, support.getReadHandshakeResponse().props().size());
        assertEquals("Value1", support.getReadHandshakeResponse().props().get("Header1"));
        assertEquals("Value2", support.getReadHandshakeResponse().props().get("Header2"));
        
        buffer.limit(testString.length());
        assertFalse(tester.process(channel, scratch));
        assertTrue(tester.isProcessedHeaders());
        assertEquals("", tester.getCurrentHeader());
        assertEquals(2, support.getReadHandshakeResponse().props().size());
        assertEquals("Value1", support.getReadHandshakeResponse().props().get("Header1"));
        assertEquals("Value2", support.getReadHandshakeResponse().props().get("Header2"));
        
        assertEquals("Extra Data Leftover".length(), scratch.position());
        scratch.flip();
        assertEquals("Extra Data Leftover", new String(scratch.array(), 0, scratch.limit()));
    }
    
    public void testEOF() throws Exception {
        String testString = "FIRST LINE\r\n" +
                            "Header1: Value1\r\n" +
                            "Header2: Value2\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(testString.getBytes());
        ReadBufferChannel channel = new ReadBufferChannel(buffer, true);

        ByteBuffer scratch = ByteBuffer.allocate(2048);
        HandshakeSupport support = new HandshakeSupport("127.0.0.1");
        ReadHandshakeTester tester = new ReadHandshakeTester(support);
        try {
            tester.process(channel, scratch);
            fail("should have IOXd");
        } catch(IOException iox) {
            assertEquals("EOF", iox.getMessage());
        }
        
        assertTrue(tester.isProcessedConnectLine());
        assertFalse(tester.isProcessedHeaders());

        Properties props = support.getReadHandshakeResponse().props();
        assertEquals(2, props.size());
        assertEquals("Value1", props.get("Header1"));
        assertEquals("Value2", props.get("Header2"));
    } */
}
