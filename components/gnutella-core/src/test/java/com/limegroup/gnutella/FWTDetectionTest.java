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
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * this class tests that the node properly detects if it is
 * capable of firewall to firewall transfers.
 */
@SuppressWarnings("unchecked")
public class FWTDetectionTest extends LimeTestCase {
    
    public FWTDetectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FWTDetectionTest.class);
    }

    //make sure the RouterService class gets loaded before the UDPService class
    //static RouterService router = new RouterService(new ActivityCallbackStub());
    
    static int REMOTE_PORT1 = 10000;
    static int REMOTE_PORT2 = 10001;
    
    static UDPPonger ponger1 = new UDPPonger(REMOTE_PORT1);
    static UDPPonger ponger2 = new UDPPonger(REMOTE_PORT2);
    
    static File gnet;

    
    
    static CMStub cmStub;
    /**
     * the basic testing routine is a node with a few hosts in its gnutella.net
     * the node sends an initial ping to them, and they return various
     * pongs.
     */
    public static void globalSetUp() {
        if(true)throw new RuntimeException("fix me");
        
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        
        // the catcher in RouterService points elsewhere 
        HostCatcher catcher = new HostCatcher(); 
        
        cmStub = new CMStub();
        
        try{
            PrivilegedAccessor.setValue(RouterService.class,"manager",cmStub);
            PrivilegedAccessor.setValue(RouterService.class,"catcher",catcher);
        }catch(Exception bad) {
            ErrorService.error(bad);
        }
        ProviderHacks.getLifecycleManager().start();
        
        cmStub.setConnected(true);
        assertTrue(RouterService.isConnected());
        cmStub.setConnected(false);
        assertFalse(RouterService.isConnected());
        
        gnet = new File(CommonUtils.getUserSettingsDir(),"gnutella.net");
                
    }
    
    public void setUp() {
        cmStub.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        
        try {
            UDPService service = ProviderHacks.getUdpService();
            PrivilegedAccessor.setValue(service,"_lastReportedPort",new Integer(0));
            PrivilegedAccessor.setValue(service,"_numReceivedIPPongs",new Integer(0));
            PrivilegedAccessor.setValue(service,"_acceptedSolicitedIncoming",new Boolean(true));
            PrivilegedAccessor.setValue(service,"_portStable",new Boolean(true));
            PrivilegedAccessor.setValue(Acceptor.class,"_externalAddress",new byte[4]);
        }catch(Exception bad) {
            ErrorService.error(bad);
        }
    }
    
    public void tearDown() {
        ponger1.drain();
        ponger2.drain();
    }
    
    /**
     * tests the scenario where we are not connected
     */
    public void testDisconnected() throws Exception {
        cmStub.setConnected(false);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(ProviderHacks.getUdpService().canDoFWT());
        
    }
    
    /**
     * tets the scenario where we have and have not received a pong 
     */
    public void testNotReceivedPong() throws Exception {
        cmStub.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(ProviderHacks.getUdpService().canDoFWT());
        
        cmStub.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //reply with a pong that does not carry info
        ponger1.reply(null);
        cmStub.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(ProviderHacks.getUdpService().canDoFWT());
        
        //reply with a pong that does carry info
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        Endpoint myself = new Endpoint(ProviderHacks.getNetworkManager().getExternalAddress(),
                ProviderHacks.getNetworkManager().getPort());
        ponger1.reply(myself);
        Thread.sleep(1000);
        
        cmStub.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
    }
    
    /**
     * tests the scenarios where we have received more than one pongs,
     * sometimes reporting different ports.
     */
    public void testReceivedManyPongs() throws Exception {
        cmStub.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        ProviderHacks.getUdpService().setReceiveSolicited(true);
        assertEquals(ProviderHacks.getNetworkManager().getPort(),ProviderHacks.getUdpService().getStableUDPPort());
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which does has a different port
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        ponger1.reply(new Endpoint(ProviderHacks.getNetworkManager().getExternalAddress(),1000));
        
        Thread.sleep(1000);
        
        cmStub.setConnected(true);
        assertFalse(ProviderHacks.getUdpService().canDoFWT());
        
        // our external port should still point to our router service port
        assertEquals(ProviderHacks.getNetworkManager().getPort(),ProviderHacks.getUdpService().getStableUDPPort());
        
        // send a second pong.. now we should be able to do FWT
        ponger1.reply(new Endpoint(ProviderHacks.getNetworkManager().getExternalAddress(),1000));
        Thread.sleep(1000);
        
        cmStub.setConnected(true);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        
        // and our external port should become the new port
        assertEquals(1000,ProviderHacks.getUdpService().getStableUDPPort());
    }
    
    /**
     * tests the scenario where we have a forced port different from our 
     * external port
     */
    public void testForcedPort() throws Exception {
        int localPort = ProviderHacks.getAcceptor().getPort(false);
        ConnectionSettings.FORCED_PORT.setValue(1000);
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue(InetAddress.getLocalHost().getHostAddress());
        assertEquals(1000,ProviderHacks.getNetworkManager().getPort());
        assertNotEquals(1000,localPort);
        cmStub.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        ProviderHacks.getUdpService().setReceiveSolicited(true);
        assertEquals(ProviderHacks.getNetworkManager().getPort(),ProviderHacks.getUdpService().getStableUDPPort());
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which carries our local ip address
        ponger1.reply(new Endpoint(ProviderHacks.getNetworkManager().getExternalAddress(),localPort));
        
        Thread.sleep(1000);
        
        // now we should be able to do FWT, and our port should be our local port
        cmStub.setConnected(true);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        assertEquals(localPort,ProviderHacks.getUdpService().getStableUDPPort());
        
        
        // reset the value of num received pongs and send another pong, 
        // carrying our forced address
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_numReceivedIPPongs",
                new Integer(0));
        ponger1.reply(new Endpoint(ProviderHacks.getNetworkManager().getExternalAddress(),1000));
        
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        assertEquals(1000,ProviderHacks.getUdpService().getStableUDPPort());
        
        //clean up
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(false);
    }
    
    /**
     * tests scenarios where we can and can't do solicited
     */
    public void testSolicited() throws Exception{
        cmStub.setConnected(true);
        
        //make sure our port is valid
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        assertTrue(
                NetworkUtils.isValidAddress(ProviderHacks.getNetworkManager().getExternalAddress()));
        
        //if we can't do solicited, we're out
        ProviderHacks.getUdpService().setReceiveSolicited(false);
        assertFalse(ProviderHacks.getUdpService().canReceiveSolicited());
        assertFalse(ProviderHacks.getUdpService().canDoFWT());
        
        ProviderHacks.getUdpService().setReceiveSolicited(true);
        assertTrue(ProviderHacks.getUdpService().canReceiveSolicited());
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
    }
    
    public void testInvalidExternal() throws Exception {
        cmStub.setConnected(true);
        
        // make sure we can receive solicited
        ProviderHacks.getUdpService().setReceiveSolicited(true);
        assertTrue(ProviderHacks.getUdpService().canReceiveSolicited());
        
        // send a pong
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which does has a different port
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        ponger1.reply(new Endpoint(ProviderHacks.getNetworkManager().getExternalAddress(),ProviderHacks.getNetworkManager().getPort()));
        
        Thread.sleep(1000);
        
        // try with valid external address
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        assertTrue(
                NetworkUtils.isValidAddress(ProviderHacks.getNetworkManager().getExternalAddress()));
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        
        // and with an invalid one
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getByName("0.0.0.0"));
        assertFalse(
                NetworkUtils.isValidAddress(ProviderHacks.getNetworkManager().getExternalAddress()));
        assertFalse(ProviderHacks.getUdpService().canDoFWT());
        
    }
    /**
     * tests if the pings are requesting ip:port check properly
     */
    public void testPingsRequesting() throws Exception {
        
        // make sure we have not received incoming connection in the past
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        // if we have received incoming, pings should not be requesting
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        
        assertFalse(ponger1.listen().requestsIP());
        
    }
    
    
    /**
     * tests the case where both pinged hosts reply with the same
     * ip:port.
     */
    public void testPongsCarryGoodInfo() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        
        assertTrue(ponger1.listen().requestsIP());
        assertTrue(ponger2.listen().requestsIP());
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        Endpoint myself = new Endpoint(ProviderHacks.getNetworkManager().getExternalAddress(),
                ProviderHacks.getNetworkManager().getPort());
        ponger1.reply(myself);
        ponger2.reply(myself);
        Thread.sleep(500);
        cmStub.setConnected(true);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        assertFalse(ConnectionSettings.LAST_FWT_STATE.getValue());
        cmStub.setConnected(false);
    }
    
    
    /**
     * tests the case where a pong says we have a different port
     */
    public void testPongCarriesBadPort() throws Exception{
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        
        assertTrue(ponger1.listen().requestsIP());
        assertTrue(ponger2.listen().requestsIP());
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        
        Endpoint badPort1 = new Endpoint(InetAddress.getLocalHost().getAddress(),12345);
        Endpoint badPort2 = new Endpoint(InetAddress.getLocalHost().getAddress(),12346);
        ponger1.reply(badPort1);
        ponger2.reply(badPort2);
        Thread.sleep(500);
        cmStub.setConnected(true);
        assertFalse(ProviderHacks.getUdpService().canDoFWT());
        assertTrue(ConnectionSettings.LAST_FWT_STATE.getValue());
        cmStub.setConnected(false);
    }
    
    /**
     * tests the scenario where a (malicious) pong does not affect our
     * status
     */
    public void testMaliciousPongDoesNotDisable() throws Exception {
        ProviderHacks.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        cmStub.setConnected(true);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        cmStub.setConnected(false);
        ProviderHacks.getHostCatcher().expire();
        ProviderHacks.getHostCatcher().sendUDPPings();
        ponger1.listen();
        Endpoint badAddress = new Endpoint("1.2.3.4",ProviderHacks.getNetworkManager().getPort());
        PingReply badGuid = ProviderHacks.getPingReplyFactory().create(GUID.makeGuid(),(byte)1,badAddress);
        ponger1.replyPong(badGuid);
        Thread.sleep(1000);
        cmStub.setConnected(true);
        assertTrue(ProviderHacks.getUdpService().canDoFWT());
    }
    
    public void testServerResponse() throws Exception {
        cmStub.setConnected(true);
        DatagramSocket sock = new DatagramSocket(20000);
        sock.setSoTimeout(500);
        
        PingRequest with = new PingRequest((byte)1);
        PingRequest without = new PingRequest((byte)1);
        
        with.addIPRequest();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        with.write(baos);
        
        byte [] data = baos.toByteArray();
        DatagramPacket pack = new DatagramPacket(data,data.length,
                new InetSocketAddress(InetAddress.getLocalHost(),ProviderHacks.getNetworkManager().getPort()));
        
        sock.send(pack);
        
        Thread.sleep(100);
        DatagramPacket read = new DatagramPacket(new byte[100],100);
        sock.receive(read);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(read.getData());
        
        PingReply replyWith = (PingReply)MessageFactory.read(bais);
        
        assertNotNull(replyWith.getMyInetAddress());
        assertEquals(InetAddress.getLocalHost(),replyWith.getMyInetAddress());
        assertEquals(20000,replyWith.getMyPort());
        
        // test a ping without
        baos = new ByteArrayOutputStream();
        without.write(baos);
        
        data = baos.toByteArray();
        pack = new DatagramPacket(data,data.length,
                new InetSocketAddress(InetAddress.getLocalHost(),ProviderHacks.getNetworkManager().getPort()));
        
        sock.send(pack);
        
        Thread.sleep(100);
        read = new DatagramPacket(new byte[100],100);
        sock.receive(read);
        bais = new ByteArrayInputStream(read.getData());
        
        PingReply replyWithout = (PingReply)MessageFactory.read(bais);
        
        assertNull(replyWithout.getMyInetAddress());
        assertEquals(0,replyWithout.getMyPort());
        
    }
    
    private static void writeToGnet(String hosts) throws Exception {
        FileOutputStream fos = new FileOutputStream(gnet);
        fos.write(hosts.getBytes());fos.flush();fos.close();
    }
    
    private static class UDPPonger {
        private DatagramSocket _sock;
        private SocketAddress _lastAddress;
        
        public PingReply reply;
        public boolean shouldAsk;
        
        private byte [] lastReceived;
        
        public UDPPonger(int port) {
            try {
            
                _sock = new DatagramSocket(port);
                _sock.setSoTimeout(5000);
            }catch(Exception bad) {
                ErrorService.error(bad);
            }
        }
        
        public PingRequest listen() throws Exception {
            byte [] data = new byte[1024];
                //receive a ping.
                DatagramPacket pack = new DatagramPacket(data,1024);
                _sock.receive(pack);
                _lastAddress = pack.getSocketAddress();
                
                ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
                PingRequest ret = (PingRequest) MessageFactory.read(bais);
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
                toSend = ProviderHacks.getPingReplyFactory().create(lastReceived,(byte)1);
            else
                toSend = ProviderHacks.getPingReplyFactory().create(lastReceived,(byte)1,reply);
         
            replyPong(toSend);
            
            
        }
        
        public void replyPong(PingReply reply) throws Exception{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            reply.write(baos);
            byte []data = baos.toByteArray();
            DatagramPacket pack = new DatagramPacket(data,data.length,_lastAddress);
            _sock.send(pack);
        }
        
        public void drain()  {
            try{
                _sock.setSoTimeout(100);
                try{
                    while(true)
                        _sock.receive(new DatagramPacket(new byte[1000],1000));
                }catch(IOException expected) {}
                _sock.setSoTimeout(5000);
            }catch(SocketException bad) {
                ErrorService.error(bad);
            }
        }
    }
    
    private static class CMStub extends ConnectionManagerStub {
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
        
        public List getInitializedConnections() {
            return Collections.EMPTY_LIST;
        }
    }

}
