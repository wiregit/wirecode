package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.activation.api.ActivationManager;
import org.limewire.collection.Tuple;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.UrnSet;
import org.limewire.net.SocketsManager;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.library.SharedFilesKeywordIndex;
import com.limegroup.gnutella.messagehandlers.InspectionRequestHandler;
import com.limegroup.gnutella.messagehandlers.LimeACKHandler;
import com.limegroup.gnutella.messagehandlers.OOBHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.QuerySettings;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

public class StandardMessageRouterTest extends LimeTestCase {

    @Inject
    private TestStandardMessageRouter messageRouter;
    
    @Inject
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    
    @Inject
    private QueryRequestFactory queryRequestFactory;
    
    @Inject
    private NetworkManagerStub networkManagerStub;

    private Response[] largeResponses;

    private Response[] smallResponses;

    private QueryRequest queryRequest;

    private Mockery context;
    
    private static final String LARGE_TORRENT_XML = "<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"TA3EUOCICPEIEULAYPWQTWKO2SVHETG6\" trackers=\"http://localhost/announce\" name=\"src\" filepaths=\"/CVS/Entries///CVS/Repository///CVS/Root///CVS/Tag///CVS/Template///main/CVS/Entries///main/CVS/Repository///main/CVS/Root///main/CVS/Tag///main/CVS/Template///main/java/CVS/Entries///main/java/CVS/Repository///main/java/CVS/Root///main/java/CVS/Tag///main/java/CVS/Template///main/java/org/CVS/Entries///main/java/org/CVS/Repository///main/java/org/CVS/Root///main/java/org/CVS/Tag///main/java/org/CVS/Template///main/java/org/limewire/CVS/Entries///main/java/org/limewire/CVS/Repository///main/java/org/limewire/CVS/Root///main/java/org/limewire/CVS/Tag///main/java/org/limewire/CVS/Template///main/java/org/limewire/friend/CVS/Entries///main/java/org/limewire/friend/CVS/Repository///main/java/org/limewire/friend/CVS/Root///main/java/org/limewire/friend/CVS/Tag///main/java/org/limewire/friend/CVS/Template///main/java/org/limewire/friend/impl/FriendListListeners.java///main/java/org/limewire/friend/impl/LimeWireFriendXmppModule.java///main/java/org/limewire/friend/impl/CVS/Entries///main/java/org/limewire/friend/impl/CVS/Repository///main/java/org/limewire/friend/impl/CVS/Root///main/java/org/limewire/friend/impl/CVS/Tag///main/java/org/limewire/friend/impl/CVS/Template///main/java/org/limewire/xmpp/client/LimeWireXMPPModule.java///main/java/org/limewire/xmpp/client/CVS/Entries///main/java/org/limewire/xmpp/client/CVS/Repository///main/java/org/limewire/xmpp/client/CVS/Root///main/java/org/limewire/xmpp/client/CVS/Tag///main/java/org/limewire/xmpp/client/CVS/Template///main/java/org/limewire/xmpp/client/impl/ConnectionConfigurationFactory.java///main/java/org/limewire/xmpp/client/impl/DNSConnectionConfigurationFactory.java///main/java/org/limewire/xmpp/client/impl/FallbackConnectionConfigurationFactory.java///main/java/org/limewire/xmpp/client/impl/IdleStatusMonitor.java///main/java/org/limewire/xmpp/client/impl/IdleStatusMonitorFactory.java///main/java/org/limewire/xmpp/client/impl/IdleTime.java///main/java/org/limewire/xmpp/client/impl/IdleTimeImpl.java///main/java/org/limewire/xmpp/client/impl/Password.java///main/java/org/limewire/xmpp/client/impl/PasswordManagerImpl.java///main/java/org/limewire/xmpp/client/impl/PresenceImpl.java///main/java/org/limewire/xmpp/client/impl/ReconnectionManager.java///main/java/org/limewire/xmpp/client/impl/SubscriptionListener.java///main/java/org/limewire/xmpp/client/impl/XMPPConnectionFactoryImpl.java///main/java/org/limewire/xmpp/client/impl/XMPPConnectionImplFactory.java///main/java/org/limewire/xmpp/client/impl/XMPPFriendConnectionImpl.java///main/java/org/limewire/xmpp/client/impl/XMPPFriendImpl.java///main/java/org/limewire/xmpp/client/impl/CVS/Entries///main/java/org/limewire/xmpp/client/impl/CVS/Repository///main/java/org/limewire/xmpp/client/impl/CVS/Root///main/java/org/limewire/xmpp/client/impl/CVS/Tag///main/java/org/limewire/xmpp/client/impl/CVS/Template///main/java/org/limewire/xmpp/client/impl/features/NoSaveFeatureInitializer.java///main/java/org/limewire/xmpp/client/impl/features/CVS/Entries///main/java/org/limewire/xmpp/client/impl/features/CVS/Repository///main/java/org/limewire/xmpp/client/impl/features/CVS/Root///main/java/org/limewire/xmpp/client/impl/features/CVS/Tag///main/java/org/limewire/xmpp/client/impl/features/CVS/Template///main/java/org/limewire/xmpp/client/impl/messages/InvalidIQException.java///main/java/org/limewire/xmpp/client/impl/messages/address/AddressIQ.java///main/java/org/limewire/xmpp/client/impl/messages/address/AddressIQListener.java///main/java/org/limewire/xmpp/client/impl/messages/address/AddressIQListenerFactory.java///main/java/org/limewire/xmpp/client/impl/messages/address/AddressIQProvider.java///main/java/org/limewire/xmpp/client/impl/messages/address/CVS/Entries///main/java/org/limewire/xmpp/client/impl/messages/address/CVS/Repository///main/java/org/limewire/xmpp/client/impl/messages/address/CVS/Root///main/java/org/limewire/xmpp/client/impl/messages/address/CVS/Tag///main/java/org/limewire/xmpp/client/impl/messages/address/CVS/Template///main/java/org/limewire/xmpp/client/impl/messages/authtoken/AuthTokenIQ.java///main/java/org/limewire/xmpp/client/impl/messages/authtoken/AuthTokenIQListener.java///main/java/org/limewire/xmpp/client/impl/messages/authtoken/AuthTokenIQListenerFactory.java///main/java/org/limewire/xmpp/client/impl/messages/authtoken/AuthTokenIQProvider.java///main/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Entries///main/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Repository///main/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Root///main/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Tag///main/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Template///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/ConnectBackRequestIQ.java///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/ConnectBackRequestIQListener.java///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/ConnectBackRequestIQListenerFactory.java///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/ConnectBackRequestIQProvider.java///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Entries///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Repository///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Root///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Tag///main/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Template///main/java/org/limewire/xmpp/client/impl/messages/CVS/Entries///main/java/org/limewire/xmpp/client/impl/messages/CVS/Repository///main/java/org/limewire/xmpp/client/impl/messages/CVS/Root///main/java/org/limewire/xmpp/client/impl/messages/CVS/Tag///main/java/org/limewire/xmpp/client/impl/messages/CVS/Template///main/java/org/limewire/xmpp/client/impl/messages/discoinfo/DiscoInfoListener.java///main/java/org/limewire/xmpp/client/impl/messages/discoinfo/CVS/Entries///main/java/org/limewire/xmpp/client/impl/messages/discoinfo/CVS/Repository///main/java/org/limewire/xmpp/client/impl/messages/discoinfo/CVS/Root///main/java/org/limewire/xmpp/client/impl/messages/discoinfo/CVS/Tag///main/java/org/limewire/xmpp/client/impl/messages/discoinfo/CVS/Template///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/FileTransferIQ.java///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/FileTransferIQListener.java///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/FileTransferIQListenerFactory.java///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/XMPPFileMetaData.java///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Entries///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Repository///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Root///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Tag///main/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Template///main/java/org/limewire/xmpp/client/impl/messages/library/LibraryChangedIQ.java///main/java/org/limewire/xmpp/client/impl/messages/library/LibraryChangedIQListener.java///main/java/org/limewire/xmpp/client/impl/messages/library/LibraryChangedIQListenerFactory.java///main/java/org/limewire/xmpp/client/impl/messages/library/CVS/Entries///main/java/org/limewire/xmpp/client/impl/messages/library/CVS/Repository///main/java/org/limewire/xmpp/client/impl/messages/library/CVS/Root///main/java/org/limewire/xmpp/client/impl/messages/library/CVS/Tag///main/java/org/limewire/xmpp/client/impl/messages/library/CVS/Template///main/java/org/limewire/xmpp/client/impl/messages/nosave/NoSaveIQ.java///main/java/org/limewire/xmpp/client/impl/messages/nosave/NoSaveIQListener.java///main/java/org/limewire/xmpp/client/impl/messages/nosave/CVS/Entries///main/java/org/limewire/xmpp/client/impl/messages/nosave/CVS/Repository///main/java/org/limewire/xmpp/client/impl/messages/nosave/CVS/Root///main/java/org/limewire/xmpp/client/impl/messages/nosave/CVS/Tag///main/java/org/limewire/xmpp/client/impl/messages/nosave/CVS/Template///main/java/org/limewire/xmpp/CVS/Entries///main/java/org/limewire/xmpp/CVS/Repository///main/java/org/limewire/xmpp/CVS/Root///main/java/org/limewire/xmpp/CVS/Tag///main/java/org/limewire/xmpp/CVS/Template///test/CVS/Entries///test/CVS/Repository///test/CVS/Root///test/CVS/Tag///test/CVS/Template///test/java/CVS/Entries///test/java/CVS/Repository///test/java/CVS/Root///test/java/CVS/Tag///test/java/CVS/Template///test/java/org/CVS/Entries///test/java/org/CVS/Repository///test/java/org/CVS/Root///test/java/org/CVS/Tag///test/java/org/CVS/Template///test/java/org/limewire/CVS/Entries///test/java/org/limewire/CVS/Repository///test/java/org/limewire/CVS/Root///test/java/org/limewire/CVS/Tag///test/java/org/limewire/CVS/Template///test/java/org/limewire/xmpp/client/CVS/Entries///test/java/org/limewire/xmpp/client/CVS/Repository///test/java/org/limewire/xmpp/client/CVS/Root///test/java/org/limewire/xmpp/client/CVS/Tag///test/java/org/limewire/xmpp/client/CVS/Template///test/java/org/limewire/xmpp/client/impl/AddressEventTestBroadcaster.java///test/java/org/limewire/xmpp/client/impl/EmptyJabberSettings.java///test/java/org/limewire/xmpp/client/impl/FallbackConnectionConfigurationFactoryTest.java///test/java/org/limewire/xmpp/client/impl/FileOfferHandlerMock.java///test/java/org/limewire/xmpp/client/impl/FriendConnectionConfigurationMock.java///test/java/org/limewire/xmpp/client/impl/IdleStatusMonitorTest.java///test/java/org/limewire/xmpp/client/impl/IdleTimeImplTest.java///test/java/org/limewire/xmpp/client/impl/IncomingChatListenerMock.java///test/java/org/limewire/xmpp/client/impl/LimeWireXMPPTestModule.java///test/java/org/limewire/xmpp/client/impl/MessageReaderMock.java///test/java/org/limewire/xmpp/client/impl/MockScheduledExecutorService.java///test/java/org/limewire/xmpp/client/impl/PasswordManagerImplTest.java///test/java/org/limewire/xmpp/client/impl/PasswordTest.java///test/java/org/limewire/xmpp/client/impl/RosterListenerMock.java///test/java/org/limewire/xmpp/client/impl/XMPPAddressRegistryTest.java///test/java/org/limewire/xmpp/client/impl/XMPPAddressSerializerTest.java///test/java/org/limewire/xmpp/client/impl/XmppBaseTestCase.java///test/java/org/limewire/xmpp/client/impl/XMPPConnectionListenerMock.java///test/java/org/limewire/xmpp/client/impl/XmppFriendSubscriptionTest.java///test/java/org/limewire/xmpp/client/impl/XMPPServiceTest.java///test/java/org/limewire/xmpp/client/impl/CVS/Entries///test/java/org/limewire/xmpp/client/impl/CVS/Repository///test/java/org/limewire/xmpp/client/impl/CVS/Root///test/java/org/limewire/xmpp/client/impl/CVS/Tag///test/java/org/limewire/xmpp/client/impl/CVS/Template///test/java/org/limewire/xmpp/client/impl/features/LibraryChangedNotifierFeatureInitializerTest.java///test/java/org/limewire/xmpp/client/impl/features/CVS/Entries///test/java/org/limewire/xmpp/client/impl/features/CVS/Repository///test/java/org/limewire/xmpp/client/impl/features/CVS/Root///test/java/org/limewire/xmpp/client/impl/features/CVS/Tag///test/java/org/limewire/xmpp/client/impl/features/CVS/Template///test/java/org/limewire/xmpp/client/impl/messages/IQTestUtils.java///test/java/org/limewire/xmpp/client/impl/messages/address/AddressIQTest.java///test/java/org/limewire/xmpp/client/impl/messages/address/CVS/Entries///test/java/org/limewire/xmpp/client/impl/messages/address/CVS/Repository///test/java/org/limewire/xmpp/client/impl/messages/address/CVS/Root///test/java/org/limewire/xmpp/client/impl/messages/address/CVS/Tag///test/java/org/limewire/xmpp/client/impl/messages/address/CVS/Template///test/java/org/limewire/xmpp/client/impl/messages/authtoken/AuthTokenIQTest.java///test/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Entries///test/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Repository///test/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Root///test/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Tag///test/java/org/limewire/xmpp/client/impl/messages/authtoken/CVS/Template///test/java/org/limewire/xmpp/client/impl/messages/connectrequest/ConnectBackRequestIQTest.java///test/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Entries///test/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Repository///test/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Root///test/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Tag///test/java/org/limewire/xmpp/client/impl/messages/connectrequest/CVS/Template///test/java/org/limewire/xmpp/client/impl/messages/CVS/Entries///test/java/org/limewire/xmpp/client/impl/messages/CVS/Repository///test/java/org/limewire/xmpp/client/impl/messages/CVS/Root///test/java/org/limewire/xmpp/client/impl/messages/CVS/Tag///test/java/org/limewire/xmpp/client/impl/messages/CVS/Template///test/java/org/limewire/xmpp/client/impl/messages/filetransfer/FileTransferIQListenerTest.java///test/java/org/limewire/xmpp/client/impl/messages/filetransfer/FileTransferIQTest.java///test/java/org/limewire/xmpp/client/impl/messages/filetransfer/XMPPFileMetaDataTest.java///test/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Entries///test/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Repository///test/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Root///test/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Tag///test/java/org/limewire/xmpp/client/impl/messages/filetransfer/CVS/Template///test/java/org/limewire/xmpp/client/impl/messages/library/LibraryChangedIQListenerTest.java///test/java/org/limewire/xmpp/client/impl/messages/library/CVS/Entries///test/java/org/limewire/xmpp/client/impl/messages/library/CVS/Repository///test/java/org/limewire/xmpp/client/impl/messages/library/CVS/Root///test/java/org/limewire/xmpp/client/impl/messages/library/CVS/Tag///test/java/org/limewire/xmpp/client/impl/messages/library/CVS/Template///test/java/org/limewire/xmpp/CVS/Entries///test/java/org/limewire/xmpp/CVS/Repository///test/java/org/limewire/xmpp/CVS/Root///test/java/org/limewire/xmpp/CVS/Tag///test/java/org/limewire/xmpp/CVS/Template\" filesizes=\"22 20 30 23 0 11 25 30 23 0 10 30 30 23 0 15 34 30 23 0 24 43 30 23 0 11 50 30 23 0 4280 242 167 55 30 23 0 6146 90 55 30 23 0 1440 6182 1596 2219 119 131 380 4553 2945 2574 3129 6274 5775 463 33598 13938 1321 60 30 23 0 5033 86 69 30 23 0 391 3248 2174 306 959 310 77 30 23 0 2424 1868 240 775 318 79 30 23 0 5453 3455 263 807 353 84 30 23 0 191 69 30 23 0 7872 80 79 30 23 0 4207 2889 249 1036 324 82 30 23 0 1464 3501 248 254 77 30 23 0 3749 7830 146 76 30 23 0 13 48 30 23 0 11 25 30 23 0 10 30 30 23 0 15 34 30 23 0 11 43 30 23 0 11 55 30 23 0 740 339 5047 797 2140 5135 1149 529 251 580 3418 3845 2430 5569 1227 1175 4835 591 12291 21562 1687 60 30 23 0 2330 106 69 30 23 0 461 3193 75 77 30 23 0 1682 77 79 30 23 0 3707 86 84 30 23 0 155 69 30 23 0 2027 3699 3976 246 82 30 23 0 1440 90 77 30 23 0 13 48 30 23 0\"/></torrents>";

    private UploadManager uploadManager;

    private ReplyHandler replyHandler;

    public static Test suite() {
        return buildTestSuite(StandardMessageRouterTest.class);
    }  
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        uploadManager = context.mock(UploadManager.class);
        replyHandler = context.mock(ReplyHandler.class);

        context.checking(new Expectations() {{
            allowing(uploadManager).isServiceable();
            will(returnValue(true));
            allowing(uploadManager).mayBeServiceable();
            will(returnValue(true));
            allowing(uploadManager).hadSuccesfulUpload();
            will(returnValue(true));
            allowing(uploadManager).measuredUploadSpeed();
            will(returnValue(500));
            allowing(replyHandler).getInetAddress();
            will(returnValue(InetAddress.getLocalHost()));
        }});

        
        LimeTestUtils.createInjectorNonEagerly(LimeTestUtils.createModule(this), NetworkManagerStub.MODULE,
                TestUtils.bind(MessageRouter.class).to(TestStandardMessageRouter.class),
                TestUtils.bind(UploadManager.class).toInstances(uploadManager));
        largeResponses = new Response[] {
                new ResponseImpl(1, 100, "large1.torrent", "large1.torrent".length(), new UrnSet(UrnHelper.SHA1), limeXMLDocumentFactory.createLimeXMLDocument(LARGE_TORRENT_XML), new IpPortSet(), 10945, DataUtils.EMPTY_BYTE_ARRAY, null, true),
                new ResponseImpl(2, 100, "large2.torrent", "large2.torrent".length(), new UrnSet(UrnHelper.SHA1), limeXMLDocumentFactory.createLimeXMLDocument(LARGE_TORRENT_XML), new IpPortSet(), 10945, DataUtils.EMPTY_BYTE_ARRAY, null, true),
                new ResponseImpl(3, 100, "large3.torrent", "large3.torrent".length(), new UrnSet(UrnHelper.SHA1), limeXMLDocumentFactory.createLimeXMLDocument(LARGE_TORRENT_XML), new IpPortSet(), 10945, DataUtils.EMPTY_BYTE_ARRAY, null, true),
        };
        smallResponses = new Response[] {
                new ResponseImpl(1, 100, "small1.torrent", "small1.torrent".length(), new UrnSet(UrnHelper.SHA1), limeXMLDocumentFactory.createLimeXMLDocument("<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"W2RALTKDHY4MHYPUKBDG6CRKHGWAQZZH\" trackers=\"\" name=\"friend\" filepaths=\"/impl/util/CVS/Template\" filesizes=\"501\"/></torrents>"), new IpPortSet(), 10945, DataUtils.EMPTY_BYTE_ARRAY, null, true),
                new ResponseImpl(1, 100, "small2.torrent", "small2.torrent".length(), new UrnSet(UrnHelper.SHA1), limeXMLDocumentFactory.createLimeXMLDocument("<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"W2RALTKDHY4MHYPUKBDG6CRKHGWAQZZH\" trackers=\"\" name=\"friend\" filepaths=\"/impl/util/CVS/Template\" filesizes=\"501\"/></torrents>"), new IpPortSet(), 10945, DataUtils.EMPTY_BYTE_ARRAY, null, true),
                new ResponseImpl(1, 100, "small3.torrent", "small3.torrent".length(), new UrnSet(UrnHelper.SHA1), limeXMLDocumentFactory.createLimeXMLDocument("<?xml version=\"1.0\"?><torrents xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/torrent.xsd\"><torrent infohash=\"W2RALTKDHY4MHYPUKBDG6CRKHGWAQZZH\" trackers=\"\" name=\"friend\" filepaths=\"/impl/util/CVS/Template\" filesizes=\"501\"/></torrents>"), new IpPortSet(), 10945, DataUtils.EMPTY_BYTE_ARRAY, null, true),
        };
        queryRequest = queryRequestFactory.createQuery("torrent");
        networkManagerStub.setCanReceiveSolicited(true);
    }
    
    public void testSplitLargeResponsesReturnsSameForSmallResponses() {
        Tuple<Response[], List<QueryReply>> split = messageRouter.splitLargeResponses(queryRequest, smallResponses);
        assertSame(smallResponses, split.getFirst());
        assertLessThan(StandardMessageRouter.UDP_MTU, messageRouter.createSingleResponseQueryReply(split.getFirst()[0], queryRequest).getPayload().length + StandardMessageRouter.GNUTELLA_HEADER_SIZE);
        assertLessThan(StandardMessageRouter.UDP_MTU, messageRouter.createSingleResponseQueryReply(split.getFirst()[1], queryRequest).getPayload().length + StandardMessageRouter.GNUTELLA_HEADER_SIZE);
        assertLessThan(StandardMessageRouter.UDP_MTU, messageRouter.createSingleResponseQueryReply(split.getFirst()[2], queryRequest).getPayload().length + StandardMessageRouter.GNUTELLA_HEADER_SIZE);
    }
    
    public void testSplitLargeResponsesReturnsEmptySmallResponses() {
        Tuple<Response[], List<QueryReply>> split = messageRouter.splitLargeResponses(queryRequest, largeResponses);
        assertEquals(0, split.getFirst().length);
        assertEquals(3, split.getSecond().size());
        assertGreaterThan(StandardMessageRouter.UDP_MTU, split.getSecond().get(0).getPayload().length + StandardMessageRouter.GNUTELLA_HEADER_SIZE);
        assertGreaterThan(StandardMessageRouter.UDP_MTU, split.getSecond().get(1).getPayload().length + StandardMessageRouter.GNUTELLA_HEADER_SIZE);
        assertGreaterThan(StandardMessageRouter.UDP_MTU, split.getSecond().get(2).getPayload().length + StandardMessageRouter.GNUTELLA_HEADER_SIZE);
    }
    
    public void testSplitLargeResponsesWithSmallAndLargeResponses() throws Exception {
        Tuple<Response[], List<QueryReply>> split = messageRouter.splitLargeResponses(queryRequest, smallResponses[0], largeResponses[0], smallResponses[1], smallResponses[2], largeResponses[1], largeResponses[2]);
        assertEquals(3, split.getFirst().length);
        assertEquals(3, split.getSecond().size());
        assertEquals(smallResponses, split.getFirst());
        List<QueryReply> largeReplies = split.getSecond();
        assertEquals(largeResponses[0].getName(), largeReplies.get(0).getResultsArray()[0].getName());
        assertEquals(largeResponses[1].getName(), largeReplies.get(1).getResultsArray()[0].getName());
        assertEquals(largeResponses[2].getName(), largeReplies.get(2).getResultsArray()[0].getName());
    }
    
    public void testSplitLargeResponsesProducesNoFalseNegatives() throws Exception {
        String template = "<?xml version=\"1.0\"?><images xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/image.xsd\"><image title=\"hello world\" description=\"{0}\"/></images>";
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7\u30b9\u30c6\u30e0\u30d5\u30a1\u30a4\u30eb\u30b7.\u30b9\u30c6\u30e0";
        Random r = new Random();
        int falseNegatives = 0;
        int largeReplies = 0;
        for (int i = 0; i < 2000; i++) {
            char[] description = new char[i];
            for (int j = 0; j < i; j++) {
                description[j] = characters.charAt(r.nextInt(characters.length()));
            }
            String desc = new String(description);
            String xml = MessageFormat.format(template, desc);
            LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(xml);
            ResponseImpl response = new ResponseImpl(1, 100, "small1.video", "small1.video".length(), new UrnSet(UrnHelper.SHA1), doc, new IpPortSet(new IpPortImpl("111.44.4.4:8000")), 10945, DataUtils.EMPTY_BYTE_ARRAY, null, true);
            Tuple<Response[], List<QueryReply>> split = messageRouter.splitLargeResponses(queryRequest, response);
            QueryReply queryReply = messageRouter.createSingleResponseQueryReply(response, queryRequest);
            if (queryReply.getPayload().length + StandardMessageRouter.GNUTELLA_HEADER_SIZE > StandardMessageRouter.UDP_MTU) {
                largeReplies++;
                if (split.getFirst().length > 0) {
                    falseNegatives++;
                }
            }
        }
        assertGreaterThan(100, largeReplies);
        assertEquals(0, falseNegatives);
    }
    
    public void testSendResponsesSendsLargeResponsesImmediately() throws Exception {
        QueryRequest queryRequest = queryRequestFactory.createOutOfBandQuery("hello", new byte[] { 111, 111, 111, 111}, 1000);
        
        messageRouter.sendResponses(new Response[] { smallResponses[0], largeResponses[0], smallResponses[1], largeResponses[1]}, queryRequest, replyHandler);
        
        assertEquals(2, messageRouter.sentReplies.size());
        assertEquals("large1.torrent", messageRouter.sentReplies.get(0).getResultsArray()[0].getName());
        assertEquals("large2.torrent", messageRouter.sentReplies.get(1).getResultsArray()[0].getName());
        
        context.assertIsSatisfied();
    }
    
    public void testSendResponsesOneResponseSplitIntoLargeResponse() throws Exception {
        QueryRequest queryRequest = queryRequestFactory.createOutOfBandQuery("hello", new byte[] { 111, 111, 111, 111}, 1000);
        
        messageRouter.sendResponses(new Response[] { largeResponses[0] }, queryRequest, replyHandler);
        
        assertEquals(1, messageRouter.sentReplies.size());
        assertEquals("large1.torrent", messageRouter.sentReplies.get(0).getResultsArray()[0].getName());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Subclass for unit under test to get a hold of query replies sent in-band.
     */
    private static class TestStandardMessageRouter extends StandardMessageRouter {
        
        public final List<QueryReply> sentReplies = new ArrayList<QueryReply>();

        @Inject
        public TestStandardMessageRouter(NetworkManager networkManager,
                QueryRequestFactory queryRequestFactory, QueryHandlerFactory queryHandlerFactory,
                OnDemandUnicaster onDemandUnicaster, HeadPongFactory headPongFactory,
                PingReplyFactory pingReplyFactory, ConnectionManager connectionManager,
                @Named("forMeReplyHandler") ReplyHandler forMeReplyHandler, QueryUnicaster queryUnicaster,
                FileViewManager fileManager, ContentManager contentManager, DHTManager dhtManager,
                UploadManager uploadManager, DownloadManager downloadManager,
                UDPService udpService, Provider<SearchResultHandler> searchResultHandler,
                SocketsManager socketsManager, HostCatcher hostCatcher,
                QueryReplyFactory queryReplyFactory, StaticMessages staticMessages,
                Provider<MessageDispatcher> messageDispatcher, MulticastService multicastService,
                QueryDispatcher queryDispatcher, Provider<ActivityCallback> activityCallback,
                ConnectionServices connectionServices, ApplicationServices applicationServices,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor, Provider<PongCacher> pongCacher,
                Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
                GuidMapManager guidMapManager, UDPReplyHandlerCache udpReplyHandlerCache,
                Provider<InspectionRequestHandler> inspectionRequestHandlerFactory,
                Provider<UDPCrawlerPingHandler> udpCrawlerPingHandlerFactory,
                Statistics statistics,
                ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory,
                PingRequestFactory pingRequestFactory, MessageHandlerBinder messageHandlerBinder,
                Provider<OOBHandler> oobHandlerFactory,
                Provider<MACCalculatorRepositoryManager> MACCalculatorRepositoryManager,
                Provider<LimeACKHandler> limeACKHandler,
                OutgoingQueryReplyFactory outgoingQueryReplyFactory,
                SharedFilesKeywordIndex sharedFilesKeywordIndex, QRPUpdater qrpUpdater,
                URNFilter urnFilter, SpamServices spamServices,
                ActivationManager activationManager, QuerySettings querySettings) {
            super(networkManager, queryRequestFactory, queryHandlerFactory, onDemandUnicaster, headPongFactory,
                    pingReplyFactory, connectionManager, forMeReplyHandler, queryUnicaster, fileManager,
                    contentManager, dhtManager, uploadManager, downloadManager, udpService,
                    searchResultHandler, socketsManager, hostCatcher, queryReplyFactory, staticMessages,
                    messageDispatcher, multicastService, queryDispatcher, activityCallback, connectionServices,
                    applicationServices, backgroundExecutor, pongCacher, simppManager, updateHandler,
                    guidMapManager, udpReplyHandlerCache, inspectionRequestHandlerFactory,
                    udpCrawlerPingHandlerFactory, statistics, replyNumberVendorMessageFactory,
                    pingRequestFactory, messageHandlerBinder, oobHandlerFactory,
                    MACCalculatorRepositoryManager, limeACKHandler, outgoingQueryReplyFactory,
                    sharedFilesKeywordIndex, qrpUpdater, urnFilter, spamServices, activationManager,
                    querySettings);
        }
        
        @Override
        protected void sendQueryReply(QueryReply queryReply) throws IOException {
            sentReplies.add(queryReply);
        }
        
    }
}
