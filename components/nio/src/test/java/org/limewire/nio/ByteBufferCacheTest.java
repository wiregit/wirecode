package org.limewire.nio;

import java.nio.ByteBuffer;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class ByteBufferCacheTest extends BaseTestCase {
    
    public ByteBufferCacheTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ByteBufferCacheTest.class);
    }
    
    private ByteBufferCache CACHE;
    
    @Override
    public void setUp() {
        CACHE = new ByteBufferCacheImpl();
    }
    /**
     * Tests that requesting a buffer, releasing it and then requesting
     * a buffer of the same size again will return the same object.
     */
    public void testGetSameBuffer() throws Exception {
        ByteBuffer buf = CACHE.get(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        assertEquals(100, CACHE.getCacheSize());
        buf = CACHE.get(100);
        assertEquals(hashCode, System.identityHashCode(buf));
        assertEquals(0, CACHE.getCacheSize());
    }
    
    /**
     * Tests that requesting two buffers w/o releasing will return two
     * different objects. 
     */
    public void testGetNoRelease() throws Exception {
        ByteBuffer buf = CACHE.get(100);
        int hashCode = System.identityHashCode(buf);
        buf = CACHE.get(100);
        assertNotEquals(hashCode, System.identityHashCode(buf));
        assertEquals(0, CACHE.getCacheSize());
    }
    
    
    /**
     * Tests that requesting a larger buffer if a smaller one is
     * available will create a new object.
     */
    public void testNoSlicingIfLarger() throws Exception {
        ByteBuffer buf = CACHE.get(100);
        buf = CACHE.get(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        assertEquals(100, CACHE.getCacheSize());
        ByteBuffer larger = CACHE.get(200);
        assertEquals(100, CACHE.getCacheSize());
        assertNotEquals(hashCode, System.identityHashCode(larger));
        
        // we haven't returned the larger buffer, and requesting a buffer
        // of the equal size will return the same object we had the first
        // time around.
        buf = CACHE.get(100);
        assertEquals(0, CACHE.getCacheSize());
        assertEquals(hashCode, System.identityHashCode(buf));
    }
    
    /**
     * Tests that requesting a smaller buffer if a larger one is
     * available will create a new object.
     */
    public void testNoSlicingIfSmaller() throws Exception {
        ByteBuffer buf = CACHE.get(100);
        buf = CACHE.get(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        assertEquals(100, CACHE.getCacheSize());
        ByteBuffer smaller = CACHE.get(50);
        assertEquals(100, CACHE.getCacheSize());
        assertNotEquals(hashCode, System.identityHashCode(smaller));
        
        // smaller did not use up the larger size.
        buf = CACHE.get(100);
        assertEquals(0, CACHE.getCacheSize());
        assertEquals(hashCode, System.identityHashCode(buf));
    }
    
    /**
     * Test that clearing the cache will purge any cached buffers.
     */
    public void testClearing() throws Exception {
        ByteBuffer buf = CACHE.get(100);
        buf = CACHE.get(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        assertEquals(100, CACHE.getCacheSize());
        buf = CACHE.get(100);
        assertEquals(0, CACHE.getCacheSize());
        assertEquals(hashCode, System.identityHashCode(buf));
        CACHE.release(buf);
        assertEquals(100, CACHE.getCacheSize());
        
        CACHE.clearCache();
        assertEquals(0, CACHE.getCacheSize());
        buf = CACHE.get(100);
        assertNotEquals(hashCode, System.identityHashCode(buf));
    }
}
