package com.limegroup.gnutella.dht.db;

import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.util.UnitTestUtils;

import com.limegroup.gnutella.dht.DHTManager;

public class AltLocFinderTest extends MojitoTestCase {

    private Mockery context;
    private MojitoDHT mojitoDHT;
    private List<MojitoDHT> dhts;

    public AltLocFinderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AltLocFinderTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        final DHTManager dhtManager = context.mock(DHTManager.class);
        dhts = UnitTestUtils.createBootStrappedDHTs(1);
        mojitoDHT = dhts.get(0);
        context.checking(new Expectations() {{
            allowing(dhtManager).getMojitoDHT();
            will(returnValue(mojitoDHT));
        }});
        assertTrue(dhtManager.getMojitoDHT().isBootstrapped());
    }
    
    @Override
    protected void tearDown() throws Exception {
        for (MojitoDHT dht : dhts) {
            dht.close();
        }
    }

    public void testAltLocListenerIsNotifiedOfLocations() {
        
    }
}
