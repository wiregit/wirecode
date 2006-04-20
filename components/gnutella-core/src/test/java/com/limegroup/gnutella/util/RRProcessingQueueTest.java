package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;

public class RRProcessingQueueTest extends BaseTestCase {
    public RRProcessingQueueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RRProcessingQueueTest.class);
    } 
    
    public void testRRProcessing() throws Exception {
    	RRProcessingQueue queue = new RRProcessingQueue("test");
    	List order = Collections.synchronizedList(new ArrayList());
    	Runner a1 = new Runner(order);
    	Runner a2 = new Runner(order);
    	Runner b1 = new Runner(order);
    	Runner b2 = new Runner(order);
    	Runner c1 = new Runner(order);
    	
    	queue.invokeLater(a1, "a");
    	queue.invokeLater(a2, "a");
    	queue.invokeLater(b1, "b");
    	queue.invokeLater(b2, "b");
    	queue.invokeLater(c1, "c");
    	assertEquals(5, queue.size());
    	
    	try {
    		Thread.sleep(1000);
    	} catch (InterruptedException ignored) {}
    	
    	// order of execution should be:
    	// a1, b1, c1, a2, b2
    	assertEquals(a1,order.get(0));
    	assertEquals(b1,order.get(1));
    	assertEquals(c1,order.get(2));
    	assertEquals(a2,order.get(3));
    	assertEquals(b2,order.get(4));
    	assertEquals(0, queue.size());
    }
    
    private class Runner implements Runnable {
    	final List toNotify;
    	Runner(List toNotify) {
    		this.toNotify = toNotify;
    	}
    	public void run() {
    		try {
    		Thread.sleep(150);
    		} catch (InterruptedException iex){
    			fail(iex);
    		}
    		toNotify.add(this);
    	}
    }
}
