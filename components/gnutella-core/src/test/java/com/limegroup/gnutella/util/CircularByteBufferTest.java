package com.limegroup.gnutella.util;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import junit.framework.Test;

import com.limegroup.gnutella.connection.ReadBufferChannel;
import com.limegroup.gnutella.connection.WriteBufferChannel;

/**
 * Tests some of the functionality of the Circular buffer.  
 *
 */
public class CircularByteBufferTest extends BaseTestCase {

    public CircularByteBufferTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CircularByteBufferTest.class);
    }
    
    public void testBasic() throws Exception {
        byte [] in = new byte[]{(byte)1,(byte)2};
        CircularByteBuffer buf = new CircularByteBuffer(2,false);
        assertEquals(2,buf.remainingIn());
        assertEquals(0,buf.remainingOut());
        
        buf.put(ByteBuffer.wrap(in));
        
        assertEquals(0,buf.remainingIn());
        assertEquals(2,buf.remainingOut());
        
        byte [] out = new byte[2];
        buf.get(out);
        
        assertEquals(2,buf.remainingIn());
        assertEquals(0,buf.remainingOut());

        assertEquals(in[0],out[0]);
        assertEquals(in[1],out[1]);
    }
    
    public void testOverflow() throws Exception {
        byte [] in = new byte[]{(byte)1,(byte)2};
        CircularByteBuffer buf = new CircularByteBuffer(1,false);
        
        assertEquals(1,buf.remainingIn());
        assertEquals(0,buf.remainingOut());
        
        try {
            buf.put(ByteBuffer.wrap(in));
            fail(" should have overflown");
        } catch (BufferOverflowException expected) {}
        
        assertEquals(1,buf.remainingIn());
        assertEquals(0,buf.remainingOut());
    }
    
    public void testUnderflow() throws Exception {
        byte [] in = new byte[]{(byte)1,(byte)2};
        CircularByteBuffer buf = new CircularByteBuffer(2,false);
        
        buf.put(ByteBuffer.wrap(in));
        
        assertEquals(0,buf.remainingIn());
        assertEquals(2,buf.remainingOut());
        
        byte [] out = new byte[3];
        
        try {
            buf.get(out);
            fail("should have underflown");
        } catch (BufferUnderflowException expected) {}
        
        assertEquals(0,buf.remainingIn());
        assertEquals(2,buf.remainingOut());
    }
    
    public void testWrapAround() throws Exception {
        byte [] in = new byte[30];
        for(int i = 0;i < 20;i++) in[i]=(byte)i;
        
        CircularByteBuffer buf = new CircularByteBuffer(5,false);
        ReadableByteChannel source = new ReadBufferChannel(in);
        
        // R1 R2 R3 R4 R5
        assertEquals(5,buf.read(source));
        
        assertEquals(0,buf.remainingIn());
        assertEquals(5,buf.remainingOut());
        
        // R1 R2 - - -
        buf.get();buf.get();
        assertEquals(2,buf.remainingIn());
        assertEquals(3,buf.remainingOut());
        assertEquals(2,buf.read(source));
        
        assertEquals(0,buf.remainingIn());
        assertEquals(5,buf.remainingOut());
        
        // W1 W2 W3 W4 W5
        WritableByteChannel sink = new WriteBufferChannel(20);
        assertEquals(5,buf.write(sink));
        assertEquals(5,buf.read(source));
        
        // - - W1 W2 W3
        buf.get();buf.get();
        assertEquals(3,buf.write(sink));
        assertEquals(5,buf.read(source));
        
        // W5 W1 W2 W3 W4
        buf.get();
        assertEquals(1,buf.read(source));
        assertEquals(5,buf.write(sink));
        
        // R5 R1 R2 R3 R4
        assertEquals(5,buf.read(source));
    }
    
}
