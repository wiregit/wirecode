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
        
        manager.add(direct);
        manager.add(push);
        manager.add(pushFWT);
        
        Collection c = manager.getDirect(HugeTestUtils.SHA1,2);
        assertEquals(1,c.size());
        assertTrue(c.contains(direct));
        
        c = manager.getPush(HugeTestUtils.SHA1, 2, false);
        assertEquals(2,c.size());
        assertTrue(c.contains(push));
        assertTrue(c.contains(pushFWT));
        
        c = manager.getPush(HugeTestUtils.SHA1, 1, false);
        assertEquals(1,c.size());
        assertTrue(c.contains(push) || c.contains(pushFWT));
        
        c = manager.getPush(HugeTestUtils.SHA1, 2, true);
        assertEquals(1,c.size());
        assertTrue(c.contains(pushFWT));
        
        Collection [] col = manager.getBoth(HugeTestUtils.SHA1, 2, false);
        assertEquals(1,col[0].size());
        assertTrue(col[0].contains(direct));
        assertEquals(2,col[1].size());
        assertTrue(col[1].contains(push));
        assertTrue(col[1].contains(pushFWT));
        
        col = manager.getBoth(HugeTestUtils.SHA1, 2, true);
        assertEquals(1,col[0].size());
        assertTrue(col[0].contains(direct));
        assertEquals(1,col[1].size());
        assertTrue(col[1].contains(pushFWT));
        
        manager.purge();
        assertNull(manager.getBoth(HugeTestUtils.SHA1, 2, true));
        assertNull(manager.getBoth(HugeTestUtils.SHA1, 2, false));
        assertNull(manager.getDirect(HugeTestUtils.SHA1, 2));
        assertNull(manager.getPush(HugeTestUtils.SHA1, 2, false));
        assertNull(manager.getPush(HugeTestUtils.SHA1, 2, true));
    }
    
    
    public void testPromotionDemotion() throws Exception {
        AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",HugeTestUtils.SHA1);
        manager.add(direct);
        manager.remove(direct);
        Collection c = manager.getDirect(HugeTestUtils.SHA1, 2);
        assertTrue(c.contains(direct));
        assertTrue(direct.isDemoted());
        
        manager.add(direct);
        assertFalse(direct.isDemoted());
        
        manager.remove(direct);
        manager.remove(direct);
        assertNull(manager.getDirect(HugeTestUtils.SHA1, 2));
    }
    
}
