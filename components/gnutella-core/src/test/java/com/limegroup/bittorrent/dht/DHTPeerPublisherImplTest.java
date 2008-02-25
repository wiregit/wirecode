package com.limegroup.bittorrent.dht;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.mojito.MojitoDHT;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentDHTManager;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;

public class DHTPeerPublisherImplTest extends BaseTestCase {

    public DHTPeerPublisherImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DHTPeerPublisherImplTest.class);
    }

    private Mockery mockery;

    public void setUp() throws Exception {
        mockery = new Mockery();
    }

    public void testPublishYourself() throws Exception {
        final ManagedTorrent managedTorrent = mockery.mock(ManagedTorrent.class);
        final TorrentDHTManager torrentDHTManager = mockery.mock(TorrentDHTManager.class);
        final DHTPeerPublisher dhtPeerPublisher = mockery.mock(DHTPeerPublisher.class);
        
        final DHTManager dhtManager = mockery.mock(DHTManager.class);
        final ApplicationServices applicationServices = mockery.mock(ApplicationServices.class);
        final NetworkManager networkManager = mockery.mock(NetworkManager.class);
        final MojitoDHT dht = mockery.mock(MojitoDHT.class);
        
        mockery.checking(new Expectations() {{
            atLeast(1).of(dhtManager).getMojitoDHT();
            will(returnValue(dht));
        }});        
        
//        // final DHTPeerLocatorFactory dptf =
//        // mockery.mock(DHTPeerLocatorFactory.class);
//        final ManagedTorrentFactory mtf = mockery.mock(ManagedTorrentFactory.class);
//        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
//        final TorrentContext tc = mockery.mock(TorrentContext.class);
//        final BTMetaInfo btmi = mockery.mock(BTMetaInfo.class);
//        mockery.checking(new Expectations() {{
//            one(tc).getDiskManager(); will(returnValue(tdm));
//            one(tc).getMetaInfo();will(returnValue(btmi));
//        }
//        });
//        final BTMetaInfo torrentMeta = mockery.mock(BTMetaInfo.class);        
//        Module m = new AbstractModule() {
//            public void configure() {
//              // bind(DHTPeerLocatorFactory.class).annotatedWith(Names.named("Peer
//                // Locator")).toInstance(dptf);
//                bind(ManagedTorrentFactory.class).annotatedWith(Names.named("Peer Locator Torrent")).toInstance(mtf);               
//            }
//        };
//        Injector inj = LimeTestUtils.createInjector(m);
//        DHTPeerPublisher dhtPeerLocator = inj.getInstance(DHTPeerPublisher.class);
//        ManagedTorrentFactory managedTorrentFactory = inj.getInstance(ManagedTorrentFactory.class);        
//        ManagedTorrent torrent = managedTorrentFactory.create(tc);        
//        // DHTPeerPublisher dhtPeerLocator =
//        // dhtPeerLocatorFactory.createDHTPeerLocator(torrent, torrentMeta);
//        dhtPeerLocator.publishYourself(torrent);        
    }
}
