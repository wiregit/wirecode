package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * tests the selection of source ranker that is appropriate to the system.
 */
public class SourceRankerSelectionTest extends BaseTestCase {

    public SourceRankerSelectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SourceRankerSelectionTest.class);
    }
    
    
    public void testSelectRanker() throws Exception {
        // if we cannot do solicited udp, we get the legacy ranker
        RouterService.acceptedIncomingConnection();
        PrivilegedAccessor.setValue(UDPService.instance(),"_acceptedSolicitedIncoming",Boolean.FALSE);
        assertFalse(RouterService.canReceiveSolicited());
        SourceRanker ranker = SourceRanker.getAppropriateRanker();
        
        assertTrue(ranker instanceof LegacyRanker);
        
        // if we can, use the PingRanker
        PrivilegedAccessor.setValue(UDPService.instance(),"_acceptedSolicitedIncoming",Boolean.TRUE);
        
        ranker = SourceRanker.getAppropriateRanker();
        
        assertTrue(ranker instanceof PingRanker);
    }

}
