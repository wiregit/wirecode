package com.limegroup.gnutella.net.address;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.BlockingAddressResolutionObserver;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.stubs.NetworkManagerStub;

public class SameNATAddressResolverTest extends BaseTestCase {

    private NetworkManagerStub networkManagerStub;
    private SameNATAddressResolver resolver;

    public SameNATAddressResolverTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        resolver = new SameNATAddressResolver(networkManagerStub);
        resolver.register(networkManagerStub.getListenerSupport());
        networkManagerStub.setExternalAddress(new byte[] { (byte)129, 0, 0, 1 });
        networkManagerStub.setAddress(new byte[] { (byte)192, (byte)168, 0, 1});
        networkManagerStub.fireEvent(new AddressEvent(new Address(){}, Address.EventType.ADDRESS_CHANGED));
    }
    
    public void testCanResolve() {
        assertTrue(resolver.canResolve(new FirewalledAddress(null, null, null, null, 0)));
        assertFalse(resolver.canResolve(new ResolvedFirewalledAddress(null, null, null, null, 0)));
    }

    public void testResolvesSameNatAddressToConnectable() throws Exception {
        BlockingAddressResolutionObserver observer = new BlockingAddressResolutionObserver();
        FirewalledAddress behindSameNatAddress = createAddress("129.0.0.1", "192.168.0.2");
        resolver.resolve(behindSameNatAddress, 1, observer);
        Address[] resolvedAddresses = observer.getAddresses();
        Connectable connectable = (Connectable) resolvedAddresses[0];
        assertEquals("192.168.0.2", connectable.getAddress());
    }
    
    public void testDoesNotResolveDifferentSiteLocalNetwork() throws Exception {
        BlockingAddressResolutionObserver observer = new BlockingAddressResolutionObserver();
        FirewalledAddress samePublicAddressDifferentSiteLocalNetwork = createAddress("129.0.0.1", "172.16.0.5");
        resolver.resolve(samePublicAddressDifferentSiteLocalNetwork, 1, observer);
        Address[] resolvedAddresses = observer.getAddresses();
        assertTrue(resolvedAddresses[0] instanceof ResolvedFirewalledAddress);
    }
    
    public void testDoesNotResolveDifferentPublicAddress() throws Exception {
        BlockingAddressResolutionObserver observer = new BlockingAddressResolutionObserver();
        FirewalledAddress samePublicAddressDifferentSiteLocalNetwork = createAddress("128.0.0.1", "191.168.0.2");
        resolver.resolve(samePublicAddressDifferentSiteLocalNetwork, 1, observer);
        Address[] resolvedAddresses = observer.getAddresses();
        assertTrue(resolvedAddresses[0] instanceof ResolvedFirewalledAddress);
    }
    
    private FirewalledAddress createAddress(String publicAddress, String privateAddress) throws Exception {
        return new FirewalledAddress(new ConnectableImpl(publicAddress, 5555, false), 
                new ConnectableImpl(privateAddress, 6666, true),
                null, null, 0);
    }

}
