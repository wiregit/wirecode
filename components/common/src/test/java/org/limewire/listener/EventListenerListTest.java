package org.limewire.listener;

import java.util.concurrent.CountDownLatch;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * A class to test the methods of EventListenerList: adding, removing.
 * Need to add tests for multiple adds, adds to existing lists, adding different 
 * types of listeners, as well as broadcasting.
 */
public class EventListenerListTest extends BaseTestCase {
    
    public EventListenerListTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(EventListenerListTest.class);
    }
    
    /**
     * A simple test to add a listener (for which the key did not exist before),
     * and to remove it after
     */
    public void testAddRemoveListener() {
        EventListenerList<Object> list = new EventListenerList<Object>();
        Listener l = new Listener();
        list.addListener(l);
        assertTrue(list.removeListener(l));
        assertFalse(list.removeListener(l));
    }
    
    public void testInlineEvent() {
        EventListenerList<Object> list = new EventListenerList<Object>();
        Listener l = new Listener();
        list.addListener(l);
        assertNull(l.thread);
        l.handleEvent(new Object());
        assertEquals(Thread.currentThread(), l.thread);   
    }
    
    public void testSwingProxyEvent() {
        EventListenerList<Foo> list = new EventListenerList<Foo>();
        SwingProxyListener l = new SwingProxyListener();
        list.addListener(l);
        assertNull(l.thread);
        l.handleEvent(new Foo());
        assertEquals(Thread.currentThread(), l.thread);
    }
    
    public void testSwingProxyEventWithSubclassedEvent() {
        EventListenerList<Foo> list = new EventListenerList<Foo>();
        SwingProxyListener l = new SwingProxyListener();
        list.addListener(l);
        assertNull(l.thread);
        l.handleEvent(new SubFoo());
        assertEquals(Thread.currentThread(), l.thread);
    }
    
    private static class Listener implements EventListener<Object> {
        private Thread thread; 
        public void handleEvent(Object event) {
            this.thread = Thread.currentThread();
        }
    }
    
    private static class SwingProxyListener implements EventListener<Foo> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile Thread thread;
        
        @Override
        public void handleEvent(Foo event) {
            this.thread = Thread.currentThread();
            latch.countDown();
        }
    }
    
    private static class Foo {}
    private static class SubFoo extends Foo {}
    

}
