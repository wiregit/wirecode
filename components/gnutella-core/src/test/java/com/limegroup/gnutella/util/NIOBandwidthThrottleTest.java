package com.limegroup.gnutella.util;

import junit.framework.Test;

/**
 * Tests the class that performs non-blocking bandwidth throttling.
 */
public final class NIOBandwidthThrottleTest extends BaseTestCase {

    public NIOBandwidthThrottleTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NIOBandwidthThrottleTest.class);
    }  
    
    /**
     * General test to make sure that throttling is working as expected.
     * 
     * @throws Exception if anything goes wrong
     */
    public void testThrottling() throws Exception {
        int bytes = 100000;
        NIOBandwidthThrottle throttle = 
            NIOBandwidthThrottle.createThrottle(bytes);
        assertEquals("unexpected number of bytes available", bytes/10,
            throttle.bytesAvailable());
        
        // make sure the expected number of bytes are available for writing
        // after each incremental write
        long now = System.currentTimeMillis();
        int increment = 100;
        for(int i=0; i<10; i++) {
            if(System.currentTimeMillis()-now >= 100) {
                break;
            }
            assertEquals("unexpected number of bytes available", 
                (bytes/10)-i*increment, throttle.bytesAvailable());  
            throttle.addBytesWritten(increment);  
        }
        if(System.currentTimeMillis()-now < 1000) {
            Thread.sleep(1000 - (System.currentTimeMillis()-now));
        }
        
        // all bytes should be available again
        assertEquals("unexpected number of bytes available", bytes/10,
            throttle.bytesAvailable());
        
        
        // make sure there are no bytes available after writing to capacity    
        throttle.addBytesWritten(bytes/10);
        assertEquals("unexpected number of bytes available", 0,
            throttle.bytesAvailable());
        
        // make sure there's room yet again after waiting    
        Thread.sleep(bytes/10);
        assertEquals("unexpected number of bytes available", bytes/10,
            throttle.bytesAvailable());       
        
        // now make sure that IllegalArgumentException is thrown if we try to
        // write too many bytes     
        try {
            throttle.addBytesWritten(bytes*2);
            fail("should have throw illegal argument for trying to add too"+
                "many bytes");
        } catch(IllegalArgumentException e) {
            // expected
        }
    }
}
