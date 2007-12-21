package org.limewire.listener;

import org.limewire.util.BaseTestCase;

/**
 * A class to test the methods of WeakEventListenerList: adding, removing.
 * Need to add tests for multiple adds, adds to existing lists, adding different 
 * types of listeners, as well as broadcasting.
 */
public class WeakEventListenerListTest extends BaseTestCase {
    
    public WeakEventListenerListTest(String name) {
        super(name);
    }

    /**
     * A simple test to add a listener (for which the key did not exist before),
     * and to remove it after
     */
    public void testAddRemoveListener(){
        WeakEventListenerList weakList = new WeakEventListenerList();
        String ref = "testref";
        weakList.addListener(ref, null);
        
        assertTrue(weakList.removeListener(ref, null));
    }

}
