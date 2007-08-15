package com.limegroup.gnutella.downloader;

import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * tests the selection of source ranker that is appropriate to the system.
 */
public class SourceRankerSelectionTest extends LimeTestCase {

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
        
        // if headpings are disabled, use legacy
        DownloadSettings.USE_HEADPINGS.setValue(false);
        assertTrue(SourceRanker.getAppropriateRanker() instanceof LegacyRanker);
        
        // and vice versa
        DownloadSettings.USE_HEADPINGS.setValue(true);
        assertTrue(SourceRanker.getAppropriateRanker() instanceof PingRanker);
    }
    
    public void testUpdateRanker() throws Exception {
        // LegacyRanker -> PingRanker
        RouterService.acceptedIncomingConnection();
        PrivilegedAccessor.setValue(UDPService.instance(),"_acceptedSolicitedIncoming",Boolean.FALSE);
        assertFalse(RouterService.canReceiveSolicited());
        SourceRanker ranker = SourceRanker.getAppropriateRanker();
        
        assertTrue(ranker instanceof LegacyRanker);
        
        // now we accept some solicited
        PrivilegedAccessor.setValue(UDPService.instance(),"_acceptedSolicitedIncoming",Boolean.TRUE);
        
        ranker = SourceRanker.getAppropriateRanker(ranker);
        
        assertTrue(ranker instanceof PingRanker);
        
        // now we try again
        SourceRanker ranker2 = SourceRanker.getAppropriateRanker(ranker);
        
        // it should be the same object
        assertTrue(ranker == ranker2);
    }

}
