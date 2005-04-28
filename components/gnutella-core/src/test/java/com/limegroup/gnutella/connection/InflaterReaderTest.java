package com.limegroup.gnutella.connection;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.zip.*;
import java.net.*;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;

/**
 * Tests that InflaterReader inflates the source channel correctly.
 */
public final class InflaterReaderTest extends BaseTestCase {
    
    private Inflater INFLATER = new Inflater();
    private InflaterReader READER = new InflaterReader(INFLATER);

	public InflaterReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(InflaterReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    public void testChannelMethods() throws Exception {
        try {
            READER.setReadChannel(null);
            fail("expected NPE");
        } catch(NullPointerException expected) {}
        
        ReadableByteChannel channel = new ReadBufferChannel(new byte[0]);
        READER.setReadChannel(channel);
        assertSame(channel, READER.getReadChannel());
        
        try {
            new InflaterReader(null);
            fail("expected NPE");
        } catch(NullPointerException expected) {}
            
        READER = new InflaterReader(channel, INFLATER);
        assertSame(channel, READER.getReadChannel());
        
        READER = new InflaterReader(null, INFLATER);
        assertNull(READER.getReadChannel());
    }
    
    public void testClosing() throws Exception {
        ReadBufferChannel source = new ReadBufferChannel();
        assertTrue(source.isOpen());
        READER.setReadChannel(source);
        assertTrue(READER.isOpen());
        
        source.setClosed(true);
        assertFalse(READER.isOpen());
        source.setClosed(false);
        assertTrue(READER.isOpen());
        
        assertTrue(source.isOpen());
        READER.close();
        assertFalse(source.isOpen());
    }
    
    public void testFinishedInflation() throws Exception {
        byte[] out = new byte[] { 1, (byte)2, (byte)3, (byte)-127, (byte)7, (byte)6 };
        ByteBuffer b = deflate(out);
        
        READER.setReadChannel(channel(b));
        ByteBuffer in = ByteBuffer.allocate(out.length + 1);
        assertEquals(out.length, READER.read(in));
        assertEquals(out.length, in.position());
        assertEquals(buffer(out), in.flip());
        
        // Since the deflater finished the stream, the next read should return -1.
        in.limit(out.length + 1);
        assertEquals("should have seen finished stream", -1, READER.read(in));
    }
    
    public void testStreamedInflation() throws Exception {
        byte[] out = new byte[] { 1, (byte)2, (byte)3, (byte)-127, (byte)7, (byte)6 };
        ByteBuffer b = stream(out);
        
        READER.setReadChannel(channel(b));
        ByteBuffer in = ByteBuffer.allocate(out.length + 1);
        assertEquals(out.length, READER.read(in));
        assertEquals(out.length, in.position());
        assertEquals(buffer(out), in.flip());
        
        // Since the deflater didn't finish the stream, the next read should return 0.
        in.limit(out.length + 1);
        assertEquals("stream shouldn't have ended", 0, READER.read(in));
    }
    
    public void testLargeInput() throws Exception {
        byte[] out = new byte[128 * 1024];
        new Random().nextBytes(out);
        ByteBuffer b = stream(out);
        assertGreaterThan("deflated data too small", 20 * 1024, b.remaining());
        
        READER.setReadChannel(channel(b));
        ByteBuffer in = ByteBuffer.allocate(out.length + 1);
        assertEquals("input buffer: " + b, out.length, READER.read(in));
        assertEquals(out.length, in.position());
        assertEquals(buffer(out), in.flip());
        
        // Since the deflater didn't finish the stream, the next read should return 0.
        in.limit(out.length + 1);
        assertEquals("stream shouldn't have ended", 0, READER.read(in));
    }
    
    
    private ReadableByteChannel channel(ByteBuffer buffer) throws Exception {
        return new ReadBufferChannel(buffer);
    }
    
    private ReadableByteChannel eof(ByteBuffer buffer) throws Exception {
        return new ReadBufferChannel(buffer, true);
    }
    
    private ByteBuffer deflate(byte[] data) throws Exception {
        OutputStream dos = null;
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            dos = new DeflaterOutputStream(baos);
            dos.write(data, 0, data.length);
            dos.close();                      //flushes bytes
            return buffer(baos.toByteArray());
        } finally {
            if(dos != null)
                try { dos.close(); } catch(IOException x) {}
        }
    }
    
    private ByteBuffer stream(byte[] data) throws Exception {
        OutputStream dos = null;
        Deflater def = new Deflater();
        try {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            dos = new CompressingOutputStream(baos, def);
            dos.write(data, 0, data.length);
            dos.flush();
            dos.flush();
            dos.flush();
            dos.flush();
            return buffer(baos.toByteArray());
        } finally {
            if(dos != null)
                try { dos.close(); } catch(IOException x) {}
            def.end();
        }
    }

    
    private ByteBuffer buffer(byte[] b) throws Exception {
        return ByteBuffer.wrap(b);
    }
    
    private ByteBuffer buffer(ByteBuffer[] bufs) throws Exception {
        int length = 0;
        for(int i = 0; i < bufs.length; i++)
            length += bufs[i].limit();
        ByteBuffer combined = ByteBuffer.allocate(length);
        for(int i = 0; i < bufs.length; i++)
            combined.put(bufs[i]);
        combined.flip();
        return combined;
    }
}