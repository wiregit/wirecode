package com.limegroup.bittorrent;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.LimeTestUtils;

public class ManagedTorrentImplTest extends BaseTestCase{
    public ManagedTorrentImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ManagedTorrentImplTest.class);
    }

    private Mockery mockery;

    public void setUp() throws Exception {
        mockery = new Mockery();
    }

    /**
     * Tests to see DispatchEvent calls the handleTorrentEvent in TorrentDHTManager with the appropriate events
     */
    public void testDispatchEvent() {
        final TorrentDHTManager torrentDHTManager = mockery.mock(TorrentDHTManager.class);
        final TorrentContext context = mockery.mock(TorrentContext.class); 

        Module m = new AbstractModule() {
            public void configure() {
                bind(TorrentDHTManager.class).toInstance(torrentDHTManager);              
            }
        };
        
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(context).getMetaInfo();
                atLeast(1).of(context).getDiskManager();
            }
        });
        
        Injector inj = LimeTestUtils.createInjector(m);
        ManagedTorrentFactory managedTorrentFactory = inj.getInstance(ManagedTorrentFactory.class);
        ManagedTorrentImpl managedTorrent = (ManagedTorrentImpl)managedTorrentFactory.create(context);
        final TorrentEvent torrentEventChunkVerified = new TorrentEvent(managedTorrent, TorrentEvent.Type.CHUNK_VERIFIED, managedTorrent, null);
        final TorrentEvent torrentEventTrackerFailed = new TorrentEvent(managedTorrent, TorrentEvent.Type.TRACKER_FAILED, managedTorrent, null);
        
        mockery.checking(new Expectations() {
            {
                one(torrentDHTManager).init();
                one(torrentDHTManager).handleTorrentEvent(torrentEventChunkVerified);
                one(torrentDHTManager).handleTorrentEvent(torrentEventTrackerFailed);
            }
        });
                
        managedTorrent.init();
        managedTorrent.dispatchEvent(torrentEventChunkVerified);
        managedTorrent.dispatchEvent(torrentEventTrackerFailed);
        mockery.assertIsSatisfied();        
    }
}
