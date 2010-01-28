package org.limewire.activation.impl;

import java.util.Date;

import junit.framework.Test;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.util.BaseTestCase;

public class ActivationItemImplTest extends BaseTestCase {

    private ActivationItemFactory factory;
    
    public ActivationItemImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActivationItemImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        factory = new ActivationItemFactoryImpl(new ActivationSettingStub());
    }
    
    public void testExpiredFromServer() throws Exception {
        long time = System.currentTimeMillis();
        ActivationItem expiredDate = factory.createActivationItem(1, "license name", new Date(time - 200), new Date(time - 100), Status.ACTIVE);
        ActivationItem activeDate = factory.createActivationItem(1, "license name", new Date(time - 200), new Date(time + 10000), Status.ACTIVE);
        
        assertEquals(Status.ACTIVE, expiredDate.getStatus());
        assertEquals(Status.ACTIVE, activeDate.getStatus());
    }
    
    public void testExpiredFromDisk() throws Exception {
        long time = System.currentTimeMillis();
        ActivationItem expired = factory.createActivationItemFromDisk(1, "license name", new Date(time - 200), new Date(time - 100), Status.ACTIVE);
        ActivationItem unuseable = factory.createActivationItemFromDisk(-1, "license name", new Date(time - 200), new Date(time - 100), Status.ACTIVE);
        ActivationItem active = factory.createActivationItemFromDisk(1, "license name", new Date(time - 200), new Date(time + 10000), Status.ACTIVE);
        
        assertEquals(Status.EXPIRED, expired.getStatus());
        assertEquals(Status.UNUSEABLE_LW, unuseable.getStatus());
        assertEquals(Status.ACTIVE, active.getStatus());
    }
}
