package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.net.ConnectionDispatcher;
import org.limewire.service.ErrorService;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.ManagedConnectionFactory;
import com.limegroup.gnutella.connection.MessageReaderFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HandshakeStatus;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.SocketsManager;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;
import com.limegroup.gnutella.version.UpdateHandler;

public class BusyLeafQRTUpdateTest extends LimeTestCase {
    
    private ManagedConnectionCountQRT peer; 
    private ManagedConnectionCountQRT leaf;
    private ConnectionManagerCountQRT cm;
    private MessageRouter mr;
    
    public void setUp() throws Exception {
                
        Module module = new AbstractModule() {

            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(ConnectionManagerCountQRT.class);
                bind(FileManager.class).toInstance(new FileManagerStub());
                bind(ManagedConnection.class).to(ManagedConnectionCountQRT.class);
            }
            
        };
        
        Injector injector = LimeTestUtils.createInjector(module);
        
        peer = (ManagedConnectionCountQRT)injector.getInstance(ManagedConnection.class);
        leaf = (ManagedConnectionCountQRT)injector.getInstance(ManagedConnection.class);
        cm = (ConnectionManagerCountQRT)injector.getInstance(ConnectionManager.class);
        
        peer.setLeafConnection(false);
        leaf.setLeafConnection(true);
        
        cm.addStubOvrConnection(peer);
        cm.addStubOvrConnection(leaf);
        
        
        
        mr = injector.getInstance(MessageRouter.class);
        cm = (ConnectionManagerCountQRT)injector.getInstance(ConnectionManager.class);
    }

    /**
     * Start of BusyLeafQRTUpdateTest suite of tests
     * 
     * @throws Exception
     */
	public void testBusyLeafNoticed() throws Exception {
        leaf.setBusyEnoughToTriggerQRTRemoval(true);
        
        //  Should't work for >TEST_MIN_BUSY_LEAF_TIME seconds
        assertFalse( isAnyBusyLeafTriggeringQRTUpdate() ); 

        waitForSeconds();
        
        //  Should work, since >TEST_MIN_BUSY_LEAF_TIME seconds have elapsed
        assertTrue( isAnyBusyLeafTriggeringQRTUpdate() ); 
        
        //  UPDATE: Should still work, prior one should NOT have cleared the busy flag.
        assertTrue( isAnyBusyLeafTriggeringQRTUpdate() );
    }
    
    public void testBusyPeerNotNoticed() throws Exception {
         peer.setBusyEnoughToTriggerQRTRemoval(true);
        
        //  Should't work at all...
        assertFalse( isAnyBusyLeafTriggeringQRTUpdate() ); 

        waitForSeconds();
         
        //  Shouldn't work still
        assertFalse( isAnyBusyLeafTriggeringQRTUpdate() );
    }
    
    public void testNonBusyLastHop() throws Exception {
        //  No busy leaves
        forwardQueryRouteTablesCaller();
        assertEquals( 1, getTotalNumberOfLeavesQRTsIncluded() );
        cm.clearConnectionQRTStatus();
        waitForSeconds();
        
        //  Still no busy leaves
        forwardQueryRouteTablesCaller();
        assertEquals( 1, getTotalNumberOfLeavesQRTsIncluded() );
    }
    
    public void testBusyLastHop() throws Exception {
        //  No busy leaves
        forwardQueryRouteTablesCaller();
        assertEquals( 1, getTotalNumberOfLeavesQRTsIncluded() );
        cm.clearConnectionQRTStatus();
        
        leaf.setBusyEnoughToTriggerQRTRemoval(true);
        waitForSeconds();
        
        //  A busy leaf should have been noticed
        forwardQueryRouteTablesCaller();
        assertEquals( 0, getTotalNumberOfLeavesQRTsIncluded() );
    }
    
    public void forwardQueryRouteTablesCaller() throws Exception {
        PrivilegedAccessor.invokeMethod(mr, "forwardQueryRouteTables");
    }
    
    
    /**
     * Shortcut to waiting for awhile
     *
     */
    public void waitForSeconds() throws Exception {
            Thread.sleep(5000 + (ManagedConnectionCountQRT.TEST_MIN_BUSY_LEAF_TIME+5));
    }
    
    /**
     * Check to see how many leaves' tables would be included in the LastHop QRT
     * @return number of leaf tables which would be included in a LastHop QRT
     */
    private int getTotalNumberOfLeavesQRTsIncluded() {
        int leaves=0;
        
        Iterator it=cm.getInitializedClientConnections().iterator();
        
        while( it.hasNext() ){
            ManagedConnectionCountQRT mc=((ManagedConnectionCountQRT)it.next());
            
            if( mc._qrtIncluded )
                leaves++;
        }
        
        return leaves;
    }
    
    /**
     * Loops through all managed connections and checks them for whether they have 
     * been a busy leaf long enough to trigger a last-hop QRT update.
     * 
     * @return true iff the last-hop QRT tables should be updated early.
     */
     public boolean isAnyBusyLeafTriggeringQRTUpdate(){
        boolean busyLeaf=false;
        List list=cm.getInitializedClientConnections();
        Iterator it=list.iterator();
        
        while( !busyLeaf && it.hasNext() )
            //  NOTE: don't remove the leaf's BUSY flag, since some peers may not have
            //  been updated yet due to timing quirks.
            if( ((ManagedConnection)it.next()).isBusyEnoughToTriggerQRTRemoval() )
                busyLeaf=true;
        
        return busyLeaf;
    }
    
    /**
     * The local stub for testing busy leaf QRT updating functionality
     * 
     * Attaches ManagedConnectionStubs to this, and performs superclass related 
     * functionality on them
     * 
     * Also, adds method clearConnectionQRTStatus(), which loops through connections 
     * and clears the per-connection flag which is used to indicate whether or not a 
     * connection's QRT table was included in the LastHop QRT.
     */
    @Singleton
    static class ConnectionManagerCountQRT extends ConnectionManagerStub {
        
        
        @Inject
        public ConnectionManagerCountQRT(NetworkManager networkManager,
                Provider<HostCatcher> hostCatcher,
                @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                Provider<SimppManager> simppManager,
                CapabilitiesVMFactory capabilitiesVMFactory,
                ManagedConnectionFactory managedConnectionFactory,
                Provider<MessageRouter> messageRouter,
                Provider<QueryUnicaster> queryUnicaster,
                SocketsManager socketsManager,
                ConnectionServices connectionServices,
                Provider<NodeAssigner> nodeAssigner,
                Provider<IPFilter> ipFilter,
                ConnectionCheckerManager connectionCheckerManager,
                PingRequestFactory pingRequestFactory) {
            super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor,
                    simppManager, capabilitiesVMFactory, managedConnectionFactory,
                    messageRouter, queryUnicaster, socketsManager, connectionServices,
                    nodeAssigner, ipFilter, connectionCheckerManager, pingRequestFactory);
            
        }

        public void addStubOvrConnection( ManagedConnectionCountQRT mcso ) throws Exception {
            PrivilegedAccessor.invokeMethod( 
                    this, "connectionInitializing",  
                    new Object[] { mcso }, new Class[] { ManagedConnection.class } );

            PrivilegedAccessor.invokeMethod( 
                    this, "connectionInitialized",  
                    new Object[] { mcso }, new Class[] { ManagedConnection.class } );
        }
        
        /**
         * Loop through connections and clear each one's "QRT Included" flag 
         */
        public void clearConnectionQRTStatus() {
            Iterator it=getConnections().iterator();
            while( it.hasNext() ) {
                ManagedConnectionCountQRT mc=((ManagedConnectionCountQRT)it.next());
                if( mc._qrtIncluded ) {
                    mc._qrtIncluded=false;
                }
            }
        }
    }
    
    /**
     * The local stub for testing busy leaf connections
     */
    static class ManagedConnectionCountQRT extends ManagedConnectionStub {      
        
        @Inject
        public ManagedConnectionCountQRT(ConnectionManager connectionManager,
                NetworkManager networkManager,
                QueryRequestFactory queryRequestFactory,
                HeadersFactory headersFactory,
                HandshakeResponderFactory handshakeResponderFactory,
                QueryReplyFactory queryReplyFactory,
                MessageDispatcher messageDispatcher,
                NetworkUpdateSanityChecker networkUpdateSanityChecker,
                UDPService udService, SearchResultHandler searchResultHandler,
                CapabilitiesVMFactory capabilitiesVMFactory,
                SocketsManager socketsManager, Acceptor acceptor,
                MessagesSupportedVendorMessage supportedVendorMessage,
                Provider<SimppManager> simppManager,
                Provider<UpdateHandler> updateHandler,
                Provider<ConnectionServices> connectionServices,
                GuidMapManager guidMapManager,
                SpamFilterFactory spamFilterFactory,
                MessageReaderFactory messageReaderFactory,
                MessageFactory messageFactory,
                ApplicationServices applicationServices) {
            super(connectionManager, networkManager, queryRequestFactory, headersFactory,
                    handshakeResponderFactory, queryReplyFactory, messageDispatcher,
                    networkUpdateSanityChecker, udService, searchResultHandler,
                    capabilitiesVMFactory, socketsManager, acceptor,
                    supportedVendorMessage, simppManager, updateHandler,
                    connectionServices, guidMapManager, spamFilterFactory,
                    messageReaderFactory, messageFactory, applicationServices);
            try {
              PrivilegedAccessor.setValue(ManagedConnection.class, "MIN_BUSY_LEAF_TIME", new Long(TEST_MIN_BUSY_LEAF_TIME));                
              PrivilegedAccessor.setValue(this, "hopsFlowMax", new Integer(-1));
          } catch (Exception e) {
              ErrorService.error( e );
          }  
        }

        private static final long TEST_MIN_BUSY_LEAF_TIME=1000*5;
        
        private boolean isLeaf = false;
        
        public long getNextQRPForwardTime() {
            return 0l;
        }        
        
        public void incrementNextQRPForwardTime(long curTime) {
        }

        public boolean isUltrapeerQueryRoutingConnection() {
            return !isLeaf;
        }
        
        /** 
         * If I'm not a leaf then I'm an Ultrapeer! See also 
         * isClientSupernodeConnection()
         */
        public boolean isSupernodeClientConnection() {
            return isLeaf;
        }
        
        /** 
         * If I'm not a leaf then I'm an Ultrapeer! See also 
         * isSupernodeClientConnection()
         */
        public boolean isClientSupernodeConnection() {
            return isLeaf;
        }
        
        public boolean isQueryRoutingEnabled() {
            return !isLeaf;
        }
        
        public void send(Message m) {}

        public void setBusyEnoughToTriggerQRTRemoval( boolean busy ) throws Exception{
            PrivilegedAccessor.setValue(this, "hopsFlowMax", new Integer( ((busy)?(0):(-1)) ) );
            
            setBusy(busy);
         }
        
        public void setLeafConnection( boolean isLeaf ){
            this.isLeaf = isLeaf;
        }

        public boolean _qrtIncluded=false;
        public QueryRouteTable getQueryRouteTableReceived() {
            _qrtIncluded = true;
            return super.getQueryRouteTableReceived();
        }
    }
    
    /**
     * JUnit crap...
     */
    public BusyLeafQRTUpdateTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BusyLeafQRTUpdateTest.class);
    }
    
    @Singleton
    public static class ConnectionManagerStub extends ConnectionManager {
                
        public ConnectionManagerStub(NetworkManager networkManager,
                Provider<HostCatcher> hostCatcher,
                @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                Provider<SimppManager> simppManager,
                CapabilitiesVMFactory capabilitiesVMFactory,
                ManagedConnectionFactory managedConnectionFactory,
                Provider<MessageRouter> messageRouter,
                Provider<QueryUnicaster> queryUnicaster,
                SocketsManager socketsManager,
                ConnectionServices connectionServices,
                Provider<NodeAssigner> nodeAssigner,
                Provider<IPFilter> ipFilter,
                ConnectionCheckerManager connectionCheckerManager,
                PingRequestFactory pingRequestFactory) {
            super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor,
                    simppManager, capabilitiesVMFactory, managedConnectionFactory,
                    messageRouter, queryUnicaster, socketsManager, connectionServices,
                    nodeAssigner, ipFilter, connectionCheckerManager, pingRequestFactory);
            
        }

        volatile boolean enableRemove=false;

        public void setEnableRemove(boolean enableRemove) {
            this.enableRemove = enableRemove;
        }

        /** Calls c.close iff enableRemove */
        public void remove(ManagedConnection c) {
            if (enableRemove) 
                c.close();
        }
        
        public boolean isSupernode() {
            return true;
        }        

        public HandshakeStatus allowConnection(HandshakeResponse hr) {
            return HandshakeStatus.OK;
        }
        
        public String toString() {
            return "ConnectionManagerStub";
        }
    }

    public static class ManagedConnectionStub extends ManagedConnection {

        @Inject
        public ManagedConnectionStub(
                ConnectionManager connectionManager, NetworkManager networkManager,
                QueryRequestFactory queryRequestFactory,
                HeadersFactory headersFactory,
                HandshakeResponderFactory handshakeResponderFactory,
                QueryReplyFactory queryReplyFactory,
                MessageDispatcher messageDispatcher,
                NetworkUpdateSanityChecker networkUpdateSanityChecker,
                UDPService udService,
                SearchResultHandler searchResultHandler,
                CapabilitiesVMFactory capabilitiesVMFactory,
                SocketsManager socketsManager, Acceptor acceptor,
                MessagesSupportedVendorMessage supportedVendorMessage,
                Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
                Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager, SpamFilterFactory spamFilterFactory,
                MessageReaderFactory messageReaderFactory,
                MessageFactory messageFactory,
                ApplicationServices applicationServices) {
            super("1.2.3.4", 6346, ConnectType.PLAIN, connectionManager, networkManager,
                  queryRequestFactory, 
                  headersFactory,
                  handshakeResponderFactory,
                  queryReplyFactory, messageDispatcher,
                  networkUpdateSanityChecker,
                  udService,
                  searchResultHandler,
                  capabilitiesVMFactory,
                  socketsManager, acceptor,
                  supportedVendorMessage,
                  simppManager, updateHandler,
                  connectionServices, guidMapManager, spamFilterFactory,
                  messageReaderFactory, messageFactory, applicationServices);
        }

        public ManagedConnectionStub(Socket socket,
                ConnectionManager connectionManager,
                NetworkManager networkManager,
                QueryRequestFactory queryRequestFactory,
                HeadersFactory headersFactory,
                HandshakeResponderFactory handshakeResponderFactory,
                QueryReplyFactory queryReplyFactory,
                MessageDispatcher messageDispatcher,
                NetworkUpdateSanityChecker networkUpdateSanityChecker,
                UDPService udService, SearchResultHandler searchResultHandler,
                CapabilitiesVMFactory capabilitiesVMFactory, Acceptor acceptor,
                MessagesSupportedVendorMessage supportedVendorMessage,
                Provider<SimppManager> simppManager,
                Provider<UpdateHandler> updateHandler,
                Provider<ConnectionServices> connectionServices,
                GuidMapManager guidMapManager,
                SpamFilterFactory spamFilterFactory,
                MessageReaderFactory messageReaderFactory,
                MessageFactory messageFactory,
                ApplicationServices applicationServices) {
            super(socket, connectionManager, networkManager, queryRequestFactory,
                    headersFactory, handshakeResponderFactory, queryReplyFactory,
                    messageDispatcher, networkUpdateSanityChecker, udService,
                    searchResultHandler, capabilitiesVMFactory, acceptor,
                    supportedVendorMessage, simppManager, updateHandler,
                    connectionServices, guidMapManager, spamFilterFactory,
                    messageReaderFactory, messageFactory, applicationServices);
            
        }

        @Override
        public void initialize() throws IOException, NoGnutellaOkException,
                BadHandshakeException {
        }
    }
    
}