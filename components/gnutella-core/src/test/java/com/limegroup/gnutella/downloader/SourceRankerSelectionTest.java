package com.limegroup.gnutella.downloader;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ProviderHacks;
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
//        
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_acceptedSolicitedIncoming",Boolean.FALSE);
        assertFalse(ProviderHacks.getNetworkManager().canReceiveSolicited());
        SourceRanker ranker = ProviderHacks.getSourceRankerFactory().getAppropriateRanker();
        
        assertTrue(ranker instanceof LegacyRanker);
        
        // if we can, use the PingRanker
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_acceptedSolicitedIncoming",Boolean.TRUE);
        
        ranker = ProviderHacks.getSourceRankerFactory().getAppropriateRanker();
        
        assertTrue(ranker instanceof PingRanker);
        
        // if headpings are disabled, use legacy
        DownloadSettings.USE_HEADPINGS.setValue(false);
        assertTrue(ProviderHacks.getSourceRankerFactory().getAppropriateRanker() instanceof LegacyRanker);
        
        // and vice versa
        DownloadSettings.USE_HEADPINGS.setValue(true);
        assertTrue(ProviderHacks.getSourceRankerFactory().getAppropriateRanker() instanceof PingRanker);
    }
    
    public void testUpdateRanker() throws Exception {
        // LegacyRanker -> PingRanker
        ProviderHacks.getNetworkManager().acceptedIncomingConnection();
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_acceptedSolicitedIncoming",Boolean.FALSE);
        assertFalse(ProviderHacks.getNetworkManager().canReceiveSolicited());
        SourceRanker ranker = ProviderHacks.getSourceRankerFactory().getAppropriateRanker();
        
        assertTrue(ranker instanceof LegacyRanker);
        
        // now we accept some solicited
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_acceptedSolicitedIncoming",Boolean.TRUE);
        
        ranker = ProviderHacks.getSourceRankerFactory().getAppropriateRanker(ranker);
        
        assertTrue(ranker instanceof PingRanker);
        
        // now we try again
        SourceRanker ranker2 = ProviderHacks.getSourceRankerFactory().getAppropriateRanker(ranker);
        
        // it should be the same object
        assertTrue(ranker == ranker2);
    }

}
