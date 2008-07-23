package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.DirectConnectionAddressImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
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
        acceptor.setListeningPort(5001);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5001, true), addressChangedListener.events.get(0).getSource());        
    }
    
    public void testPushProxyAddressEvent() throws IOException {
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
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
        acceptor.setListeningPort(5001);
        assertEquals(1, addressChangedListener.events.size());
        assertEquals(new DirectConnectionAddressImpl("199.199.199.199", 5001, true), addressChangedListener.events.get(0).getSource());        
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
