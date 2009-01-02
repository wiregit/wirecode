package com.limegroup.gnutella.connection;

import java.net.InetAddress;
import java.util.Properties;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.NetworkUtils;
import org.limewire.util.BaseTestCase;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.stubs.NetworkManagerStub;

public class GnutellaConnectionTest extends BaseTestCase {

    private Mockery context;

    public GnutellaConnectionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GnutellaConnectionTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    /**
     * Ensures the address is updated to the external address if port
     * forwarding is enabled in the firewall, but not in the client.
     * 
     * To have a consistent state, the external address should be 
     * advertised to peers in this case.
     */
    public void testUpdateAddressToExternalAddressIfAcceptedIncoming() throws Exception {
        final Acceptor acceptor = context.mock(Acceptor.class);
        NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(TestUtils.bind(Acceptor.class, NetworkManager.class).toInstances(acceptor, networkManagerStub));
        RoutedConnectionFactory connectionFactory = injector.getInstance(RoutedConnectionFactory.class);
        GnutellaConnection connection = (GnutellaConnection) connectionFactory.createRoutedConnection("10.0.0.1", 5000);
        
        assertFalse(ConnectionSettings.FORCE_IP_ADDRESS.getValue());
        networkManagerStub.setAddress(new byte[] { (byte)192, (byte)168, 0, 1 });
        assertTrue(NetworkUtils.isValidAddress(networkManagerStub.getAddress()));
        
        final InetAddress address = InetAddress.getByAddress(new byte[] { (byte)129, 0, 0, 1 });
        context.checking(new Expectations() {{
            one(acceptor).acceptedIncoming();
            will(returnValue(true));
            one(acceptor).setAddress(with(equal(address)));
            one(acceptor).setExternalAddress(with(equal(address)));
        }});
        
        Properties props = new Properties();
        props.put(HeaderNames.REMOTE_IP, "129.0.0.1");
        
        connection.updateAddress(new HandshakeResponse(props));
        
        context.assertIsSatisfied();
    }
   
    /**
     * Ensures the address is not updated to the external address if 
     * client can't receive incoming connections.
     */
    public void testUpdateAddressDoesNotSetAddressToExternalIfNotAcceptedIncoming() throws Exception {
        final Acceptor acceptor = context.mock(Acceptor.class);
        NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(TestUtils.bind(Acceptor.class, NetworkManager.class).toInstances(acceptor, networkManagerStub));
        RoutedConnectionFactory connectionFactory = injector.getInstance(RoutedConnectionFactory.class);
        GnutellaConnection connection = (GnutellaConnection) connectionFactory.createRoutedConnection("10.0.0.1", 5000);
        
        assertFalse(ConnectionSettings.FORCE_IP_ADDRESS.getValue());
        networkManagerStub.setAddress(new byte[] { (byte)192, (byte)168, 0, 1 });
        assertTrue(NetworkUtils.isValidAddress(networkManagerStub.getAddress()));
        
        final InetAddress address = InetAddress.getByAddress(new byte[] { (byte)129, 0, 0, 1 });
        context.checking(new Expectations() {{
            one(acceptor).acceptedIncoming();
            will(returnValue(false));
            never(acceptor).setAddress(with(equal(address)));
            one(acceptor).setExternalAddress(with(equal(address)));
        }});
        
        Properties props = new Properties();
        props.put(HeaderNames.REMOTE_IP, "129.0.0.1");
        
        connection.updateAddress(new HandshakeResponse(props));
        
        context.assertIsSatisfied();
    }
}
