package com.limegroup.gnutella.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;

import junit.framework.Test;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.connection.WriteBufferChannel;

/**
 * Tests that ThrottleWriter throttles data correctly.
 */
public final class ThrottleWriterTest extends BaseTestCase {
    
    private FakeThrottle THROTTLE = new FakeThrottle();
    private WriteBufferChannel SINK = new WriteBufferChannel(1024 * 1024);
    private ThrottleWriter WRITER = new ThrottleWriter(THROTTLE, SINK);
    private static Random RND = new Random();
    private WriteBufferChannel SOURCE = new WriteBufferChannel(WRITER);

	public ThrottleWriterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ThrottleWriterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void setUp() {
	    THROTTLE.clear();
	}
	
	public void testInterestAndBandwidthAvailable() throws Exception {
	    assertFalse(SINK.interested());
	    assertEquals(0, THROTTLE.interests());
	    WRITER.interest(SOURCE, false);
	    assertFalse(SINK.interested());
	    assertEquals(0, THROTTLE.interests());
	    WRITER.interest(SOURCE, true);
        assertEquals(1, THROTTLE.interests());
	    assertFalse(SINK.interested());
	    
	    WRITER.bandwidthAvailable();
	    assertTrue(SINK.interested());
    }
    
    public void testeHandleWriteDataLeft() throws Exception {
        assertEquals(0, THROTTLE.interests());
        WRITER.interest(SOURCE, true);
        assertEquals(1, THROTTLE.interests());
        
        // Test when data is still left after available.

        // set up writer & SINK.
        WRITER.bandwidthAvailable();
        assertTrue(SINK.interested());
        assertEquals(0, SINK.written());
        
        // set up SOURCE & THROTTLE.
        SOURCE.setBuffer(buffer(data(750)));
        THROTTLE.setAvailable(250);
        
        assertFalse(THROTTLE.didRequest());
        assertFalse(THROTTLE.didRelease());
        assertTrue(WRITER.handleWrite()); // still data left to write.
        assertTrue(THROTTLE.didRequest());
        assertTrue(THROTTLE.didRelease());
        
        assertEquals(250, SINK.written()); // only wrote 250
        assertEquals(0, THROTTLE.getAvailable()); // all throttle used up.
        assertEquals(500, SOURCE.remaining()); // data still in source
        assertEquals(2, THROTTLE.interests()); // is interested in more events.
        assertFalse(SINK.interested()); // sink should not give events.
    }
    
    public void testHandleWriteSourceEmptiesWithLeftover() throws Exception {
        assertEquals(0, THROTTLE.interests());
        WRITER.interest(SOURCE, true);
        assertEquals(1, THROTTLE.interests());
        
        SOURCE.setBuffer(buffer(data(500)));
        THROTTLE.setAvailable(550);        

        WRITER.bandwidthAvailable();
        assertTrue(SINK.interested());
        
        assertFalse(WRITER.handleWrite()); // all data written
        assertEquals(500, SINK.written()); // only wrote that we had.
        assertEquals(50, THROTTLE.getAvailable()); // throttle still has data
        assertEquals(1, THROTTLE.interests()); // didn't request interest again.
        assertFalse(SINK.interested()); // sink off.
    }
        
    public void testHandleWriteSourceEmptiesExactly() throws Exception {
        assertEquals(0, THROTTLE.interests());
        WRITER.interest(SOURCE, true);
        assertEquals(1, THROTTLE.interests());
        
        SOURCE.setBuffer(buffer(data(200)));
        THROTTLE.setAvailable(200);
        WRITER.bandwidthAvailable();
        assertEquals(0, SINK.written());
        assertTrue(SINK.interested());
        assertFalse(WRITER.handleWrite());
        assertEquals(1, THROTTLE.interests());
        assertEquals(200, SINK.written());
        assertEquals(0, THROTTLE.getAvailable());
        assertEquals(0, SOURCE.remaining());
        assertFalse(SINK.interested());
    }
    
    public void testHandleWriteSinkFills() throws Exception {
        // test to make sure if sink can't take data that we release &
        // say we wrote it all, even if we still had stuff to write.
        SINK.resize(100);
        SOURCE.setBuffer(buffer(data(250)));
        THROTTLE.setAvailable(300);
        WRITER.bandwidthAvailable();
        WRITER.interest(SOURCE, true);
        assertEquals(1, THROTTLE.interests());
        
        assertTrue(WRITER.handleWrite()); // data still leftover.
        assertEquals(100, SINK.written());
        assertEquals(150, SOURCE.remaining());
        assertEquals(200, THROTTLE.getAvailable());
        assertEquals(2, THROTTLE.interests());
        assertFalse(SINK.interested());
        
        SINK.resize(150);
        THROTTLE.clear();
        THROTTLE.setAvailable(200);
        WRITER.bandwidthAvailable();
        assertEquals(0, THROTTLE.interests());
        assertFalse(WRITER.handleWrite());
        assertEquals(150, SINK.written());
        assertEquals(50, THROTTLE.getAvailable());
        assertEquals(0, THROTTLE.interests());
        assertFalse(SINK.interested());
    }
    
    public void testBandwidthAvailableWhenClosed() throws Exception {
        assertFalse(SINK.interested());
        assertTrue(WRITER.bandwidthAvailable());
        assertTrue(SINK.interested());
        
        SINK.interest(null, false);
        assertFalse(SINK.interested());
        SINK.close();
        assertFalse(WRITER.bandwidthAvailable());
        assertFalse(SINK.interested());
    }   
	
	private byte[] data(int size) {
	    byte[] data = new byte[size];
	    RND.nextBytes(data);
	    return data;
	}
	
	private ByteBuffer buffer(byte[] data) {
	    return ByteBuffer.wrap(data);
	}
}