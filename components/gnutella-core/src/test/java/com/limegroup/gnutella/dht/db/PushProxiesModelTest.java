package com.limegroup.gnutella.dht.db;

import java.util.Collection;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.db.Storable;
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
    public void testGetStorablesForNoneFirewalledPeer() {
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

}
