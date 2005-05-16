package com.limegroup.gnutella.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;

import junit.framework.Test;

import com.limegroup.gnutella.util.*;

/**
 * Tests that Throttle does its thing.
 */
public final class NBThrottleTest extends BaseTestCase {
    
    private Data[] DATA = new Data[50];
    private int RATE = 3 * 1024; // 3KB/s
    private NBThrottle THROTTLE = new NBThrottle(true, RATE);

	public NBThrottleTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(NBThrottleTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void setUp() {
	    for(int i = 0; i < DATA.length; i++)
	        DATA[i] = new Data();
	}
	
	public void testInterestGivesBandwidthOnTick() {
	    // Control.
	    for(int i = 0; i < DATA.length; i++)
	        assertFalse(DATA[i].STUB.available());
	        
        // Make sure a tick with nothing interested sets nothing.
        THROTTLE.tick(0);
	    for(int i = 0; i < DATA.length; i++)
	        assertFalse(DATA[i].STUB.available());
	        
        // Set interest on some things, tick & check.
	    for(int i = 0; i < DATA.length; i++) {
	        if(i % 3 == 0)
	            THROTTLE.interest(DATA[i].STUB, DATA[i].ATTACHMENT);
	    }
	    THROTTLE.tick(1000);
	    for(int i = 0; i < DATA.length; i++)
	        assertEquals(i%3==0, DATA[i].STUB.available());
	        
        // When requesting interest again, people who already were interested
        // should not be given bandwidth again, but new people should.
	    for(int i = 0; i < DATA.length; i++) {
	        DATA[i].STUB.clear();
	        if(i % 2 == 0)
	            THROTTLE.interest(DATA[i].STUB, DATA[i].ATTACHMENT);
        }
        THROTTLE.tick(2000);
	    for(int i = 0; i < DATA.length; i++)
	        assertEquals(i%2==0 && i%3!=0, DATA[i].STUB.available());
	        
        // If we tick within an interval & there's stuff still available,
        // it'll set interest.
        for(int i = 0; i < DATA.length; i++) {
	        DATA[i].STUB.clear();
	        if(i % 5 == 0)
	            THROTTLE.interest(DATA[i].STUB, DATA[i].ATTACHMENT);            
        }
        THROTTLE.tick(2001);
	    for(int i = 0; i < DATA.length; i++)
	        assertEquals(i%5==0 && i%2!=0 && i%3!=0, DATA[i].STUB.available());
	        
        // However, if we request all the data and tick, then nothing should
        // be set on that tick if it's before the interval.
        THROTTLE.selectableKeys(set(DATA[5].KEY));
        THROTTLE.request(DATA[5].STUB, DATA[5].ATTACHMENT);
        
        for(int i = 0; i < DATA.length; i++) {
	        DATA[i].STUB.clear();
	        THROTTLE.interest(DATA[i].STUB, DATA[i].ATTACHMENT);
        }
        
        THROTTLE.tick(2002);
	    for(int i = 0; i < DATA.length; i++)
	        assertFalse(DATA[i].STUB.available());        
    }
    
    private Set set(Object o) {
        Set set = new HashSet();
        set.add(o);
        return set;
    }
	
	
	private static class Data {
        private Object ATTACHMENT = new Object();
        private StubThrottleListener STUB = new StubThrottleListener(ATTACHMENT);
        private FakeSelectionKey KEY = new FakeSelectionKey(ATTACHMENT);
    }
}