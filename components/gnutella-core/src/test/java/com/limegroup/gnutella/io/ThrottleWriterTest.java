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
	    THROTTLE.interestOff();
	}
	
	public void testInterestAndBandwidthAvailable() throws Exception {
	    assertFalse(SINK.interested());
	    assertFalse(THROTTLE.isInterested());
	    WRITER.interest(SOURCE, false);
	    assertFalse(SINK.interested());
	    assertFalse(THROTTLE.isInterested());
	    WRITER.interest(SOURCE, true);
	    assertTrue(THROTTLE.isInterested());
	    assertFalse(SINK.interested());
	    
	    WRITER.bandwidthAvailable();
	    assertTrue(SINK.interested());
    }
    
    public void testHandleWriteAndStuff() throws Exception {
        assertFalse(THROTTLE.isInterested());
        WRITER.interest(SOURCE, true);
        assertTrue(THROTTLE.isInterested());
        
        // Test when data is still left after available.

        // set up writer & SINK.
        WRITER.bandwidthAvailable();
        assertTrue(SINK.interested());
        assertEquals(0, SINK.written());
        
        // set up SOURCE & THROTTLE.
        SOURCE.setBuffer(buffer(data(750)));
        THROTTLE.setAvailable(250);
        
        assertTrue(WRITER.handleWrite()); // still data left to write.
        assertEquals(250, SINK.written()); // only wrote 250
        assertEquals(0, THROTTLE.getAvailable()); // all throttle used up.
        assertEquals(500, SOURCE.remaining()); // data still in source
        assertTrue(THROTTLE.isInterested()); // throttle should give events still
        assertFalse(SINK.interested()); // sink should not give events.
        assertFalse(THROTTLE.isAllWrote()); // not everything written.
        
        WRITER.bandwidthAvailable();
        assertTrue(SINK.interested());
        SINK.clear();
        
        // Test when available has more than we need.
        THROTTLE.setAvailable(550);
        assertFalse(WRITER.handleWrite()); // all data written
        assertEquals(500, SINK.written()); // only wrote that we had.
        assertEquals(50, THROTTLE.getAvailable()); // throttle still has data
        assertFalse(THROTTLE.isInterested()); // no more requests from throttle
        assertFalse(SINK.interested()); // sink off.
        assertTrue(THROTTLE.isAllWrote()); // everything written.
        
        // Test when available is exactly what we need.
        THROTTLE.clear();
        SINK.clear();
        WRITER.interest(SOURCE, true);
        SOURCE.setBuffer(buffer(data(200)));
        THROTTLE.setAvailable(200);
        WRITER.bandwidthAvailable();
        assertFalse(THROTTLE.isAllWrote());
        assertEquals(0, SINK.written());
        assertTrue(SINK.interested());
        assertFalse(WRITER.handleWrite());
        assertEquals(200, SINK.written());
        assertEquals(0, THROTTLE.getAvailable());
        assertEquals(0, SOURCE.remaining());
        assertFalse(THROTTLE.isInterested());
        assertFalse(SINK.interested());
        assertTrue(THROTTLE.isAllWrote());
    }
    
    public void testReleasesAndWritesAllWhenWriteBlocks() throws Exception {
        // test to make sure if sink can't take data that we release &
        // say we wrote it all, even if we still had stuff to write.
        SINK.resize(100);
        SOURCE.setBuffer(buffer(data(250)));
        THROTTLE.setAvailable(300);
        WRITER.bandwidthAvailable();
        WRITER.interest(SOURCE, true);
        
        assertTrue(WRITER.handleWrite()); // data still leftover.
        assertEquals(100, SINK.written());
        assertEquals(150, SOURCE.remaining());
        assertEquals(200, THROTTLE.getAvailable());
        assertTrue(THROTTLE.isInterested()); // need to wait for sink.
        assertFalse(SINK.interested());
        assertTrue(THROTTLE.isAllWrote()); // 'cause sink blocked.
        
        SINK.resize(150);
        THROTTLE.clear();
        THROTTLE.setAvailable(200);
        WRITER.bandwidthAvailable();
        assertFalse(WRITER.handleWrite());
        assertEquals(150, SINK.written());
        assertEquals(50, THROTTLE.getAvailable());
        assertFalse(THROTTLE.isInterested());
        assertFalse(SINK.interested());
        assertTrue(THROTTLE.isAllWrote());
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