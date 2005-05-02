package com.limegroup.gnutella.connection;

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
    
    private Deflater DEFLATER = new Deflater();
    private WriteBufferChannel SINK = new WriteBufferChannel(32 * 1024);
    private DeflaterWriter WRITER = new DeflaterWriter(DEFLATER, SINK);
    private static Random RND = new Random();
    private WriteBufferChannel SOURCE = new WriteBufferChannel(WRITER);

	public DeflaterWriterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DeflaterWriterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testSimpleDeflation() throws Exception {
	    byte[] data = data(10 * 1024);
	    SOURCE.setBuffer(buffer(data));
	    assertFalse(WRITER.handleWrite()); // should have been able to write everything.
	    
	    ByteBuffer deflated = SINK.getBuffer();
	    assertGreaterThan(deflated.toString(), 0, deflated.limit());
	    byte[] inflated = inflate(deflated);
	    assertEquals(data, inflated);
	}
	
	
	private byte[] data(int size) {
	    byte[] data = new byte[size];
	    RND.nextBytes(data);
	    return data;
	}
	
	private ByteBuffer buffer(byte[] data) {
	    return ByteBuffer.wrap(data);
	}
	

	
	private byte[] inflate(ByteBuffer data) throws Exception {
	    Inflater inflater = new Inflater();
	    inflater.setInput(data.array(), 0, data.limit());
	    byte[] buf = new byte[512];
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    int inflated = 0;
	    while( (inflated = inflater.inflate(buf)) > 0)
	        out.write(buf, 0, inflated);
	    return out.toByteArray();
    }
}