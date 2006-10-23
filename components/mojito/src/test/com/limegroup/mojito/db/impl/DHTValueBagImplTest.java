package com.limegroup.mojito.db.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValueFactory;
import com.limegroup.mojito.db.DHTValue.ValueType;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.ContactFactory;
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
        assertGreaterThan(DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.getValue(), 
                load);
        Thread.sleep(500);
        assertGreaterThan(load, bag.incrementRequestLoad());
        
        //test a 0 smoothing factor
        DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.setValue(0);
        bag = new DHTValueBagImpl(KUID.createRandomID());
        Thread.sleep(500);
        assertEquals(0F, bag.incrementRequestLoad());
        
        //try a delay larger than nulling time
        bag = new DHTValueBagImpl(KUID.createRandomID());
        long now = System.currentTimeMillis();
        PrivilegedAccessor.setValue(bag, "lastRequestTime", 
                now - DatabaseSettings.VALUE_REQUEST_LOAD_NULLING_DELAY.getValue()*1000L);
        Thread.sleep(500);
        load = bag.incrementRequestLoad();
        //load should be 0
        assertEquals(0, (int)load);
        
        //try a very very large delay
        bag = new DHTValueBagImpl(KUID.createRandomID());
        PrivilegedAccessor.setValue(bag, "lastRequestTime", 1);
        load = bag.incrementRequestLoad();
        //load should never get < 0
        assertGreaterThanOrEquals(0f, load);
        Thread.sleep(200);
        bag.incrementRequestLoad();
        Thread.sleep(200);
        bag.incrementRequestLoad();
        Thread.sleep(200);
        bag.incrementRequestLoad();
        Thread.sleep(200);
        load = bag.incrementRequestLoad();
        //should now have increased (a lot!)
        assertGreaterThan(1f, load);
    }
    
    public void testAddInvalid() throws Exception {
    	KUID valueId = KUID.createRandomID();
    	DHTValueBagImpl bag = new DHTValueBagImpl(valueId);
    	
    	SocketAddress addr = new InetSocketAddress(6666);
        Contact orig = ContactFactory.createLiveContact(addr, 0, 0, 
        		KUID.createRandomID(), addr, 0, Contact.DEFAULT_FLAG);
        Contact sender = ContactFactory.createLiveContact(addr, 0, 0, 
        		KUID.createRandomID(), addr, 0, Contact.DEFAULT_FLAG);   
        
        try {
        	bag.add(DHTValueFactory.createRemoteValue(orig, sender, 
        			KUID.createRandomID(), ValueType.BINARY, "test".getBytes()));
        	assertTrue("Should have thrown an exception: wrong KUID", false);
        } catch(IllegalArgumentException ex) {}
        
        bag.add(DHTValueFactory.createRemoteValue(orig, sender, 
    			valueId, ValueType.BINARY, "test".getBytes()));
        assertEquals(1, bag.size());
        assertFalse(bag.isEmpty());
    }

}
