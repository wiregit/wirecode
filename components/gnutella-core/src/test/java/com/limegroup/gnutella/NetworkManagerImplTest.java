package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.DirectConnectionAddress;
import org.limewire.net.address.DirectConnectionAddressImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.net.address.gnutella.PushProxyAddress;
import com.limegroup.gnutella.net.address.gnutella.PushProxyAddressImpl;
import com.limegroup.gnutella.net.address.gnutella.PushProxyHolePunchAddressImpl;
import com.limegroup.gnutella.net.address.gnutella.PushProxyMediatorAddress;
import com.limegroup.gnutella.net.address.gnutella.PushProxyMediatorAddressImpl;
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
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
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
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
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
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
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
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
    }    
    
    public void testDirectConnectionAddressEventNoDups() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setListeningPort(5000);
        assertEquals(0, addressChangedListener.events.size());
        acceptor.setIncoming(true);
        assertEquals(0, addressChangedListener.events.size());
    }
    
    public void xxxtestDirectConnectionAddressChangedAcceptedIncomingTrigger() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
        acceptor.setIncoming(false);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("200.200.200.200", 5001, true), addressChangedListener.events.get(0).getSource());        
    }
    
    public void testDirectConnectionAddressChangedExternalAddressTrigger() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("200.200.200.200", 5000, true), addressChangedListener.events.get(0).getSource());        
    }
    
    public void testDirectConnectionAddressChangedEventPortTrigger() throws IOException {
        assertEquals(0, addressChangedListener.events.size());
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        acceptor.setExternalAddress(InetAddress.getByName("199.199.199.199"));
        acceptor.setListeningPort(5000);
        acceptor.setIncoming(true);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5000, true), addressChangedListener.events.get(0).getSource());
        addressChangedListener.events.clear();
        acceptor.setListeningPort(5001);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5001, true), addressChangedListener.events.get(0).getSource());        
    }
    
    public void testPushProxyAddressEvent() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        assertEquals(0, addressChangedListener.events.size());
        Set<PushProxyAddress> proxies = new HashSet<PushProxyAddress>();
        PushProxyAddressImpl proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
        PushProxyMediatorAddress address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(address, addressChangedListener.events.get(0).getSource());        
    }
    
    public void testPushProxyAddressChangedEvent() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        assertEquals(0, addressChangedListener.events.size());
        Set<PushProxyAddress> proxies = new HashSet<PushProxyAddress>();
        PushProxyAddressImpl proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
        PushProxyMediatorAddress address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(address, addressChangedListener.events.get(0).getSource()); 
        addressChangedListener.events.clear();
        
        proxies = new HashSet<PushProxyAddress>();
        proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        PushProxyAddressImpl proxyAddress2 = new PushProxyAddressImpl("200.200.200.200", 5001, false);
        proxies.add(proxyAddress);
        proxies.add(proxyAddress2);
        address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(address, addressChangedListener.events.get(0).getSource()); 
    }
    
    public void testPushProxyAddressEventNoDups() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        assertEquals(0, addressChangedListener.events.size());
        Set<PushProxyAddress> proxies = new HashSet<PushProxyAddress>();
        PushProxyAddressImpl proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
        PushProxyMediatorAddress address = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(address);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(address, addressChangedListener.events.get(0).getSource()); 
        addressChangedListener.events.clear();
        
        proxies = new HashSet<PushProxyAddress>();
        proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
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
        Set<PushProxyAddress> proxies = new HashSet<PushProxyAddress>();
        PushProxyAddressImpl proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(mediatorAddress, addressChangedListener.events.get(0).getSource()); 
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
        
        DirectConnectionAddress directAddress = new DirectConnectionAddressImpl("200.200.200.200", 5001, true);
        PushProxyHolePunchAddressImpl holePunch = new PushProxyHolePunchAddressImpl(networkManager.supportsFWTVersion(), 
                directAddress, mediatorAddress);
        assertEquals(holePunch, addressChangedListener.events.get(0).getSource()); 
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
        
        Set<PushProxyAddress> proxies = new HashSet<PushProxyAddress>();
        PushProxyAddressImpl proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        assertEquals(1, addressChangedListener.events.size());
        
        DirectConnectionAddress directAddress = new DirectConnectionAddressImpl("200.200.200.200", 5001, true);
        PushProxyHolePunchAddressImpl holePunch = new PushProxyHolePunchAddressImpl(networkManager.supportsFWTVersion(), 
                directAddress, mediatorAddress);
        assertEquals(holePunch, addressChangedListener.events.get(0).getSource()); 
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
        
        Set<PushProxyAddress> proxies = new HashSet<PushProxyAddress>();
        PushProxyAddressImpl proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        DirectConnectionAddress directAddress = new DirectConnectionAddressImpl("200.200.200.200", 5001, true);
        PushProxyHolePunchAddressImpl holePunch = new PushProxyHolePunchAddressImpl(networkManager.supportsFWTVersion(), 
                directAddress, mediatorAddress);
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(holePunch, addressChangedListener.events.get(0).getSource()); 
        addressChangedListener.events.clear();
        
        proxies = new HashSet<PushProxyAddress>();
        proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        PushProxyAddressImpl proxyAddress2 = new PushProxyAddressImpl("201.201.201.201", 5002, false);
        proxies.add(proxyAddress);
        proxies.add(proxyAddress2);
        mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        holePunch = new PushProxyHolePunchAddressImpl(networkManager.supportsFWTVersion(), 
                directAddress, mediatorAddress);
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(holePunch, addressChangedListener.events.get(0).getSource()); 
    }
    
    public void xxxtestPushProxyHolePunchAddressChangedEventFWTStatusTrigger() throws IOException {
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        AcceptorImpl acceptor = (AcceptorImpl)injector.getInstance(Acceptor.class);
        assertEquals(0, addressChangedListener.events.size());
        
        UDPService udpService = injector.getInstance(UDPService.class);
        udpService.setReceiveSolicited(true);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        
        acceptor.setExternalAddress(InetAddress.getByName("200.200.200.200"));
        acceptor.setListeningPort(5001);   
        
        Set<PushProxyAddress> proxies = new HashSet<PushProxyAddress>();
        PushProxyAddressImpl proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        DirectConnectionAddress directAddress = new DirectConnectionAddressImpl("200.200.200.200", 5001, true);
        PushProxyHolePunchAddressImpl holePunch = new PushProxyHolePunchAddressImpl(networkManager.supportsFWTVersion(), 
                directAddress, mediatorAddress);
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(holePunch, addressChangedListener.events.get(0).getSource()); 
        addressChangedListener.events.clear();
        
        udpService.setReceiveSolicited(false);
        ConnectionSettings.CANNOT_DO_FWT.setValue(false);
        networkManager.incomingStatusChanged();
        
        holePunch = new PushProxyHolePunchAddressImpl(networkManager.supportsFWTVersion(), 
                directAddress, mediatorAddress);
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(holePunch, addressChangedListener.events.get(0).getSource()); 
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
        
        Set<PushProxyAddress> proxies = new HashSet<PushProxyAddress>();
        PushProxyAddressImpl proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
        PushProxyMediatorAddress mediatorAddress = new PushProxyMediatorAddressImpl(new GUID(applicationServices.getMyGUID()),
                proxies);
        networkManager.newMediatedConnectionAddress(mediatorAddress);
        
        DirectConnectionAddress directAddress = new DirectConnectionAddressImpl("200.200.200.200", 5001, true);
        PushProxyHolePunchAddressImpl holePunch = new PushProxyHolePunchAddressImpl(networkManager.supportsFWTVersion(), 
                directAddress, mediatorAddress);
        
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(holePunch, addressChangedListener.events.get(0).getSource()); 
        addressChangedListener.events.clear();
        
        networkManager.incomingStatusChanged();
        assertEquals(0, addressChangedListener.events.size());
        
        proxies = new HashSet<PushProxyAddress>();
        proxyAddress = new PushProxyAddressImpl("199.199.199.199", 5000, true);
        proxies.add(proxyAddress);
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
