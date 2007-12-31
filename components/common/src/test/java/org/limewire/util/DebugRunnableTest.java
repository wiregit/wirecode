package org.limewire.util;

import junit.framework.Test;

public class DebugRunnableTest  extends BaseTestCase {
    
    public DebugRunnableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DebugRunnableTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testTraceNoCause() {
        Runnable r = new Runnable() {
            public void run() {
                throw new RuntimeException("parent");
            }
        };
        
        DebugRunnable debugR = new DebugRunnable(r);
        try {
            debugR.run();
            fail("should have thrown!");
        } catch(RuntimeException re) {
            assertEquals("parent", re.getMessage());
            assertEquals("Debug Exception Creation", re.getCause().getMessage());
        }
        
    }
    
    public void testTraceWithCause() {
        Runnable r = new Runnable() {
            public void run() {
                throw new RuntimeException("parent", new RuntimeException("cause"));
            }
        };
        
        DebugRunnable debugR = new DebugRunnable(r);
        try {
            debugR.run();
            fail("should have thrown!");
        } catch(RuntimeException re) {
            assertEquals("parent", re.getMessage());
            assertEquals("cause", re.getCause().getMessage());
        }
    }
}
