package org.limewire.listener;

import junit.framework.TestCase;

public class WeakEventListenerListTest extends TestCase {
    
    public void testAddRemoveListener(){
        WeakEventListenerList weakList = new WeakEventListenerList();
        String ref = "testref";
        weakList.addListener(ref, null);
        
        assertTrue(weakList.removeListener(ref, null));
    }

}
