package com.limegroup.gnutella;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messagehandlers.AdvancedToggleHandler;
import com.limegroup.gnutella.messagehandlers.InspectionRequestHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.SocketsManager;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;
import com.limegroup.gnutella.version.UpdateHandler;


/**
 * Test to make sure that query routing tables are correctly exchanged between
 * Ultrapeers.
 *
 * ULTRAPEER_1  ----  ULTRAPEER_2
 */
@SuppressWarnings("unchecked")
public final class UltrapeerQueryRouteTableTest extends LimeTestCase {


    @SuppressWarnings("unused") //DPINJ - testfix
    private static ActivityCallback CALLBACK;
    @SuppressWarnings("unused") //DPINJ - testfix
    private static TestMessageRouter MESSAGE_ROUTER;        
 //   private static RouterService ROUTER_SERVICE;
            
	/**
     * A filename that won't match.
     */
    private static final String noMatch = "junkie junk";

	/**
	 * The central Ultrapeer used in the test.
	 */
	//private static final RouterService ULTRAPEER_2 = 
    //new RouterService(new TestCallback());

    //private static final ReplyHandler REPLY_HANDLER =
    //  new TestReplyHandler();

    private static List REPLIES = new LinkedList();
    
    private static List SENT = new LinkedList();
    private LifecycleManager lifecycleManager;
    private QueryRequestFactory queryRequestFactory;
    private ConnectionServices connectionServices;

    public UltrapeerQueryRouteTableTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UltrapeerQueryRouteTableTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
    
    private static void setSettings() throws Exception {
        setStandardSettings();
        //SearchSettings.PROBE_TTL.setValue((byte)1);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        ConnectionSettings.PORT.setValue(6332);
        
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
    }
    
    public void setUp() throws Exception {
        setSettings();
        
        launchBackend();

        CALLBACK = new TestCallback();

        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MessageRouter.class).to(TestMessageRouter.class);
            }
        });
        
        MESSAGE_ROUTER = (TestMessageRouter) injector.getInstance(MessageRouter.class);
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        connectionServices = injector.getInstance(ConnectionServices.class);

        lifecycleManager.start();
        connectionServices.connectToHostAsynchronously("localhost", 
            Backend.BACKEND_PORT, ConnectType.PLAIN);    
        
        // Wait for awhile after the connection to make sure the hosts have 
        // time to exchange QRP tables.
         Thread.sleep(10 * 1000);
        assertTrue("should be connected", connectionServices.isConnected());
	}

	public void tearDown() throws Exception {
	    connectionServices.disconnect();
	    lifecycleManager.shutdown();
	}
    
    /**
     * Test to make sure we will never send with a TTL of 1 to a 
     * Ultrapeer that doesn't have a hit.
     */
    public void testSentQueryIsNotTTL1() throws Exception {
        assertTrue("should be connected", connectionServices.isConnected());
        QueryRequest qr = queryRequestFactory.createQuery(noMatch, (byte)1);
        sendQuery(qr);        
        Thread.sleep(2000);
        // we will send the query, but with a TTL of 2, not 1, because
        // the ultrapeer doesn't have this query in its qrp table.
        assertTrue("should have sent query", !SENT.isEmpty());
        assertEquals("should not have received any replies", 0, REPLIES.size());
        QueryRequest qSent = (QueryRequest)SENT.get(0);
        assertEquals("wrong ttl", 2, qSent.getTTL());
        assertEquals("wrong hops", 0, qSent.getHops());
        assertEquals("wrong query", qr.getQuery(), qSent.getQuery());
        assertEquals("wrong guid", qr.getGUID(), qSent.getGUID());
	}
    
    /**
     * Test to make sure that dynamic querying sends a query with TTL=1 and 
     * other properties when a neighboring Ultrapeer has a hit in its QRP
     * table for that query.
     */
    public void testDynamicQueryingWithQRPHit() throws Exception {
        assertTrue("should be connected", connectionServices.isConnected());
                
        QueryRequest qr = queryRequestFactory.createQuery(
            "FileManagerTest.class." + Backend.SHARED_EXTENSION, (byte)1);
        sendQuery(qr);
        Thread.sleep(4000);
        assertTrue("should have sent query", !SENT.isEmpty());
        assertTrue("should have received replies", !REPLIES.isEmpty());
        
        QueryRequest qSent = (QueryRequest)SENT.get(0);
        
        // The TTL on the sent query should be 1 because the other Ultrapeer
        // should have a "hit" in its QRP table.  When there's a hit, we 
        // send with TTL 1 simply because it's likely that it's popular.
        assertEquals("wrong ttl", 1, qSent.getTTL());
        assertEquals("wrong hops", 0, qSent.getHops());
        assertEquals("wrong query", qr.getQuery(), qSent.getQuery());
        assertEquals("wrong guid", qr.getGUID(), qSent.getGUID());        
    }


    /**
     * The actual QueryRequest sent will not be the same (==) as this,
     * because QueryHandler creates new queries with appropriate TTLs.
     */
    private void sendQuery(QueryRequest qr) throws Exception {
      //  ResponseVerifier VERIFIER = (ResponseVerifier)PrivilegedAccessor.getValue(ROUTER_SERVICE, "VERIFIER");
     //   VERIFIER.record(qr);
        
        MessageRouter mr = MESSAGE_ROUTER;
        mr.sendDynamicQuery(qr);
        //mr.broadcastQueryRequest(qr);
    }
    
    @Singleton
    private static class TestMessageRouter extends StandardMessageRouter {
        
        @Inject
        public TestMessageRouter(NetworkManager networkManager,
                QueryRequestFactory queryRequestFactory,
                QueryHandlerFactory queryHandlerFactory,
                OnDemandUnicaster onDemandUnicaster,
                HeadPongFactory headPongFactory, PingReplyFactory pingReplyFactory,
                ConnectionManager connectionManager, @Named("forMeReplyHandler")
                ReplyHandler forMeReplyHandler, QueryUnicaster queryUnicaster,
                FileManager fileManager, ContentManager contentManager,
                DHTManager dhtManager, UploadManager uploadManager,
                DownloadManager downloadManager, UDPService udpService,
                SearchResultHandler searchResultHandler,
                SocketsManager socketsManager, HostCatcher hostCatcher,
                QueryReplyFactory queryReplyFactory, StaticMessages staticMessages,
                Provider<MessageDispatcher> messageDispatcher,
                MulticastService multicastService, QueryDispatcher queryDispatcher,
                Provider<ActivityCallback> activityCallback,
                ConnectionServices connectionServices,
                ApplicationServices applicationServices,
                @Named("backgroundExecutor")
                ScheduledExecutorService backgroundExecutor,
                Provider<PongCacher> pongCacher,
                Provider<SimppManager> simppManager,
                Provider<UpdateHandler> updateHandler,
                GuidMapManager guidMapManager, 
                UDPReplyHandlerCache udpReplyHandlerCache,
                Provider<InspectionRequestHandler> inspectionRequestHandlerFactory,
                Provider<UDPCrawlerPingHandler> udpCrawlerPingHandlerFactory,
                Provider<AdvancedToggleHandler> advancedToggleHandlerFactory,
                Statistics statistics,
                ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory,
                PingRequestFactory pingRequestFactory) {
            super(networkManager, queryRequestFactory, queryHandlerFactory,
                    onDemandUnicaster, headPongFactory, pingReplyFactory,
                    connectionManager, forMeReplyHandler, queryUnicaster,
                    fileManager, contentManager, dhtManager, uploadManager,
                    downloadManager, udpService, searchResultHandler,
                    socketsManager, hostCatcher, queryReplyFactory, staticMessages,
                    messageDispatcher, multicastService, queryDispatcher,
                    activityCallback, connectionServices, applicationServices,
                    backgroundExecutor, pongCacher, simppManager, updateHandler,
                    guidMapManager, udpReplyHandlerCache, inspectionRequestHandlerFactory, 
                    udpCrawlerPingHandlerFactory, advancedToggleHandlerFactory, statistics,
                    replyNumberVendorMessageFactory, pingRequestFactory);
        }
        
        public boolean originateQuery(QueryRequest r, ManagedConnection c) {
            SENT.add(r);
            super.sendQueryRequest(r, c);
            return true;
        }
	}

    private static class TestCallback extends ActivityCallbackStub {
        public void handleQueryResult(RemoteFileDesc rfd,
                                      HostData hd,
                                      Set locs) {
            REPLIES.add(new Object());
        }
    }
}
