package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import com.limegroup.gnutella.connection.ReadBufferChannel;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests that ThrottleWriter throttles data correctly.
 */
public final class ThrottleReaderTest extends LimeTestCase {
    
    private FakeThrottle THROTTLE = new FakeThrottle();
    private ReadBufferChannel SOURCE = new ReadBufferChannel();
    private ThrottleReader READER = new ThrottleReader(THROTTLE, SOURCE);
    private ByteBuffer BUFFER = ByteBuffer.allocate(1024);
    private static Random RND = new Random();
    

	public ThrottleReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ThrottleReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void setUp() {
	    THROTTLE.clear();
	}
	
	public void testInterestAndBandwidthAvailable() throws Exception {
	    assertFalse(SOURCE.isInterested());
	    assertEquals(0, THROTTLE.interests());
        READER.interest(false);
	    assertFalse(SOURCE.isInterested());
	    assertEquals(0, THROTTLE.interests());
        READER.interest(true);
        assertEquals(1, THROTTLE.interests());
	    assertFalse(SOURCE.isInterested());
	    
	    READER.bandwidthAvailable();
	    assertTrue(SOURCE.isInterested());
    }
    
    public void testeHandleReadDataLeft() throws Exception {
        // Test when data is still left after available.

        // set up reader & SOURCE.
        READER.interest(true);
        READER.bandwidthAvailable();
        assertTrue(SOURCE.isInterested());
        
        // set up SOURCE & THROTTLE.
        SOURCE.setBuffer(buffer(data(750)));
        assertEquals(1, THROTTLE.interests());
        THROTTLE.setAvailable(250);
        
        assertFalse(THROTTLE.didRequest());
        assertFalse(THROTTLE.didRelease());
        doRead(BUFFER, false);
        assertTrue(THROTTLE.didRequest());
        assertTrue(THROTTLE.didRelease());
      
        assertEquals(250, BUFFER.position());
        assertEquals(250, SOURCE.getBuffer().position());
        assertEquals(0, THROTTLE.getAvailable()); // all throttle used up.
        assertEquals(500, SOURCE.getBuffer().remaining()); // data still in source
        assertEquals(2, THROTTLE.interests()); // is interested in more events.
        assertTrue(SOURCE.isInterested()); // channel is still interested
    }

    public void testHandleReadSourceEmptiesWithLeftover() throws Exception {
        SOURCE.setBuffer(buffer(data(500)));
        READER.interest(true);
        READER.bandwidthAvailable();
        assertEquals(1, THROTTLE.interests());
        THROTTLE.setAvailable(550);        
        
        doRead(BUFFER, true);
        assertEquals(500, BUFFER.position());
        assertEquals(500, SOURCE.getBuffer().position());
        assertEquals(0, SOURCE.getBuffer().remaining());
        assertEquals(50, THROTTLE.getAvailable()); // throttle still has data
        assertEquals(1, THROTTLE.interests()); // didn't request interest again.
        assertFalse(SOURCE.isInterested()); // sink off.
    }

    public void testHandleReadSourceEmptiesExactly() throws Exception {
        SOURCE.setBuffer(buffer(data(200)));
        READER.interest(true);
        READER.bandwidthAvailable();
        assertEquals(1, THROTTLE.interests());
        THROTTLE.setAvailable(200);
        READER.bandwidthAvailable();
        assertTrue(SOURCE.isInterested());
        doRead(BUFFER, true);
        assertEquals(1, THROTTLE.interests());
        assertEquals(200, BUFFER.position());
        assertEquals(200, SOURCE.getBuffer().position());
        assertEquals(0, SOURCE.getBuffer().remaining());
        assertEquals(0, THROTTLE.getAvailable());
        assertFalse(SOURCE.isInterested());
    }
    
    public void testHandleReadSinkFills() throws Exception {
        SOURCE.setBuffer(buffer(data(100)));
        READER.interest(true);
        READER.bandwidthAvailable();
        assertEquals(1, THROTTLE.interests());        
        
        BUFFER.limit(75);
        THROTTLE.setAvailable(300);
        assertEquals(1, THROTTLE.interests());
        
        doRead(BUFFER, false);
        assertEquals(75, BUFFER.position());
        assertEquals(75, SOURCE.getBuffer().position());
        assertEquals(25, SOURCE.getBuffer().remaining());
        assertEquals(0, BUFFER.remaining());
        assertEquals(225, THROTTLE.getAvailable());
        assertEquals(2, THROTTLE.interests());
        assertTrue(SOURCE.isInterested());
        
        BUFFER.limit(150);
        THROTTLE.clear();
        THROTTLE.setAvailable(200);
        READER.bandwidthAvailable();
        assertEquals(0, THROTTLE.interests());
        doRead(BUFFER, true);
        assertEquals(100, BUFFER.position());
        assertEquals(50, BUFFER.remaining());
        assertEquals(100, SOURCE.getBuffer().position());
        assertEquals(0, SOURCE.getBuffer().remaining());
        assertEquals(175, THROTTLE.getAvailable());
        assertEquals(0, THROTTLE.interests());
        assertFalse(SOURCE.isInterested());
    }

    public void testBandwidthAvailableWhenClosed() throws Exception {
        assertFalse(SOURCE.isInterested());
        READER.interest(true);
        assertTrue(READER.bandwidthAvailable());
        assertTrue(SOURCE.isInterested());
        
        SOURCE.interest(false);
        READER.close();
        assertFalse(READER.bandwidthAvailable());
        assertFalse(SOURCE.isInterested());
    }   
    
    public void testInterestOffWhenNoBW() throws Exception {
    	assertFalse(SOURCE.isInterested());
        SOURCE.setBuffer(buffer(data(100)));
        READER.interest(true);
        READER.bandwidthAvailable();
        assertEquals(1, THROTTLE.interests()); 
        THROTTLE.setAvailable(1);
        ByteBuffer buf = buffer (data (10)); 
        doRead(buf, false);
        assertTrue(SOURCE.isInterested());
        assertEquals(2, THROTTLE.interests()); 
        
        // if there is no bandwidth available, the reader should
        // turn interest in the source off
        // (read calls w/o bandwidth come from the selector, not the throttle)
        READER.read(buf);
        assertFalse(SOURCE.isInterested());
        
        // and interest itself for when there is bandwidth later
        assertEquals(3, THROTTLE.interests()); 
    }

	private byte[] data(int size) {
	    byte[] data = new byte[size];
	    RND.nextBytes(data);
	    return data;
	}
	
	private ByteBuffer buffer(byte[] data) {
	    return ByteBuffer.wrap(data);
	}
    
    private void doRead(ByteBuffer buffer, boolean interestOff) throws IOException {
        READER.requestBandwidth();
        READER.read(buffer);
        if(interestOff)
            READER.interest(false);
        READER.releaseBandwidth();
    }
}