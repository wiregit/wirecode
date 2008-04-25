package org.limewire.listener;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * A class to test the methods of EventListenerList: adding, removing.
 * Need to add tests for multiple adds, adds to existing lists, adding different 
 * types of listeners, as well as broadcasting.
 */
public class EventListenerListTest extends BaseTestCase {
    
    private EventListenerList<Object> list;

    public EventListenerListTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(EventListenerListTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        list = new EventListenerList<Object>();
    }

    /**
     * A simple test to add a listener (for which the key did not exist before),
     * and to remove it after
     */
    public void testAddRemoveListener() {
        Listener l = new Listener();
        list.addListener(l);
        assertTrue(list.removeListener(l));
        assertFalse(list.removeListener(l));
    }
    
    private static class Listener implements EventListener<Object> {
        public void handleEvent(Object event) {
        }
    }

}
