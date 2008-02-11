package com.limegroup.bittorrent;

import junit.framework.Test;

import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

public class DHTPeerLocatorImplTest extends BaseTestCase {

    public DHTPeerLocatorImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTPeerLocatorImplTest.class);
    }
    
    private Mockery mockery;
    public void setUp() throws Exception {
        mockery = new Mockery();
    }
    
    public void testPublish() throws Exception {
        //final DHTPeerLocatorFactory dptf = mockery.mock(DHTPeerLocatorFactory.class);
        final ManagedTorrentFactory mtf = mockery.mock(ManagedTorrentFactory.class);        
        final TorrentContext tc = mockery.mock(TorrentContext.class);
        final BTMetaInfo torrentMeta = mockery.mock(BTMetaInfo.class);
        final TorrentLocation torLoc = mockery.mock(TorrentLocation.class);
        Module m = new AbstractModule() {
            public void configure() {
          //      bind(DHTPeerLocatorFactory.class).annotatedWith(Names.named("Peer Locator")).toInstance(dptf);
                bind(ManagedTorrentFactory.class).annotatedWith(Names.named("Peer Locator Torrent")).toInstance(mtf);               
            }
        };
        Injector inj = LimeTestUtils.createInjector(m);
        DHTPeerLocatorFactory dhtPeerLocatorFactory = inj.getInstance(DHTPeerLocatorFactory.class);
        ManagedTorrentFactory managedTorrentFactory = inj.getInstance(ManagedTorrentFactory.class);        
        ManagedTorrent torrent = managedTorrentFactory.create(tc);        
        DHTPeerLocator dhtPeerLocator = dhtPeerLocatorFactory.create(torrent, torrentMeta);
        //dhtPeerLocator.publish(torLoc);
        System.out.println("blah" + torLoc.getAddress());
    }

    public void testStartSearching() throws Exception {
            
    }
    
    public void testDispatch() throws Exception {
        
    }
}
