package org.limewire.listener;

import org.limewire.util.BaseTestCase;

public class WeakEventListenerListTest extends BaseTestCase {
    
    public WeakEventListenerListTest(String name) {
        super(name);
    }

    public void testAddRemoveListener(){
        WeakEventListenerList weakList = new WeakEventListenerList();
        String ref = "testref";
        weakList.addListener(ref, null);
        
        assertTrue(weakList.removeListener(ref, null));
    }

}
