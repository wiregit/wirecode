package org.limewire.xmpp.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.limewire.common.LimeWireCommonModule;
import org.limewire.inject.AbstractModule;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.EmptyProxySettings;
import org.limewire.net.EmptySocketBindingSettings;
import org.limewire.net.LimeWireNetModule;
import org.limewire.net.ProxySettings;
import org.limewire.net.SocketBindingSettings;
import org.limewire.net.address.AddressEvent;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.messages.FileMetaDataImpl;
import org.xmlpull.v1.XmlPullParserException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

public class XMPPServiceTest extends BaseTestCase {
    protected Injector injector;
    protected ServiceRegistry registry;
    
    protected RosterListenerMock rosterListener;
    protected RosterListenerMock rosterListener2;
    protected AddressEventTestBroadcaster addressEventBroadcaster;
    private FileOfferHandlerMock fileOfferHandler;

    public XMPPServiceTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        injector = createInjector(getModules());
        registry = injector.getInstance(ServiceRegistry.class);
        registry.initialize();
        registry.start();
        Thread.sleep(10 * 1000); // allow login, roster, presence, library messages to be
                                // sent, received   
                                // TODO wait()/notify()
        assertEquals("another limebuddy2 presence has been detecteded, test cannnot run", 0, rosterListener.roster.get("limebuddy2@gmail.com").size());
        assertEquals("another limebuddy1 presence has been detecteded, test cannnot run", 0, rosterListener2.roster.get("limebuddy1@gmail.com").size());
    }
    
    protected Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.PRODUCTION, modules);
    }

    private Module [] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCommonModule());
        modules.addAll(getServiceModules());
        return modules.toArray(new Module[modules.size()]);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        registry.stop();
    }

    protected List<Module> getServiceModules() {
        rosterListener = new RosterListenerMock();
        rosterListener2 = new RosterListenerMock();
        final XMPPConnectionConfiguration configuration = new XMPPConnectionConfigurationMock("limebuddy1@gmail.com",
                "limebuddy123", "talk.google.com", 5222, "gmail.com", rosterListener);
        final XMPPConnectionConfiguration configuration2 = new XMPPConnectionConfigurationMock("limebuddy2@gmail.com",
                "limebuddy234", "talk.google.com", 5222, "gmail.com", rosterListener2);
        Module xmppModule = new LimeWireXMPPModule();
        addressEventBroadcaster = new AddressEventTestBroadcaster();
        fileOfferHandler = new FileOfferHandlerMock();
        Module m = new AbstractModule() {
            protected void configure() {
                bind(new TypeLiteral<ListenerSupport<AddressEvent>>(){}).toInstance(addressEventBroadcaster);
                bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).toProvider(new XMPPConnectionConfigurationListProvider(configuration, configuration2));                
                bind(FileOfferHandlerMock.class).toInstance(fileOfferHandler);
                bind(XMPPConnectionListener.class).to(XMPPConnectionListenerMock.class);
            }
        };
        return Arrays.asList(xmppModule, m, new LimeWireNetTestModule());
    }

    public void testRosterIsPopulated() throws InterruptedException, UnknownHostException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("limebuddy2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(0, rosterListener.roster.get("limebuddy2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("limebuddy1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(0, rosterListener2.roster.get("limebuddy1@gmail.com").size());
    }
    
    public void testSetName() throws InterruptedException {
        assertEquals("buddy2", rosterListener.users.get("limebuddy2@gmail.com").getName());
        rosterListener.users.get("limebuddy2@gmail.com").setName("foo");
        Thread.sleep(3 * 1000);
        assertEquals("foo", rosterListener.users.get("limebuddy2@gmail.com").getName());
        rosterListener.users.get("limebuddy2@gmail.com").setName("buddy2");
    }
    
    public void testDetectLimePresences() throws InterruptedException, UnknownHostException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("limebuddy2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(0, rosterListener.roster.get("limebuddy2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("limebuddy1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(0, rosterListener2.roster.get("limebuddy1@gmail.com").size());
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000 * 2);
        
        assertEquals(1, rosterListener.roster.get("limebuddy2@gmail.com").size());
        assertTrue(rosterListener.roster.get("limebuddy2@gmail.com").get(0) instanceof LimePresence);
        LimePresence buddy2 = (LimePresence)rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());
        Connectable address = (Connectable)buddy2.getPresenceAddress();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable()); 
        assertTrue(new String(buddy2.getAuthToken()).startsWith("limebuddy1@gmail.com"));
        
        assertEquals(1, rosterListener2.roster.get("limebuddy1@gmail.com").size());
        assertTrue(rosterListener2.roster.get("limebuddy1@gmail.com").get(0) instanceof LimePresence);
        LimePresence buddy1 = (LimePresence)rosterListener2.roster.get("limebuddy1@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy1.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());
        address = (Connectable)buddy1.getPresenceAddress();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        assertTrue(new String(buddy1.getAuthToken()).startsWith("limebuddy2@gmail.com"));
    }
    
    public void testUserLogout() throws InterruptedException {
        XMPPService service = injector.getInstance(XMPPService.class);
        List<XMPPConnection> connections = service.getConnections();
        assertTrue(connections.get(0).isLoggedIn());
        connections.get(0).logout();
        assertFalse(connections.get(0).isLoggedIn());
    }
    
    public void testStatusChanges() throws InterruptedException, UnknownHostException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("limebuddy2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(0, rosterListener.roster.get("limebuddy2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("limebuddy1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(0, rosterListener2.roster.get("limebuddy1@gmail.com").size());
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000); 
        
        LimePresence buddy2 = (LimePresence)rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());

        XMPPService xmppService = injector.getInstance(XMPPService.class);
        for(XMPPConnection connection : xmppService.getConnections()) {
            if(connection.getConfiguration().getUsername().equals("limebuddy2@gmail.com")) {
                connection.setMode(Presence.Mode.away);
            }
        }
        
        Thread.sleep(1000); 
        
        buddy2 = (LimePresence)rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.away, buddy2.getMode());
    }
    
    public void testChat() throws InterruptedException, XMPPException, IOException {
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        MessageReaderMock reader = new MessageReaderMock();
        Presence limeBuddy2 = rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        MessageWriter writer = limeBuddy2.createChat(reader);
        writer.writeMessage("hello world");
       
        Thread.sleep(2 * 1000);
        
        IncomingChatListenerMock incomingChatListener2 = rosterListener2.listener;
        MessageWriter writer2 = incomingChatListener2.writer;
        writer2.writeMessage("goodbye world");
        
        Thread.sleep(2 * 1000);
        
        assertEquals(1, incomingChatListener2.reader.messages.size());
        assertEquals("hello world", incomingChatListener2.reader.messages.get(0));
        
        assertEquals(1, reader.messages.size());
        assertEquals("goodbye world", reader.messages.get(0)); 
        
    }
    
    public void testOfferFile() throws InterruptedException, IOException, XmlPullParserException {
        
        HashMap<String, ArrayList<Presence>> roster1 = rosterListener.roster;
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        LimePresence limebuddy2 = ((LimePresence)roster1.get("limebuddy2@gmail.com").get(0));
        FileMetaDataImpl metaData = new FileMetaDataImpl();
        metaData.setId(new Random().nextInt() + "");
        metaData.setName("a_cool_file.txt");
        metaData.setSize(1000);
        metaData.setCreateTime(new Date());
        metaData.setDescription("cool file");
        limebuddy2.offerFile(metaData);
        
        Thread.sleep(1000);

        List<FileMetaData> offers = fileOfferHandler.offers;
        assertEquals(1, offers.size());
        assertEquals("a_cool_file.txt", offers.get(0).getName());
    }
    
    public void testDetectAddressChanges() throws InterruptedException, UnknownHostException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("limebuddy2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(0, rosterListener.roster.get("limebuddy2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("limebuddy1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(0, rosterListener2.roster.get("limebuddy1@gmail.com").size());
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        assertEquals(1, rosterListener.roster.get("limebuddy2@gmail.com").size());
        assertTrue(rosterListener.roster.get("limebuddy2@gmail.com").get(0) instanceof LimePresence);
        LimePresence buddy2 = (LimePresence)rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy2.getType());
        Connectable address = (Connectable)buddy2.getPresenceAddress();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        
        assertEquals(1, rosterListener2.roster.get("limebuddy1@gmail.com").size());
        assertTrue(rosterListener2.roster.get("limebuddy1@gmail.com").get(0) instanceof LimePresence);
        LimePresence buddy1 = (LimePresence)rosterListener2.roster.get("limebuddy1@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy1.getType());
        address = (Connectable)buddy1.getPresenceAddress();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());

        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("200.200.200.200", 5000, false),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        assertEquals(1, rosterListener.roster.get("limebuddy2@gmail.com").size());
        assertTrue(rosterListener.roster.get("limebuddy2@gmail.com").get(0) instanceof LimePresence);
        buddy2 = (LimePresence)rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy2.getType());
        address = (Connectable)buddy2.getPresenceAddress();
        assertEquals("200.200.200.200", address.getAddress());
        assertEquals(5000, address.getPort());
        assertEquals(false, address.isTLSCapable());

        assertEquals(1, rosterListener2.roster.get("limebuddy1@gmail.com").size());
        assertTrue(rosterListener2.roster.get("limebuddy1@gmail.com").get(0) instanceof LimePresence);
        buddy1 = (LimePresence)rosterListener2.roster.get("limebuddy1@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy1.getType());
        address = (Connectable)buddy1.getPresenceAddress();
        assertEquals("200.200.200.200", address.getAddress());
        assertEquals(5000, address.getPort());
        assertEquals(false, address.isTLSCapable());
    }
    
    class LimeWireNetTestModule extends LimeWireNetModule {
        @Override
        protected void configure() {
            super.configure();
            bind(ProxySettings.class).to(EmptyProxySettings.class);
            bind(SocketBindingSettings.class).to(EmptySocketBindingSettings.class);
            bind(NetworkInstanceUtils.class).to(SimpleNetworkInstanceUtils.class);
        }
    }

}
