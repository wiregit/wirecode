package com.limegroup.gnutella.altlocs;

import java.util.Collection;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.util.BaseTestCase;

public class AltLocManagerTest extends BaseTestCase {

    public AltLocManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AltLocManagerTest.class);
    }
    
    private final AltLocManager manager = AltLocManager.instance();

    public void setUp() {
        manager.purge();
    }
    
    public void testStorage() throws Exception {
        AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",HugeTestUtils.SHA1);
        GUID g = new GUID(GUID.makeGuid());
        GUID g2 = new GUID(GUID.makeGuid());
        AlternateLocation push = AlternateLocation.create(g.toHexString()+";1.1.1.1:1",HugeTestUtils.SHA1);
        AlternateLocation pushFWT = AlternateLocation.create(g2.toHexString()+";fwt/1.0;2:2.2.2.2;3.3.3.3:3",HugeTestUtils.SHA1);
        
        manager.add(direct, null);
        manager.add(push, null);
        manager.add(pushFWT, null);
        
        AlternateLocationCollection c = manager.getDirect(HugeTestUtils.SHA1);
        assertEquals(1,c.getAltLocsSize());
        assertTrue(c.contains(direct));
        
        c = manager.getPush(HugeTestUtils.SHA1, false);
        assertEquals(1,c.getAltLocsSize());
        assertTrue(c.contains(push));
        
        
        c = manager.getPush(HugeTestUtils.SHA1, true);
        assertEquals(1,c.getAltLocsSize());
        assertTrue(c.contains(pushFWT));
        
        manager.purge();
        assertEquals(AlternateLocationCollection.EMPTY,manager.getDirect(HugeTestUtils.SHA1));
        assertEquals(AlternateLocationCollection.EMPTY,manager.getPush(HugeTestUtils.SHA1, false));
        assertEquals(AlternateLocationCollection.EMPTY,manager.getPush(HugeTestUtils.SHA1, true));
    }
    
    
    public void testPromotionDemotion() throws Exception {
        AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",HugeTestUtils.SHA1);
        manager.add(direct, null);
        manager.remove(direct, null);
        AlternateLocationCollection c = manager.getDirect(HugeTestUtils.SHA1);
        assertTrue(c.contains(direct));
        assertTrue(direct.isDemoted());
        
        manager.add(direct, null);
        assertFalse(direct.isDemoted());
        
        manager.remove(direct, null);
        manager.remove(direct, null);
        assertEquals(AlternateLocationCollection.EMPTY,manager.getDirect(HugeTestUtils.SHA1));
    }
    
    public void testNotification() throws Exception {
        // test that a registered listener receives notification of an altloc
        Listener l = new Listener();
        AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",HugeTestUtils.SHA1);
        manager.addListener(HugeTestUtils.SHA1,l);
        manager.add(direct, null);
        assertEquals(direct,l.loc);
        
        // test that a listener does not receive notification that comes from itself
        l.loc = null;
        manager.remove(direct, null);manager.remove(direct, null);
        assertFalse(manager.hasAltlocs(HugeTestUtils.SHA1));
        manager.add(direct,l);
        assertNull(l.loc);
    }
    
    private static class Listener implements AltLocListener {
        public AlternateLocation loc;
        public void locationAdded(AlternateLocation loc) {
            this.loc = loc;
        }
    }
}
