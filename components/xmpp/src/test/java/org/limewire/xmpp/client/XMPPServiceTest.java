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
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.friend.feature.features.FileOfferFeature;
import org.limewire.core.api.friend.feature.features.FileOfferer;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.http.auth.LimeWireHttpAuthModule;
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
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
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
        assertEquals("another automatedtestfriend2 presence has been detecteded, test cannnot run", 1, rosterListener.roster.get("automatedtestfriend2@gmail.com").size());
        assertEquals("another automatedtestfriend1 presence has been detecteded, test cannnot run", 1, rosterListener2.roster.get("automatedtestfriend1@gmail.com").size());
    }
    
    protected Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.PRODUCTION, modules);
    }

    private Module [] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCommonModule());
        modules.add(new LimeWireHttpAuthModule());
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
        final XMPPConnectionConfiguration configuration =
            new XMPPConnectionConfigurationMock("automatedtestfriend1@gmail.com",
                    "automatedtestfriend123", "talk.google.com", 5222, "gmail.com",
                    "Gmail", "http://gmail.com/", rosterListener);
        final XMPPConnectionConfiguration configuration2 =
            new XMPPConnectionConfigurationMock("automatedtestfriend2@gmail.com",
                    "automatedtestfriend234", "talk.google.com", 5222, "gmail.com",
                    "Gmail", "http://gmail.com/", rosterListener2);
        Module xmppModule = new LimeWireXMPPModule();
        addressEventBroadcaster = new AddressEventTestBroadcaster();
        fileOfferHandler = new FileOfferHandlerMock();
        Module m = new AbstractModule() {
            protected void configure() {
                bind(new TypeLiteral<ListenerSupport<AddressEvent>>(){}).toInstance(addressEventBroadcaster);
                bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).toProvider(new XMPPConnectionConfigurationListProvider(configuration, configuration2));                
                bind(FileOfferHandlerMock.class).toInstance(fileOfferHandler);
                bind(XMPPConnectionListenerMock.class);
            }
        };
        return Arrays.asList(xmppModule, m, new LimeWireNetTestModule());
    }

    public void testRosterIsPopulated() throws InterruptedException, UnknownHostException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("automatedtestfriend2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(1, rosterListener.roster.get("automatedtestfriend2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("automatedtestfriend1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(1, rosterListener2.roster.get("automatedtestfriend1@gmail.com").size());
    }
    
    public void testSetName() throws InterruptedException {
        assertEquals("buddy2", rosterListener.users.get("automatedtestfriend2@gmail.com").getName());
        rosterListener.users.get("automatedtestfriend2@gmail.com").setName("foo");
        Thread.sleep(3 * 1000);
        assertEquals("foo", rosterListener.users.get("automatedtestfriend2@gmail.com").getName());
        rosterListener.users.get("automatedtestfriend2@gmail.com").setName("buddy2");
    }
    
    public void testDetectLimePresences() throws InterruptedException, UnknownHostException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("automatedtestfriend2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(1, rosterListener.roster.get("automatedtestfriend2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("automatedtestfriend1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(1, rosterListener2.roster.get("automatedtestfriend1@gmail.com").size());
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000 * 2);
        
        assertEquals(1, rosterListener.roster.get("automatedtestfriend2@gmail.com").size());
        Presence buddy2 = rosterListener.roster.get("automatedtestfriend2@gmail.com").get(0);
        LimewireFeature limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());
        AddressFeature addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        Connectable address = (Connectable)addressFeature.getFeature();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        AuthTokenFeature authTokenFeature = (AuthTokenFeature)buddy2.getFeature(AuthTokenFeature.ID);
        assertNotNull(authTokenFeature);
        
        assertEquals(1, rosterListener2.roster.get("automatedtestfriend1@gmail.com").size());
        Presence buddy1 = rosterListener2.roster.get("automatedtestfriend1@gmail.com").get(0);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy1.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        address = (Connectable)addressFeature.getFeature();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        authTokenFeature = (AuthTokenFeature)buddy1.getFeature(AuthTokenFeature.ID);
        assertNotNull(authTokenFeature);
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
        assertEquals("automatedtestfriend2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(1, rosterListener.roster.get("automatedtestfriend2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("automatedtestfriend1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(1, rosterListener2.roster.get("automatedtestfriend1@gmail.com").size());
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000); 
        
        Presence buddy2 = rosterListener.roster.get("automatedtestfriend2@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.available, buddy2.getMode());

        XMPPService xmppService = injector.getInstance(XMPPService.class);
        for(XMPPConnection connection : xmppService.getConnections()) {
            if(connection.getConfiguration().getUsername().equals("automatedtestfriend2@gmail.com")) {
                connection.setMode(Presence.Mode.away);
            }
        }
        
        Thread.sleep(1000); 
        
        buddy2 = rosterListener.roster.get("automatedtestfriend2@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy2.getType());
        assertEquals(Presence.Mode.away, buddy2.getMode());
    }
    
    public void testChat() throws InterruptedException, XMPPException, IOException {
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        MessageReaderMock reader = new MessageReaderMock();
        Presence automatedtestfriend2 = rosterListener.roster.get("automatedtestfriend2@gmail.com").get(0);
        MessageWriter writer = automatedtestfriend2.getUser().createChat(reader);
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
        
        Presence automatedtestfriend2 = roster1.get("automatedtestfriend2@gmail.com").get(0);
        FileMetaDataImpl metaData = new FileMetaDataImpl();
        metaData.setId(new Random().nextInt() + "");
        metaData.setName("a_cool_file.txt");
        metaData.setSize(1000);
        metaData.setCreateTime(new Date());
        metaData.setDescription("cool file");
        FileOfferFeature feature = (FileOfferFeature)automatedtestfriend2.getFeature(FileOfferFeature.ID);
        assertNotNull(feature);
        FileOfferer fileOfferer = feature.getFeature();
        fileOfferer.offerFile(metaData);
        
        Thread.sleep(1000);

        List<FileMetaData> offers = fileOfferHandler.offers;
        assertEquals(1, offers.size());
        assertEquals("a_cool_file.txt", offers.get(0).getName());
    }
    
    public void testDetectAddressChanges() throws InterruptedException, UnknownHostException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("automatedtestfriend2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(1, rosterListener.roster.get("automatedtestfriend2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("automatedtestfriend1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(1, rosterListener2.roster.get("automatedtestfriend1@gmail.com").size());
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        assertEquals(1, rosterListener.roster.get("automatedtestfriend2@gmail.com").size());
        Presence buddy2 = rosterListener.roster.get("automatedtestfriend2@gmail.com").get(0);
        LimewireFeature limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy2.getType());
        AddressFeature addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        Connectable address = (Connectable)addressFeature.getFeature();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        
        assertEquals(1, rosterListener2.roster.get("automatedtestfriend1@gmail.com").size());
        Presence buddy1 = rosterListener2.roster.get("automatedtestfriend1@gmail.com").get(0);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy1.getType());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        address = (Connectable)addressFeature.getFeature();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());

        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new ConnectableImpl("200.200.200.200", 5000, false),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        assertEquals(1, rosterListener.roster.get("automatedtestfriend2@gmail.com").size());
        buddy2 = rosterListener.roster.get("automatedtestfriend2@gmail.com").get(0);
        limewireFeature = (LimewireFeature)buddy2.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy2.getType());
        addressFeature = (AddressFeature)buddy2.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        address = (Connectable)addressFeature.getFeature();
        assertEquals("200.200.200.200", address.getAddress());
        assertEquals(5000, address.getPort());
        assertEquals(false, address.isTLSCapable());

        assertEquals(1, rosterListener2.roster.get("automatedtestfriend1@gmail.com").size());
        buddy1 = rosterListener2.roster.get("automatedtestfriend1@gmail.com").get(0);
        limewireFeature = (LimewireFeature)buddy1.getFeature(LimewireFeature.ID);
        assertNotNull(limewireFeature);
        assertEquals(Presence.Type.available, buddy1.getType());
        addressFeature = (AddressFeature)buddy1.getFeature(AddressFeature.ID);
        assertNotNull(addressFeature);
        address = (Connectable)addressFeature.getFeature();
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