package com.limegroup.bittorrent;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
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
      
      Module m = new AbstractModule() {
          public void configure() {
              bind(DHTPeerPublisher.class).toInstance(dhtPeerPublisher);              
          }
      };
      
      Injector inj = LimeTestUtils.createInjector(m);
      
      TorrentDHTManager torrentDHTManager = inj.getInstance(TorrentDHTManager.class);
      final TorrentEvent torrentEvent = new TorrentEvent(this, TorrentEvent.Type.CHUNK_VERIFIED, managedTorrent);      
      

      mockery.checking(new Expectations() {
          {
              atLeast(1).of(dhtPeerPublisher).publishYourself(managedTorrent);
          }
      });
      
      torrentDHTManager.handleTorrentEvent(torrentEvent);
      mockery.assertIsSatisfied();
  }    
}
