package org.limewire.xmpp.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.limewire.inject.AbstractModule;
import org.limewire.lifecycle.ServiceTestCase;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.LimeWireNetTestModule;
import org.limewire.net.address.Address;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.DirectConnectionAddress;
import org.limewire.net.address.DirectConnectionAddressImpl;
import org.limewire.xmpp.client.impl.XMPPConnectionConfigurationImpl;
import org.limewire.xmpp.client.impl.XMPPException;
import org.limewire.xmpp.client.impl.messages.FileMetaDataImpl;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.LimePresence;
import org.limewire.xmpp.client.service.MessageWriter;
import org.limewire.xmpp.client.service.Presence;
import org.limewire.xmpp.client.service.XMPPConnection;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.XMPPService;
import org.xmlpull.v1.XmlPullParserException;

import com.google.inject.Module;
import com.google.inject.TypeLiteral;

public class XMPPServiceTest extends ServiceTestCase {
    protected RosterListenerImpl rosterListener;
    protected RosterListenerImpl rosterListener2;
    protected AddressEventTestBroadcaster addressEventBroadcaster;

    public XMPPServiceTest(String name) {
        super(name);
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    protected void setUp() throws Exception {
        super.setUp();  
        Thread.sleep(10 * 1000); // allow login, roster, presence, library messages to be
                                // sent, received   
                                // TODO wait()/notify()
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected List<Module> getServiceModules() {
        rosterListener = new RosterListenerImpl();
        rosterListener2 = new RosterListenerImpl();
        final XMPPConnectionConfiguration configuration = new XMPPConnectionConfigurationImpl("limebuddy1@gmail.com",
                "limebuddy123", "talk.google.com", 5222, "gmail.com", rosterListener);
        final XMPPConnectionConfiguration configuration2 = new XMPPConnectionConfigurationImpl("limebuddy2@gmail.com",
                "limebuddy234", "talk.google.com", 5222, "gmail.com", rosterListener2);
        Module xmppModule = new LimeWireXMPPModule();
        addressEventBroadcaster = new AddressEventTestBroadcaster();
        Module m = new AbstractModule() {
            protected void configure() {
                bind(new TypeLiteral<ListenerSupport<AddressEvent>>(){}).toInstance(addressEventBroadcaster);
                bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).toProvider(new XMPPConnectionConfigurationListProvider(configuration, configuration2));
                bind(FileOfferHandler.class).to(FileOfferHandlerImpl.class);
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
    

    public void testDetectLimePresences() throws InterruptedException, UnknownHostException {
        assertEquals(1, rosterListener.roster.size());
        assertEquals("limebuddy2@gmail.com", rosterListener.roster.keySet().iterator().next());
        assertEquals(0, rosterListener.roster.get("limebuddy2@gmail.com").size());
        
        assertEquals(1, rosterListener2.roster.size());
        assertEquals("limebuddy1@gmail.com", rosterListener2.roster.keySet().iterator().next());
        assertEquals(0, rosterListener2.roster.get("limebuddy1@gmail.com").size());
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new DirectConnectionAddressImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        assertEquals(1, rosterListener.roster.get("limebuddy2@gmail.com").size());
        assertTrue(rosterListener.roster.get("limebuddy2@gmail.com").get(0) instanceof LimePresence);
        LimePresence buddy2 = (LimePresence)rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy2.getType());
        DirectConnectionAddress address = (DirectConnectionAddress)buddy2.getAddress();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
        
        assertEquals(1, rosterListener2.roster.get("limebuddy1@gmail.com").size());
        assertTrue(rosterListener2.roster.get("limebuddy1@gmail.com").get(0) instanceof LimePresence);
        LimePresence buddy1 = (LimePresence)rosterListener2.roster.get("limebuddy1@gmail.com").get(0);
        assertEquals(Presence.Type.available, buddy1.getType());
        address = (DirectConnectionAddress)buddy1.getAddress();
        assertEquals("199.199.199.199", address.getAddress());
        assertEquals(2048, address.getPort());
        assertEquals(true, address.isTLSCapable());
    }
    
    public void testUserLogout() throws InterruptedException {
        XMPPService service = injector.getInstance(XMPPService.class);
        List<XMPPConnection> connections = service.getConnections();
        assertTrue(connections.get(0).isLoggedIn());
        connections.get(0).logout();
        assertFalse(connections.get(0).isLoggedIn());
    }
    
    public void testChat() throws InterruptedException, XMPPException, IOException {
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new DirectConnectionAddressImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        MessageReaderImpl reader = new MessageReaderImpl();
        Presence limeBuddy2 = rosterListener.roster.get("limebuddy2@gmail.com").get(0);
        MessageWriter writer = limeBuddy2.createChat(reader);
        writer.writeMessage("hello world");
       
        Thread.sleep(2 * 1000);
        
        IncomingChatListenerImpl incomingChatListener2 = rosterListener2.listener;
        MessageWriter writer2 = incomingChatListener2.writer;
        writer2.writeMessage("goodbye world");
        
        Thread.sleep(2 * 1000);
        
        assertEquals(1, incomingChatListener2.reader.messages.size());
        assertEquals("hello world", incomingChatListener2.reader.messages.get(0));
        
        assertEquals(1, reader.messages.size());
        assertEquals("goodbye world", reader.messages.get(0)); 
        
    }
    
    public void testSendFile() throws InterruptedException, IOException, XmlPullParserException {
        
        HashMap<String, ArrayList<Presence>> roster1 = rosterListener.roster;
        
        addressEventBroadcaster.listeners.broadcast(new AddressEvent(new DirectConnectionAddressImpl("199.199.199.199", 2048, true),
                Address.EventType.ADDRESS_CHANGED));
        
        Thread.sleep(1000);
        
        LimePresence limebuddy2 = ((LimePresence)roster1.get("limebuddy2@gmail.com").get(0));
        FileMetaDataImpl metaData = new FileMetaDataImpl();
        metaData.setId(new Random().nextInt() + "");
        metaData.setName("a_cool_file.txt");
        metaData.setSize(1000);
        metaData.setCreateTime(new Date());
        metaData.setDescription("cool file");
        limebuddy2.sendFile(metaData);
        
        Thread.sleep(1000);
        
        List<FileMetaData> offers = ((FileOfferHandlerImpl) injector.getInstance(FileOfferHandler.class)).offers;
        assertEquals(1, offers.size());
        assertEquals("a_cool_file.txt", offers.get(0).getName());
    }

}
