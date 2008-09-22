package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.StrictIpPortSet;
import org.limewire.rudp.RUDPUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.net.address.FirewalledAddress;
import com.limegroup.gnutella.net.address.PushProxyMediatorAddress;
import com.limegroup.gnutella.net.address.PushProxyMediatorAddressImpl;
import com.limegroup.gnutella.util.LimeTestCase;

public class NetworkManagerImplTest extends LimeTestCase {
    
    private Injector injector;
    private AddressChangedListener addressChangedListener;
    
    public NetworkManagerImplTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        injector = LimeTestUtils.createInjectorAndStart(new AbstractModule() {
            @Override
            protected void configure() {
                //bind(ConnectionManager.class).toInstance(connectionManager);
                bind(AddressChangedListener.class);               
            }
        });
        addressChangedListener = injector.getInstance(AddressChangedListener.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setListeningPort(0);
    }
    
    protected void tearDown() {
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        lifecycleManager.shutdown();    
    }
    
    private void assertEquals(Connectable connectable, Address address) {
        assertEquals(connectable + "!=" + address, 0, ConnectableImpl.COMPARATOR.compare(connectable, (Connectable) address));
    }
    
    public void testDirectConnectionAddressEvent() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName(RemoteFileDesc.BOGUS_IP));
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setListeningPort(5000);
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
    }
    
    public void testNewDirectConnectionAddressEventAcceptedIncomingTrigger() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
    }
    
    public void testNewDirectConnectionAddressEventExternalAddressTrigger() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);        
        acceptor.setIncoming(true);
        acceptor.setListeningPort(5000);
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
    }
    
    public void testNewDirectConnectionAddressEventPortTrigger() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);        
        acceptor.setIncoming(true);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setListeningPort(5000);        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
    }    
    
    public void testDirectConnectionAddressEventNoDups() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setListeningPort(5000);
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setIncoming(true);
        assertEquals(0, addressChangedListener.events.size());
    }
    
    public void testDirectConnectionAddressEventTriggeredAfterAllInfoIsThere() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        
        acceptor.setExternalAddress(InetAddress.getByAddress(new byte[] { (byte)129, 0, 0, 1 }));
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setListeningPort(5000);
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        
        assertEquals(new ConnectableImpl("129.0.0.1", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
    }
    
    public void testDirectConnectionAddressChangedExternalAddressTrigger() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("200.200.200.200", 5000, true), addressChangedListener.events.get(0).getSource());        
    }
    
    public void testDirectConnectionAddressChangedEventPortTrigger() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
        acceptor.setListeningPort(5001);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new ConnectableImpl("199.199.199.199", 5001, true), addressChangedListener.events.get(0).getSource());        
    }
    
    public void testPushProxyAddressEvent() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        assertEquals(0, addressChangedListener.events.size());
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        PushProxyMediatorAddress address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(1, addressChangedListener.events.size());
        FirewalledAddress firewalledAddress = (FirewalledAddress)addressChangedListener.events.get(0).getSource();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
    }
    
    public void testPushProxyAddressChangedEvent() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        assertEquals(0, addressChangedListener.events.size());
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        PushProxyMediatorAddress address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(1, addressChangedListener.events.size());
        FirewalledAddress firewalledAddress = (FirewalledAddress)addressChangedListener.events.get(0).getSource();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
        addressChangedListener.events.clear();
        
        proxies = new HashSet<Connectable>();
        Connectable proxyAddress = new ConnectableImpl("199.199.199.199", 5000, true);
        Connectable proxyAddress2 = new ConnectableImpl("200.200.200.200", 5001, false);
        proxies.add(proxyAddress);
        proxies.add(proxyAddress2);
        address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(1, addressChangedListener.events.size());
        firewalledAddress = (FirewalledAddress)addressChangedListener.events.get(0).getSource();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
    }
    
    public void testPushProxyAddressEventNoDups() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        assertEquals(0, addressChangedListener.events.size());
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        PushProxyMediatorAddress address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(1, addressChangedListener.events.size());
        FirewalledAddress firewalledAddress = (FirewalledAddress)addressChangedListener.events.get(0).getSource();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
        
        addressChangedListener.events.clear();
        
        proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(0, addressChangedListener.events.size());
    }
    
    public void testNewPushProxyHolePunchAddressEventFWTStatusTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertEquals(0, addressChangedListener.events.size());
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        assertEquals(1, addressChangedListener.events.size());
        FirewalledAddress firewalledAddress = (FirewalledAddress)addressChangedListener.events.get(0).getSource();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
        assertEquals(0, firewalledAddress.getFwtVersion());
        
        addressChangedListener.events.clear();
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        networkManager.incomingStatusChanged();
        assertEquals(1, addressChangedListener.events.size());
        addressChangedListener.events.clear();
        
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);        
        networkManager.incomingStatusChanged();
        assertEquals(1, addressChangedListener.events.size());
        
        firewalledAddress = (FirewalledAddress)addressChangedListener.events.get(0).getSource();
        assertEquals(proxies, firewalledAddress.getPushProxies());
        assertEquals(applicationServices.getMyGUID(), firewalledAddress.getClientGuid().bytes());
        assertEquals(RUDPUtils.VERSION, firewalledAddress.getFwtVersion());
        assertEquals(new ConnectableImpl("200.200.200.200", 5001, true), firewalledAddress.getPublicAddress());
    }
    
    public void testNewPushProxyHolePunchAddressEventPushProxyTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertEquals(0, addressChangedListener.events.size());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        networkManager.incomingStatusChanged();
        assertEquals(0, addressChangedListener.events.size());
        
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);   
        
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        assertEquals(1, addressChangedListener.events.size());
        FirewalledAddress expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl("0.0.0.0", 5001, networkManager.isIncomingTLSEnabled()), new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        assertEquals(expectedAddress, addressChangedListener.events.get(0).getSource()); 
    }
    
    public void testPushProxyHolePunchAddressChangedEventPushProxyTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertEquals(0, addressChangedListener.events.size());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);   
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        FirewalledAddress expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl("0.0.0.0", networkManager.getNonForcedPort(), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(expectedAddress, addressChangedListener.events.get(0).getSource()); 
        addressChangedListener.events.clear();
        
        proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        proxies.add(new ConnectableImpl("201.201.201.201", 5002, false));
        mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl("0.0.0.0", networkManager.getNonForcedPort(), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(expectedAddress, addressChangedListener.events.get(0).getSource()); 
    }
    
    public void testPushProxyHolePunchAddressChangedEventFWTStatusTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertEquals(0, addressChangedListener.events.size());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);   
        
        Set<Connectable> proxies = new HashSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        FirewalledAddress expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl("0.0.0.0", networkManager.getNonForcedPort(), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(expectedAddress, addressChangedListener.events.get(0).getSource()); 
        addressChangedListener.events.clear();
        
        udpService.setReceiveSolicited(false);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        networkManager.incomingStatusChanged();
        
        expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl("0.0.0.0", networkManager.getNonForcedPort(), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(expectedAddress, addressChangedListener.events.get(0).getSource()); 
    }
    
    public void testPushProxyHolePunchAddressEventNoDups() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertEquals(0, addressChangedListener.events.size());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);   
        
        Set<Connectable> proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        FirewalledAddress expectedAddress = new FirewalledAddress(new ConnectableImpl("200.200.200.200", 5001, true),
                new ConnectableImpl("0.0.0.0", networkManager.getNonForcedPort(), networkManager.isIncomingTLSEnabled()),
                new GUID(applicationServices.getMyGUID()), proxies, networkManager.supportsFWTVersion());
        
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(expectedAddress, addressChangedListener.events.get(0).getSource()); 
        addressChangedListener.events.clear();
        
        networkManager.incomingStatusChanged();
        assertEquals(0, addressChangedListener.events.size());
        
        proxies = new StrictIpPortSet<Connectable>();
        proxies.add(new ConnectableImpl("199.199.199.199", 5000, true));
        mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        assertEquals(0, addressChangedListener.events.size());
    }

    @Singleton
    public static class AddressChangedListener implements EventListener<AddressEvent> {
        List<AddressEvent> events = new ArrayList<AddressEvent>();
        
        @Inject
        void register(ListenerSupport<AddressEvent> broadcaster) {
            broadcaster.addListener(this);
        }
        
        public void handleEvent(AddressEvent event) {
            events.add(event);
        }
    }
}
