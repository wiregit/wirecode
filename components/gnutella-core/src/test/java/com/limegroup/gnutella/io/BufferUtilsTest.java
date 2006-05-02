package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;
import java.util.Random;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

public class BufferUtilsTest extends BaseTestCase {
    
    public BufferUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BufferUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testDelete()throws Exception {
        byte[] data = data(100);
        byte[] copy = new byte[100];
        System.arraycopy(data, 0, copy, 0, 100);
        ByteBuffer buf = (ByteBuffer)ByteBuffer.wrap(copy).compact();
        
        assertEquals(100, buf.position());
        for(int i = 0; i < 100; i++)
            assertEquals("wrong data at: " + i, buf.get(i), data[i]);
        assertEquals(0, BufferUtils.delete(buf, 1));
        assertEquals(99, buf.position());
        for(int i = 0; i < 99; i++)
            assertEquals("wrong data at: " + i, buf.get(i), data[i+1]);
        
        assertEquals(51, BufferUtils.delete(buf, 150));
        assertEquals(0, buf.position());
        
        data = data(100);
        buf = (ByteBuffer)ByteBuffer.wrap(data).compact();
        assertEquals(0, BufferUtils.delete(buf, 100));
        assertEquals(0, buf.position());
    }
    
    private byte[] data(int len) {
        Random rnd = new Random();
        byte[] b = new byte[len];
        rnd.nextBytes(b);
        return b;
    }
}
