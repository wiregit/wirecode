package com.limegroup.bittorrent;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.bittorrent.dht.DHTPeerLocator;
import com.limegroup.bittorrent.dht.DHTPeerPublisher;
import com.limegroup.gnutella.LimeTestUtils;

public class TorrentDHTManagerImplTest extends BaseTestCase {

    public TorrentDHTManagerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TorrentDHTManagerImplTest.class);
    }

    private Mockery mockery;

    public void setUp() throws Exception {
        mockery = new Mockery();
    }

    public void testHandleTorrentEvent() throws Exception {
        final ManagedTorrent managedTorrent = mockery.mock(ManagedTorrent.class);
        final DHTPeerPublisher dhtPeerPublisher = mockery.mock(DHTPeerPublisher.class);
        final DHTPeerLocator dhtPeerLocator = mockery.mock(DHTPeerLocator.class);

        Module m = new AbstractModule() {
            public void configure() {
                bind(DHTPeerPublisher.class).toInstance(dhtPeerPublisher);
                bind(DHTPeerLocator.class).toInstance(dhtPeerLocator);
            }
        };

        Injector inj = LimeTestUtils.createInjector(m);

        TorrentDHTManager torrentDHTManager = inj.getInstance(TorrentDHTManager.class);
        final TorrentEvent torrentEventChunkVerified = new TorrentEvent(this,
                TorrentEvent.Type.CHUNK_VERIFIED, managedTorrent);
        final TorrentEvent torrentEventTrackerFailed = new TorrentEvent(this,
                TorrentEvent.Type.TRACKER_FAILED, managedTorrent);

        mockery.checking(new Expectations() {
            {
                one(dhtPeerPublisher).init();
                one(dhtPeerLocator).init();
                atLeast(1).of(dhtPeerPublisher).publishYourself(managedTorrent);
                atLeast(1).of(dhtPeerLocator).locatePeer(managedTorrent);
            }
        });

        torrentDHTManager.init();
        torrentDHTManager.handleTorrentEvent(torrentEventChunkVerified);
        torrentDHTManager.handleTorrentEvent(torrentEventTrackerFailed);
        mockery.assertIsSatisfied();
    }
}
