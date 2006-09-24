package com.limegroup.mojito.util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class OnewayExchangerTest extends BaseTestCase {
    
    public OnewayExchangerTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(OnewayExchangerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testGetTimeout() throws Exception {
        OnewayExchanger e = new OnewayExchanger();
        try {
            e.get(1, TimeUnit.MILLISECONDS);
            fail("Should have thrown a TimeoutException");
        } catch (TimeoutException ignore) {
        }
    }
    
    public void testTryGet() throws Exception {
        OnewayExchanger e = new OnewayExchanger();
        assertNull(e.tryGet());
        e.setValue("Hello World!");
        assertEquals("Hello World!", e.tryGet());
    }
    
    public void testGetValue() throws Exception {
        final OnewayExchanger e = new OnewayExchanger();
        final Object value = "Hello World";
        
        Runnable getter = new Runnable() {
            public void run() {
                try {
                    Object v = e.get(100, TimeUnit.MILLISECONDS);
                    assertEquals(value, v);
                } catch (Exception err) {
                    fail("Getting value " + value + " failed", err);
                }
            }
        };
        
        new Thread(getter, "Getter-Thread").start();
        Thread.sleep(10);
        e.setValue(value);
    }
    
    public void testSetException() throws Exception {
        final OnewayExchanger e = new OnewayExchanger();
        
        Runnable getter = new Runnable() {
            public void run() {
                try {
                    e.get(100, TimeUnit.MILLISECONDS);
                    fail("Get should have thrown an IllegalStateException");
                } catch (IllegalStateException expected) {
                    assertEquals("Hello World!", expected.getMessage());
                } catch (Exception err) {
                    fail("Get threw an unexpected Exception", err);
                }
            }
        };
        
        new Thread(getter, "Getter-Thread").start();
        Thread.sleep(10);
        e.setException(new IllegalStateException("Hello World!"));
    }
    
    public void testIsDone() throws Exception {
        final OnewayExchanger e = new OnewayExchanger();
        final Object value = "Hello World";
        
        Runnable getter = new Runnable() {
            public void run() {
                assertTrue(e.isDone());
                try {
                    Object v = e.get(100, TimeUnit.MILLISECONDS);
                    assertEquals(value, v);
                } catch (Exception err) {
                    fail("Getting value " + value + " failed", err);
                }
            }
        };
        
        e.setValue(value);
        new Thread(getter, "Getter-Thread").start();
    }
    
    public void testThrowsException() throws Exception {
        final OnewayExchanger e = new OnewayExchanger();
        
        Runnable getter = new Runnable() {
            public void run() {
                assertTrue(e.isDone());
                assertTrue(e.throwsException());
                try {
                    e.get(100, TimeUnit.MILLISECONDS);
                    fail("Get should have thrown an IllegalStateException");
                } catch (IllegalStateException expected) {
                    assertEquals("Hello World!", expected.getMessage());
                } catch (Exception err) {
                    fail("Get threw an unexpected Exception", err);
                }
            }
        };
        
        e.setException(new IllegalStateException("Hello World!"));
        new Thread(getter, "Getter-Thread").start();
    }
    
    public void testOneShot() throws Exception {
        OnewayExchanger e1 = new OnewayExchanger(true);
        e1.setValue("Hello World!");
        try {
            e1.setValue("Should fail!");
            fail("Setting value should have failed with an IllegalStateException");
        } catch (IllegalStateException ignore) {
        }
        assertEquals("Hello World!", e1.tryGet());
        
        try {
            e1.reset();
            fail("Reset should have failed");
        } catch (IllegalStateException ignore) {
        }
        
        // Same test but with Exceptions
        OnewayExchanger e2 = new OnewayExchanger(true);
        e2.setException(new IllegalArgumentException("Hello World!"));
        try {
            e2.setException(new IllegalArgumentException("Should fail!"));
            fail("Setting Exception should have failed with an IllegalStateException");
        } catch (IllegalStateException ignore) {
        }
        
        try {
            e2.tryGet();
            fail("Getting Value should have failed with an IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Hello World!", expected.getMessage());
        }
    }
    
    public void testCancel() throws Exception {
        // Cannot cancel if value or Exception are set
        OnewayExchanger e1 = new OnewayExchanger();
        e1.setValue("Hello World!");
        boolean cancelled = e1.cancel();
        assertFalse(cancelled);
        assertEquals("Hello World!", e1.tryGet());
        
        OnewayExchanger e2 = new OnewayExchanger();
        e2.setException(new IllegalArgumentException("Hello World!"));
        cancelled = e2.cancel();
        assertFalse(cancelled);
        try {
            e2.tryGet();
            fail("Getting Value should have failed with an IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("Hello World!", expected.getMessage());
        }
        
        OnewayExchanger e3 = new OnewayExchanger();
        cancelled = e3.cancel();
        assertTrue(cancelled);
        assertTrue(e3.throwsException());
        
        try {
            e3.tryGet();
            fail("Getting Value should have failed with an CancellationException");
        } catch (CancellationException expected) {
        }
    }
}
