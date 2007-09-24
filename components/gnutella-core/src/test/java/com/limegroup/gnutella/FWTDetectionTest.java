package com.limegroup.gnutella;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.ManagedConnectionFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.SocketsManager;

/**
 * this class tests that the node properly detects if it is
 * capable of firewall to firewall transfers.
 */
public class FWTDetectionTest extends LimeTestCase {
    
    public FWTDetectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FWTDetectionTest.class);
    }
    
    private int REMOTE_PORT1 = 10000;
    private int REMOTE_PORT2 = 10001;
    
    private UDPPonger ponger1;
    private UDPPonger ponger2;
    
    private File gnutellaDotNetFile;   
    private CMStub connectionManager;
    private Injector injector;
    private UDPService udpService;
    private NetworkManagerStub networkManager;
    
    /**
     * the basic testing routine is a node with a few hosts in its gnutella.net
     * the node sends an initial ping to them, and they return various
     * pongs.
     */
    public void setUp() throws Exception {
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
           @Override
            protected void configure() {
               bind(ConnectionManager.class).to(CMStub.class);
               bind(NetworkManager.class).to(NetworkManagerStub.class);
               
            } 
        });
        
        connectionManager = (CMStub)injector.getInstance(ConnectionManager.class);  
        gnutellaDotNetFile = new File(CommonUtils.getUserSettingsDir(),"gnutella.net");
        
        connectionManager.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        
        udpService = injector.getInstance(UDPService.class);
        PrivilegedAccessor.setValue(udpService,"_lastReportedPort",new Integer(0));
        PrivilegedAccessor.setValue(udpService,"_numReceivedIPPongs",new Integer(0));
        PrivilegedAccessor.setValue(udpService,"_acceptedSolicitedIncoming",new Boolean(true));
        PrivilegedAccessor.setValue(udpService,"_portStable",new Boolean(true));
        
        networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        networkManager.setPort(7788);
        networkManager.setAddress(InetAddress.getLocalHost().getAddress());
        networkManager.setExternalAddress(InetAddress.getLocalHost().getAddress());
        networkManager.setSolicitedGUID(udpService.getSolicitedGUID());
        networkManager.setIpPortValid(true);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        ponger1 = new UDPPonger(REMOTE_PORT1, messageFactory, pingReplyFactory);
        ponger2 = new UDPPonger(REMOTE_PORT2, messageFactory, pingReplyFactory);
        
        injector.getInstance(LifecycleManager.class).start();
    }
    
    public void tearDown() throws Exception {
        if(ponger1 != null)
            ponger1.drainAndClose();
        if(ponger2 != null)
            ponger2.drainAndClose();
    }
    
    /**
     * tests the scenario where we are not connected
     */
    public void testDisconnected() throws Exception {
        connectionManager.setConnected(false);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(udpService.canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(udpService.canDoFWT());
        
    }
    
    /**
     * tets the scenario where we have and have not received a pong 
     */
    public void testNotReceivedPong() throws Exception {
        connectionManager.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(udpService.canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(udpService.canDoFWT());
        
        connectionManager.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);        
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //reply with a pong that does not carry info
        ponger1.reply(null);
        connectionManager.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(udpService.canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(udpService.canDoFWT());
        
        //reply with a pong that does carry info
        IpPort myself = new IpPortImpl(networkManager.getExternalAddress(), networkManager.getPort());
        ponger1.reply(myself);
        Thread.sleep(1000);
        
        connectionManager.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(udpService.canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertTrue(udpService.canDoFWT());
    }
    
    /**
     * tests the scenarios where we have received more than one pongs,
     * sometimes reporting different ports.
     */
    public void testReceivedManyPongs() throws Exception {
        connectionManager.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        udpService.setReceiveSolicited(true);
        Acceptor acceptor = injector.getInstance(Acceptor.class);
        // make sure local != forced port, so we know udpService is used forced
        assertNotEquals(networkManager.getPort(), acceptor.getPort(false));
        assertEquals(networkManager.getPort(),udpService.getStableUDPPort());
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);        
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which does has a different port
        ponger1.reply(new IpPortImpl(networkManager.getExternalAddress(),1000));
        
        Thread.sleep(1000);
        
        connectionManager.setConnected(true);
        assertFalse(udpService.canDoFWT());
        
        // our external port should still point to our router service port
        assertEquals(networkManager.getPort(), udpService.getStableUDPPort());
        
        // send a second pong.. now we should be able to do FWT
        ponger1.reply(new IpPortImpl(networkManager.getExternalAddress(),1000));
        Thread.sleep(1000);
        
        connectionManager.setConnected(true);
        assertTrue(udpService.canDoFWT());
        
        // and our external port should become the new port
        assertEquals(1000,udpService.getStableUDPPort());
    }

    /**
     * tests the scenario where we have a forced port different from our 
     * external port
     */
    public void testForcedPort() throws Exception {
        Acceptor acceptor = injector.getInstance(Acceptor.class);
        int localPort = acceptor.getPort(false);
        networkManager.setPort(1000);
        assertNotEquals(1000,localPort);
        connectionManager.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        udpService.setReceiveSolicited(true);
        assertEquals(networkManager.getPort(),udpService.getStableUDPPort());
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);        
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which carries our local ip address
        ponger1.reply(new IpPortImpl(networkManager.getExternalAddress(),localPort));        
        Thread.sleep(1000);
        
        // now we should be able to do FWT, and our port should be our local port
        connectionManager.setConnected(true);
        assertTrue(udpService.canDoFWT());
        assertEquals(localPort,udpService.getStableUDPPort());
        
        
        // reset the value of num received pongs and send another pong, 
        // carrying our forced address
        PrivilegedAccessor.setValue(udpService,"_numReceivedIPPongs", new Integer(0));
        ponger1.reply(new IpPortImpl(networkManager.getExternalAddress(),1000));
        
        assertTrue(udpService.canDoFWT());
        assertEquals(1000,udpService.getStableUDPPort());
        
        //clean up
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(false);
    }
    
    /**
     * tests scenarios where we can and can't do solicited
     */
    public void testSolicited() throws Exception{        
        //if we can't do solicited, we're out
        udpService.setReceiveSolicited(false);
        assertFalse(udpService.canReceiveSolicited());
        assertFalse(udpService.canDoFWT());
        
        udpService.setReceiveSolicited(true);
        assertTrue(udpService.canReceiveSolicited());
        assertTrue(udpService.canDoFWT());
    }
    
    public void testInvalidExternal() throws Exception {
        connectionManager.setConnected(true);
        udpService.setReceiveSolicited(true);
        
        // send a pong
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);        
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which does has a different port
        ponger1.reply(new IpPortImpl(networkManager.getExternalAddress(), networkManager.getPort()));
        
        Thread.sleep(1000);
        
        // try with valid external address
        assertTrue(udpService.canDoFWT());
        
        // and with an invalid one
        networkManager.setExternalAddress(InetAddress.getByName("0.0.0.0").getAddress());
        assertFalse(NetworkUtils.isValidAddress(networkManager.getExternalAddress()));
        assertFalse(udpService.canDoFWT());
        
    }
    /**
     * tests if the pings are requesting ip:port check properly
     */
    public void testPingsRequesting() throws Exception {        
        // make sure we have not received incoming connection in the past
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);        
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        // if we have received incoming, pings should not be requesting
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        assertFalse(ponger1.listen().requestsIP());
        
    }
    
    
    /**
     * tests the case where both pinged hosts reply with the same
     * ip:port.
     */
    public void testPongsCarryGoodInfo() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);        
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        assertTrue(ponger1.listen().requestsIP());
        assertTrue(ponger2.listen().requestsIP());
        IpPort myself = new IpPortImpl(networkManager.getExternalAddress(), networkManager.getPort());
        ponger1.reply(myself);
        ponger2.reply(myself);
        Thread.sleep(500);
        connectionManager.setConnected(true);
        assertTrue(udpService.canDoFWT());
        assertFalse(ConnectionSettings.LAST_FWT_STATE.getValue());
        connectionManager.setConnected(false);
    }
    
    
    /**
     * tests the case where a pong says we have a different port
     */
    public void testPongCarriesBadPort() throws Exception{
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);        
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        assertTrue(ponger1.listen().requestsIP());
        assertTrue(ponger2.listen().requestsIP());
        
        IpPort badPort1 = new IpPortImpl(networkManager.getExternalAddress(), 12345);
        IpPort badPort2 = new IpPortImpl(networkManager.getExternalAddress(),12346);
        ponger1.reply(badPort1);
        ponger2.reply(badPort2);
        Thread.sleep(500);
        connectionManager.setConnected(true);
        assertFalse(udpService.canDoFWT());
        assertTrue(ConnectionSettings.LAST_FWT_STATE.getValue());
        connectionManager.setConnected(false);
    }
    
    /**
     * tests the scenario where a (malicious) pong does not affect our
     * status
     */
    public void testMaliciousPongDoesNotDisable() throws Exception {
        connectionManager.setConnected(true);
        assertTrue(udpService.canDoFWT());
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        connectionManager.setConnected(false);
        
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);        
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        ponger1.listen();
        IpPort badAddress = new IpPortImpl("1.2.3.4", networkManager.getPort());
        PingReply badGuid = injector.getInstance(PingReplyFactory.class).create(GUID.makeGuid(),(byte)1,badAddress);
        ponger1.replyPong(badGuid);
        Thread.sleep(1000);
        connectionManager.setConnected(true);
        assertTrue(udpService.canDoFWT());
    }
    
    public void testServerResponse() throws Exception {
        connectionManager.setConnected(true);
        DatagramSocket sock = new DatagramSocket(20000);
        sock.setSoTimeout(500);
        
        PingRequestFactory pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        PingRequest with = pingRequestFactory.createPingRequest((byte)1);
        PingRequest without = pingRequestFactory.createPingRequest((byte)1);
        
        with.addIPRequest();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        with.write(baos);

        byte[] data = baos.toByteArray();
        DatagramPacket pack = new DatagramPacket(data, data.length, new InetSocketAddress(
                InetAddress.getByAddress(networkManager.getExternalAddress()), udpService.getListeningPort()));
        
        sock.send(pack);
        
        Thread.sleep(100);
        DatagramPacket read = new DatagramPacket(new byte[100],100);
        sock.receive(read);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(read.getData());
        
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        PingReply replyWith = (PingReply)messageFactory.read(bais);
        
        assertNotNull(replyWith.getMyInetAddress());
        assertEquals(networkManager.getExternalAddress(), replyWith.getMyInetAddress().getAddress());
        assertEquals(20000,replyWith.getMyPort());
        
        // test a ping without
        baos = new ByteArrayOutputStream();
        without.write(baos);
        
        data = baos.toByteArray();
        pack = new DatagramPacket(data, data.length, new InetSocketAddress(
                InetAddress.getByAddress(networkManager.getExternalAddress()), udpService.getListeningPort()));
        
        sock.send(pack);
        
        Thread.sleep(100);
        read = new DatagramPacket(new byte[100],100);
        sock.receive(read);
        bais = new ByteArrayInputStream(read.getData());
        
        PingReply replyWithout = (PingReply)messageFactory.read(bais);
        
        assertNull(replyWithout.getMyInetAddress());
        assertEquals(0,replyWithout.getMyPort());
        
    }
    
    private void writeToGnet(String hosts) throws Exception {
        FileOutputStream fos = new FileOutputStream(gnutellaDotNetFile);
        fos.write(hosts.getBytes());fos.flush();fos.close();
    }
    
    private static class UDPPonger {
        private MessageFactory messageFactory;
        private PingReplyFactory pingReplyFactory;
        private final DatagramSocket _sock;
        private SocketAddress _lastAddress;
        
        public PingReply reply;
        public boolean shouldAsk;
        
        private byte [] lastReceived;
        
        public UDPPonger(int port, MessageFactory messageFactory, PingReplyFactory pingReplyFactory) throws IOException {
            _sock = new DatagramSocket(port);
            _sock.setSoTimeout(5000);
            this.pingReplyFactory = pingReplyFactory;
            this.messageFactory = messageFactory;
        }
        
        public PingRequest listen() throws Exception {
            byte [] data = new byte[1024];
                //receive a ping.
                DatagramPacket pack = new DatagramPacket(data,1024);
                _sock.receive(pack);
                _lastAddress = pack.getSocketAddress();
                
                ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
                PingRequest ret = (PingRequest)messageFactory.read(bais);
                lastReceived = ret.getGUID();
                return ret;
        }
        
        /**
         * send a pong with the specified address back to the pinger.
         * it uses the solicited ping
         */
        public void reply(IpPort reply) throws Exception{
            PingReply toSend;
                
            if (reply==null)
                toSend = pingReplyFactory.create(lastReceived,(byte)1);
            else
                toSend = pingReplyFactory.create(lastReceived,(byte)1,reply);
         
            replyPong(toSend);
            
            
        }
        
        public void replyPong(PingReply reply) throws Exception{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            reply.write(baos);
            byte []data = baos.toByteArray();
            DatagramPacket pack = new DatagramPacket(data,data.length,_lastAddress);
            _sock.send(pack);
        }
        
        public void drainAndClose() throws IOException {
            _sock.setSoTimeout(100);
            try{
                while(true)
                    _sock.receive(new DatagramPacket(new byte[1000],1000));
            } catch(IOException expected) {}
            _sock.close();
        }
    }
    
    @Singleton
    private static class CMStub extends ConnectionManagerStub {
        
        @Inject
        public CMStub(NetworkManager networkManager, Provider<HostCatcher> hostCatcher,
                @Named("global")
                Provider<ConnectionDispatcher> connectionDispatcher, @Named("backgroundExecutor")
                ScheduledExecutorService backgroundExecutor, Provider<SimppManager> simppManager,
                CapabilitiesVMFactory capabilitiesVMFactory,
                ManagedConnectionFactory managedConnectionFactory,
                Provider<MessageRouter> messageRouter, Provider<QueryUnicaster> queryUnicaster,
                SocketsManager socketsManager, ConnectionServices connectionServices,
                Provider<NodeAssigner> nodeAssigner, Provider<IPFilter> ipFilter,
                ConnectionCheckerManager connectionCheckerManager, PingRequestFactory pingRequestFactory) {
            super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor, simppManager,
                    capabilitiesVMFactory, managedConnectionFactory, messageRouter, queryUnicaster,
                    socketsManager, connectionServices, nodeAssigner, ipFilter, connectionCheckerManager,
                    pingRequestFactory);
        }

        private boolean connected;
        public boolean isConnected() {
            return connected;
        }
        public void setConnected(boolean yes) {
            connected=yes;
        }
        
        public boolean isFullyConnected() {
            return false;
        }
        
        public int getPreferredConnectionCount() {
            return 1;
        }
        
        public List<ManagedConnection> getInitializedConnections() {
            return Collections.emptyList();
        }
    }

}
