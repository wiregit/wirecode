package com.limegroup.gnutella.connection;

// Edited for the Learning branch

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;

import junit.framework.Test;

import com.limegroup.gnutella.util.*;

/**
 * Tests that DeflaterWriter deflates data written to it correctly,
 * passing it on to the source channel.
 */
public final class DeflaterWriterTest extends BaseTestCase {

	// Make the objects we need to test the DeflaterWriter
    private Deflater DEFLATER = new Deflater();                              // The Java Deflater object which can actually compresses data
    private Inflater INFLATER = new Inflater();                              // The Java Inflater object which can actually decompress data

    private static Random RND = new Random();                                // A random number generator

    private WriteBufferChannel SINK   = new WriteBufferChannel(1024 * 1024); // A WriteBufferChannel with a 1 MB buffer we can write to
    private DeflaterWriter     WRITER = new DeflaterWriter(DEFLATER, SINK);  // The DeflaterWriter we're going to test, have it write to SINK
    
    // There's no way WRITER.observer is set to SOURCE yet

    // Here is where all the backlinking happens
    // Calling this links WRITER.observer to SOURCE, and then links SINK.observer to WRITER
    private WriteBufferChannel SOURCE = new WriteBufferChannel(WRITER);      // A WriteBufferChannel that will write to our DeflaterWriter
    
    // when you make a new object, you pass it the channel to write to
    // the constructor calls interest(this, true) on it, linking it back up to you
    // this is how the list becomes doubly linked
    
    // all this time, this has just been a doubly linked list, it's actually not that confusing
    // now, you need to see how the pull works, how if you write to source, it's actually sink that pulls from writer and writer that pulls from source
    
    // Now is WRITER.observer set to SOURCE? what sets it?

	public DeflaterWriterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DeflaterWriterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	public void tearDown() {
	    DEFLATER.end();
	    INFLATER.end();
	}
	
	public void testSimpleDeflation() throws Exception {
		
		// Make a byte array that holds 10 KB of data, and have our source use it
	    byte[] data = data(10 * 1024);
	    SOURCE.setBuffer(buffer(data));

	    // Have the DeflaterWriter compress and send everything it has into SINK
	    // Since it doesn't have any data yet, it should be able to write everything
	    // Since it doesn't have any data, it will ask for some
	    
	    // How does it get a reference to call SOURCE to get SOURCE to write to it?
	    
	    assertFalse(WRITER.handleWrite()); // should have been able to write everything.
	    
	    ByteBuffer deflated = SINK.getBuffer();
	    assertGreaterThan(0, deflated.limit());
	    byte[] inflated = inflate(deflated);
	    assertEquals(data, inflated);
	}
	
	public void testSourceEmptiesAndFillsRepeatedly() throws Exception {
	    byte[] data = data(3 * 1024);
	    SOURCE.setBuffer(buffer(data));
	    assertFalse(WRITER.handleWrite()); // should have been able to write everything.
	    
	    ByteBuffer deflated = SINK.getBuffer();
	    assertGreaterThan(0, deflated.limit());
	    byte[] inflated = inflate(deflated);
	    assertEquals(data, inflated);
	    
	    SINK.clear();
	    data = data(8 * 1024 + 127);
	    SOURCE.setBuffer(buffer(data));
	    assertFalse(WRITER.handleWrite());
	    deflated = SINK.getBuffer();
	    assertGreaterThan(0, deflated.limit());
	    inflated = inflate(deflated);
	    assertEquals(data, inflated);
	    
	    SINK.clear();
	    data = data(123615);
	    SOURCE.setBuffer(buffer(data));
	    assertFalse(WRITER.handleWrite());
	    deflated = SINK.getBuffer();
	    assertGreaterThan(0, deflated.limit());
	    inflated = inflate(deflated);
	    assertTrue(Arrays.equals(data, inflated));
	    assertEquals(data, inflated);
    }
    
    public void testSinkFillsAndEmptiesRepeatedly() throws Exception {
        byte[] data = data(52 * 1024);
        ByteBuffer compare = ByteBuffer.wrap(data);
        ByteBuffer out = ByteBuffer.allocate(data.length);
        SOURCE.setBuffer(buffer(data));
        
        SINK.resize(100);
        assertTrue(WRITER.handleWrite()); // still data to write.
        ByteBuffer deflated = SINK.getBuffer();
        assertGreaterThan(0, deflated.limit());
        byte[] inflated = inflate(deflated);
        assertGreaterThan(0, inflated.length);
        int position = 0;
        int limit = inflated.length + position;
        assertEquals(compare.limit(limit).position(position), buffer(inflated));
        out.put(inflated);
        assertEquals(out.position(), compare.limit());        
        
        SINK.resize(5000);
        assertTrue(WRITER.handleWrite()); // still data to write.
        deflated = SINK.getBuffer();
        assertGreaterThan(0, deflated.limit());
        inflated = inflate(deflated);
        assertGreaterThan(0, inflated.length);
        position = compare.limit();
        limit = inflated.length + position;
        assertEquals(compare.limit(limit).position(position), buffer(inflated));
        out.put(inflated);
        assertEquals(out.position(), compare.limit());
        
        SINK.resize(13535);
        assertTrue(WRITER.handleWrite()); // still data to write.
        deflated = SINK.getBuffer();
        assertGreaterThan(0, deflated.limit());
        inflated = inflate(deflated);
        assertGreaterThan(0, inflated.length);
        position = compare.limit();
        limit = inflated.length + position;
        assertEquals(compare.limit(limit).position(position), buffer(inflated));
        out.put(inflated);
        assertEquals(out.position(), compare.limit());
        
        SINK.resize(data.length * 2); // top it off.
        assertFalse(WRITER.handleWrite()); // nothing left to write.
        deflated = SINK.getBuffer();
        assertGreaterThan(0, deflated.limit());
        inflated = inflate(deflated);
        assertGreaterThan(0, inflated.length);
        position = compare.limit();
        limit = inflated.length + position;
        assertEquals(compare.limit(limit).position(position), buffer(inflated));
        out.put(inflated);
        assertEquals(out.position(), compare.limit());
        
        assertEquals(buffer(data), out.flip());
    }
	
	private byte[] data(int size) {
	    byte[] data = new byte[size];
	   // for(int i = 0; i < size; i++)
	     //   data[i] = (byte)(i % 10);
	    RND.nextBytes(data);
	    return data;
	}
	
	private ByteBuffer buffer(byte[] data) {
	    return ByteBuffer.wrap(data);
	}
	
	private byte[] inflate(ByteBuffer data) throws Exception {
	    INFLATER.setInput(data.array(), 0, data.limit());
	    byte[] buf = new byte[512];
	    ByteArrayOutputStream out = new BufferByteArrayOutputStream();
	    int inflated = 0;
	    while( (inflated = INFLATER.inflate(buf)) > 0)
	        out.write(buf, 0, inflated);
	    return out.toByteArray();
    }
}