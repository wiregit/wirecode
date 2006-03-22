package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

public class ByteBufferCacheTest extends BaseTestCase {
    
    public ByteBufferCacheTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ByteBufferCacheTest.class);
    }
    
    static ByteBufferCache CACHE;
    
    public void setUp() {
        CACHE = new ByteBufferCache();
    }
    /**
     * Tests that requesting a buffer, releasing it and then requesting
     * a buffer of the same size again will return the same object.
     */
    public void testGetSameBuffer() throws Exception {
        ByteBuffer buf = CACHE.getHeap(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        buf = CACHE.getHeap(100);
        assertEquals(hashCode, System.identityHashCode(buf));
    }
    
    /**
     * Tests that requesting two buffers w/o releasing will return two
     * different objects. 
     */
    public void testGetNoRelease() throws Exception {
        ByteBuffer buf = CACHE.getHeap(100);
        int hashCode = System.identityHashCode(buf);
        buf = CACHE.getHeap(100);
        assertNotEquals(hashCode, System.identityHashCode(buf));
    }
    
    
    /**
     * Tests that requesting a larger buffer if a smaller one is
     * available will create a new object.
     */
    public void testNoSlicingIfLarger() throws Exception {
        ByteBuffer buf = CACHE.getHeap(100);
        buf = CACHE.getHeap(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        ByteBuffer larger = CACHE.getHeap(200);
        assertNotEquals(hashCode, System.identityHashCode(larger));
        
        // we haven't returned the larger buffer, and requesting a buffer
        // of the smaller size will return the same object we had the first
        // time around.
        buf = CACHE.getHeap(100);
        assertEquals(hashCode, System.identityHashCode(buf));
    }
    
    /**
     * Tests that requesting a smaller buffer if a larger one is
     * available will not create a new object.
     */
    public void testSlicingIfSmaller() throws Exception {
        ByteBuffer buf = CACHE.getHeap(100);
        buf = CACHE.getHeap(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        ByteBuffer smaller = CACHE.getHeap(50);
        assertNotEquals(hashCode, System.identityHashCode(smaller));
        
        // we haven't returned the smaller buffer, and requesting a buffer
        // of the larger size will not return the same object we had the first
        // time around.
        buf = CACHE.getHeap(100);
        assertNotEquals(hashCode, System.identityHashCode(buf));
        
        // after returning the smaller buffer, the next request for a buffer
        // the original size will return the same object.
        CACHE.release(smaller);
        buf = CACHE.getHeap(100);
        assertEquals(hashCode, System.identityHashCode(buf));
    }
    
    /**
     * Test that clearing the cache will purge any cached buffers.
     */
    public void testClearing() throws Exception {
        ByteBuffer buf = CACHE.getHeap(100);
        buf = CACHE.getHeap(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        buf = CACHE.getHeap(100);
        assertEquals(hashCode, System.identityHashCode(buf));
        CACHE.release(buf);
        
        CACHE.clearCache();
        buf = CACHE.getHeap(100);
        assertNotEquals(hashCode, System.identityHashCode(buf));
        
    }
    
    /**
     * Tests that if the cache is cleared while a sliced buffer is
     * checked out, upon return it will restore its full size.
     */
    public void testSlicedSurviveClearing() throws Exception {
        ByteBuffer buf = CACHE.getHeap(100);
        int hashCode = System.identityHashCode(buf);
        CACHE.release(buf);
        ByteBuffer smaller = CACHE.getHeap(50);
        CACHE.clearCache(); // identical to testSlicingIfSmaller except this call.
        CACHE.release(smaller);
        buf = CACHE.getHeap(100);
        assertEquals(hashCode, System.identityHashCode(buf));
    }
}
