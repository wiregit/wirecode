package com.limegroup.mojito.db.impl;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.settings.DatabaseSettings;

public class DHTValueBagImplTest extends BaseTestCase {

    public DHTValueBagImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTValueBagImplTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testIncrementRequestLoad() throws Exception{
        DHTValueBagImpl bag = new DHTValueBagImpl(KUID.createRandomID());
        
        assertEquals(bag.incrementRequestLoad() , 0f);
        //should start with larger than smoothing factor
        Thread.sleep(500);
        float load = bag.incrementRequestLoad();
        assertGreaterThan(DatabaseSettings.VALUE_LOAD_SMOOTHING_FACTOR.getValue(), 
                load);
        Thread.sleep(500);
        assertGreaterThan(load, bag.incrementRequestLoad());
        
        //test a 0 smoothing factor
        DatabaseSettings.VALUE_LOAD_SMOOTHING_FACTOR.setValue(0);
        bag = new DHTValueBagImpl(KUID.createRandomID());
        Thread.sleep(500);
        assertEquals(0F, bag.incrementRequestLoad());
        
        //try a very very large delay
        bag = new DHTValueBagImpl(KUID.createRandomID());
        PrivilegedAccessor.setValue(bag, "lastRequestTime", 1);
        load = bag.incrementRequestLoad();
        //load should never get < 0
        assertGreaterThan(0f, load);
        Thread.sleep(200);
        bag.incrementRequestLoad();
        Thread.sleep(200);
        load = bag.incrementRequestLoad();
        //should now have increased (a lot!)
        assertGreaterThan(1f, load);
    }

}
