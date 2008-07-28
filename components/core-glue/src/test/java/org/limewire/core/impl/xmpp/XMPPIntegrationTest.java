package org.limewire.core.impl.xmpp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.BasicConfigurator;
import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.rudp.RUDPUtils;
import org.limewire.util.TestUtils;
import org.limewire.xmpp.client.LimeWireXMPPModule;
import org.limewire.xmpp.client.RosterListenerImpl;
import org.limewire.xmpp.client.XMPPConnectionConfigurationListProvider;
import org.limewire.xmpp.client.impl.XMPPConnectionConfigurationImpl;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.LimePresence;
import org.limewire.xmpp.client.service.Presence;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.BrowseHostHandler;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.net.address.gnutella.PushProxyHolePunchAddress;
import com.limegroup.gnutella.net.address.gnutella.PushProxyMediatorAddress;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.util.FileManagerTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class XMPPIntegrationTest extends LimeTestCase {
    protected RosterListenerImpl rosterListener;
    protected RosterListenerImpl rosterListener2;
    protected PushEndpointFactory pushEndpointFactory;
    
    private Injector injector;
    private QueryReplyHandler queryReplyHandler;
    private FileManager fileManager;
    private static CountDownLatch started;

    public XMPPIntegrationTest(String name) {
        super(name);
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    protected void setUp() throws Exception {
        super.setUp();
        started = new CountDownLatch(1);
        rosterListener = new RosterListenerImpl();
        rosterListener2 = new RosterListenerImpl();
        XMPPConnectionConfiguration configuration = new XMPPConnectionConfigurationImpl("limebuddy1@gmail.com",
                "limebuddy123", "talk.google.com", 5222, "gmail.com", rosterListener);
        XMPPConnectionConfiguration configuration2 = new XMPPConnectionConfigurationImpl("limebuddy2@gmail.com", 
                "limebuddy234", "talk.google.com", 5222, "gmail.com", rosterListener2);
        Module xmppModule = new LimeWireXMPPModule(new XMPPConnectionConfigurationListProvider(configuration, configuration2),
                new FileOfferHandlerImpl());
        
        String directoryName = "com/limegroup/gnutella";
        File sharedDirectory = TestUtils.getResourceFile(directoryName);
        sharedDirectory = sharedDirectory.getCanonicalFile();
        assertTrue("Could not find directory: " + directoryName,
                sharedDirectory.isDirectory());

        File[] testFiles = sharedDirectory.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory() && file.getName().endsWith(".class");
            }
        });
        assertNotNull("No files to test against", testFiles);
        assertGreaterThan("Not enough files to test against", 50,
                testFiles.length);


        SharingSettings.EXTENSIONS_TO_SHARE.setValue("class");
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(Collections
                .singleton(sharedDirectory));
        
        //ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        injector = LimeTestUtils.createInjectorAndStart(xmppModule, new AbstractModule() {
            @Override
            protected void configure() {
                //bind(ReplyHandler.class).annotatedWith(Names.named("forMeReplyHandler")).to(QueryReplyHandler.class);
                //bind(StartedWatcher.class);
                //bind(NetworkManager.class).toInstance(new NetworkManagerStub());
                //bind(ConnectionManager.class).toInstance(new ConnectionManagerStub());
            }
        });
        
        fileManager = injector.getInstance(FileManager.class);

        FileManagerTestUtils.waitForLoad(fileManager, 100000);

        assertGreaterThan(0, fileManager.getSharedFileList().getNumFiles());
        
        pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        //queryReplyHandler = (QueryReplyHandler) injector.getInstance(Key.get(ReplyHandler.class, Names.named("forMeReplyHandler")));
        
        Thread.sleep(5 * 1000);
    }

    public void testBrowseHost() throws IOException, BadPacketException, InterruptedException {
//        final CountDownLatch connectedLatch = new CountDownLatch(1);
//        injector.getInstance(NetworkManager.class).addListener(new EventListener<NetworkManagerEvent>() {
//            public void handleEvent(NetworkManagerEvent event) {
//                if(event.getType().equals(NetworkManager.EventType.EXTERNAL_ADDRESS_CHANGE)) {
//                    connectedLatch.countDown();    
//                }
//            }
//        });
//        connectedLatch.await();
        HashMap<String, ArrayList<Presence>> roster2 = rosterListener2.roster;
        while(roster2.get("limebuddy1@gmail.com").size() == 0) {
            Thread.sleep(1000);
        }

        LimePresence presence = (LimePresence) roster2.get("limebuddy1@gmail.com").get(0);
        
        //Thread.sleep(5 * 60 *1000);
        
        byte [] guidBytes = null;
        Set<? extends IpPort> proxies = null;
        byte features = ~PushEndpoint.PPTLS_BINARY;
        int version = 0;
        IpPort directAddrss = null;
        
        if(presence.getAddress() instanceof PushProxyMediatorAddress) {
            PushProxyMediatorAddress address = (PushProxyMediatorAddress) presence.getAddress();
            guidBytes = address.getClientID().bytes();
            proxies = address.getPushProxies();
        } else if(presence.getAddress() instanceof PushProxyHolePunchAddress) {
            PushProxyHolePunchAddress address = (PushProxyHolePunchAddress) presence.getAddress();
            guidBytes = ((PushProxyMediatorAddress)address.getMediatorAddress()).getClientID().bytes();
            proxies = ((PushProxyMediatorAddress)address.getMediatorAddress()).getPushProxies();
            version = address.getVersion();
            BitNumbers bn = HTTPHeaderUtils.getTLSIndices(proxies, (Math.min(proxies.size(), PushEndpoint.MAX_PROXIES)));
            features = bn.toByteArray()[0] |= PushEndpoint.PPTLS_BINARY;
            directAddrss = address.getDirectConnectionAddress();
        }
        
        
        PushEndpoint pushEndpoint = pushEndpointFactory.createPushEndpoint(guidBytes, proxies, features, version, directAddrss);
        InetSocketAddress inetSocketAddress = pushEndpoint.getInetSocketAddress();
        Connectable host = inetSocketAddress != null ? new ConnectableImpl(inetSocketAddress, false) : null;
        
        
        GUID guid = new GUID(GUID.makeGuid());
        
        BrowseHostHandler bhh = injector.getInstance(SearchServices.class).doAsynchronousBrowseHost(
                                    host, guid, new GUID(pushEndpoint.getClientGUID()), pushEndpoint.getProxies(),
                                    pushEndpoint.getFWTVersion() >= RUDPUtils.VERSION);
     
        
        //while(queryReplyHandler.replies.size() == 0) {
        //    Thread.sleep(1000);
        //}
        
        Thread.sleep(5 * 60 * 1000);
        
        List<String> files = new ArrayList<String>();
        for(QueryReply reply : queryReplyHandler.replies) {
            Response[] results = reply.getResultsArray();
            for (Response result : results) {
                files.add(result.getName());
                assertTrue("Expected .class or LimeWire file, got: " + result.getName(),
                        result.getName().endsWith(".class") || result.getName().toLowerCase().startsWith("limewire"));
            }
        }

        assertEquals(fileManager.getSharedFileList().getNumFiles(), files.size());

        for (Iterator<FileDesc> it = fileManager.getIndexingIterator(); it.hasNext();) {
            FileDesc result = it.next();
            boolean contained = files.remove(result.getFileName());
            assertTrue("File is missing in browse response: "
                    + result.getFileName(), contained);
        }
        assertTrue("Browse returned more results than shared: " + files,
                files.isEmpty());
    }
    
    @Singleton
    public static class QueryReplyHandler extends ReplyHandlerStub {

        @Inject
        public QueryReplyHandler(){}

        ArrayList<QueryReply> replies = new ArrayList<QueryReply>();

        @Override
        public void handleQueryReply(QueryReply queryReply, ReplyHandler receivingConnection) {
            replies.add(queryReply);
        }

    }
    
    class FileOfferHandlerImpl implements FileOfferHandler, Provider<FileOfferHandler> {
        public boolean fileOfferred(FileMetaData f) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public FileOfferHandler get() {
            return this;
        }
    }
}
