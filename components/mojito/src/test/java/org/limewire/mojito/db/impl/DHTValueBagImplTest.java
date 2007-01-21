package org.limewire.mojito.db.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.util.PrivilegedAccessor;


public class DHTValueBagImplTest extends MojitoTestCase {

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
        //now try a very very small delay
        bag.incrementRequestLoad();
        Thread.sleep(10);
        bag.incrementRequestLoad();
        Thread.sleep(10);
        bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        //should now have increased (a lot!)
        assertGreaterThan(1f, load);
        //but never be larger than 1/0.01
        assertLessThan(1f/0.01f, load);
    }
    
    public void testAddInvalid() throws Exception {
    	KUID valueId = KUID.createRandomID();
    	DHTValueBagImpl bag = new DHTValueBagImpl(valueId);
    	
    	SocketAddress addr = new InetSocketAddress(6666);
        Contact creator = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.UNKNOWN, 
        		KUID.createRandomID(), addr, 0, Contact.DEFAULT_FLAG);
        Contact sender = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.UNKNOWN, 
        		KUID.createRandomID(), addr, 0, Contact.DEFAULT_FLAG);   
        
        try {
            bag.add(new DHTValueEntity(creator, sender, KUID.createRandomID(), 
                        new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, "test".getBytes()), false));
        	assertTrue("Should have thrown an exception: wrong KUID", false);
        } catch(IllegalArgumentException ex) {}
        
        bag.add(new DHTValueEntity(creator, sender, valueId, 
                    new DHTValueImpl(DHTValueType.TEST, Version.UNKNOWN, "test".getBytes()), false));
        
        assertEquals(1, bag.size());
        assertFalse(bag.isEmpty());
    }

}
