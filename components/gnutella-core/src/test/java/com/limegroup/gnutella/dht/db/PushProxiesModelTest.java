package com.limegroup.gnutella.dht.db;

import java.util.Collection;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.settings.SSLSettings;
import com.limegroup.gnutella.stubs.NetworkManagerStub;

public class PushProxiesModelTest extends BaseTestCase {

    public PushProxiesModelTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushProxiesModelTest.class);
    }
    
    /**
     * An integration test that makes sure we create a storable for a 
     * non-firewalled peer.
     */
    public void testGetStorablesForNonFirewalledPeer() {
        final NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setAcceptedIncomingConnection(true);
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        
        PushProxiesModel pushProxiesModel = injector.getInstance(PushProxiesModel.class);
        
        Collection<Storable> storables = pushProxiesModel.getStorables();
        assertEquals(1, storables.size());
        PushProxiesValue value = (PushProxiesValue) storables.iterator().next().getValue();
        Connectable expected = new ConnectableImpl(new IpPortImpl(networkManagerStub.getAddress(), networkManagerStub.getPort()), SSLSettings.isOutgoingTLSEnabled());
        assertEquals(0, IpPort.IP_COMPARATOR.compare(expected, value.getPushProxies().iterator().next()));
    }

    /**
     * When the set of push proxies or the fwt capability of this peer change it 
     * should publish its updated info into the DHT the next time the StorableModel
     * is asked for them.
     */
    public void testGetStorablesWhenProxiesValueHasChanged() throws Exception {
        final NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setSupportsFWTVersion(1);

        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        
        PushProxiesModel pushProxiesModel = injector.getInstance(PushProxiesModel.class);
        
        Collection<Storable> storables = pushProxiesModel.getStorables();
        assertEquals(1, storables.size());
        Storable storable = storables.iterator().next();
        PushProxiesValue value = (PushProxiesValue) storable.getValue();
        Connectable expected = new ConnectableImpl(new IpPortImpl(networkManagerStub.getAddress(), networkManagerStub.getPort()), SSLSettings.isOutgoingTLSEnabled());
        assertEquals(0, IpPort.IP_COMPARATOR.compare(expected, value.getPushProxies().iterator().next()));
        assertEquals(1, value.getFwtVersion());
        
        // mark storable as published and make sure model doesn't return it again right away
        storable.setPublishTime(System.currentTimeMillis());
        storable.setLocationCount(KademliaSettings.REPLICATION_PARAMETER.getValue());
        assertTrue(pushProxiesModel.getStorables().isEmpty());
        
        // change fwt support status so we should get a different pushproxy value 
        // for ourselves that needs to be republished
        networkManagerStub.setSupportsFWTVersion(2);
        storables = pushProxiesModel.getStorables();
        assertEquals("Expected one new storable for the changed push proxy value", 1, storables.size());
        storable = storables.iterator().next();
        value = (PushProxiesValue) storable.getValue();
        assertEquals(2, value.getFwtVersion());
    }
}
