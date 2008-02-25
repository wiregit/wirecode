package com.limegroup.bittorrent;

import junit.framework.Test;

import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

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

    public void testChunkVerified() throws Exception {
//        final ManagedTorrent managedTorrent = mockery.mock(ManagedTorrent.class);
//        final TorrentDHTManager torrentDHTManager = mockery.mock(TorrentDHTManager.class);
//        final DHTPeerPublisher dhtPeerPublisher = mockery.mock(DHTPeerPublisher.class);
//
//        mockery.checking(new Expectations() {
//            {
//                atLeast(1).of(dhtPeerPublisher).publishYourself(managedTorrent);
//            }
//        });
    }
}
