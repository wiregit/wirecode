package com.limegroup.mojito.db.impl;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
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
        
    }

}
