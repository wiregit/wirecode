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

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * this class tests that the node properly detects if it is
 * capable of firewall to firewall transfers.
 */
public class FWTDetectionTest extends BaseTestCase {
    
    public FWTDetectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FWTDetectionTest.class);
    }

    //make sure the RouterService class gets loaded before the UDPService class
    static RouterService router = new RouterService(new ActivityCallbackStub());
    
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
        router.start();
        
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
            UDPService service = UDPService.instance();
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
        assertTrue(UDPService.instance().canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(UDPService.instance().canDoFWT());
        
    }
    
    /**
     * tets the scenario where we have and have not received a pong 
     */
    public void testNotReceivedPong() throws Exception {
        cmStub.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(UDPService.instance().canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(UDPService.instance().canDoFWT());
        
        cmStub.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //reply with a pong that does not carry info
        ponger1.reply(null);
        cmStub.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(UDPService.instance().canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertFalse(UDPService.instance().canDoFWT());
        
        //reply with a pong that does carry info
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        Endpoint myself = new Endpoint(RouterService.getExternalAddress(),
                RouterService.getPort());
        ponger1.reply(myself);
        Thread.sleep(1000);
        
        cmStub.setConnected(true);
        ConnectionSettings.LAST_FWT_STATE.setValue(false);
        assertTrue(UDPService.instance().canDoFWT());
        ConnectionSettings.LAST_FWT_STATE.setValue(true);
        assertTrue(UDPService.instance().canDoFWT());
    }
    
    /**
     * tests the scenarios where we have received more than one pongs,
     * sometimes reporting different ports.
     */
    public void testReceivedManyPongs() throws Exception {
        cmStub.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        UDPService.instance().setReceiveSolicited(true);
        assertEquals(RouterService.getPort(),UDPService.instance().getStableUDPPort());
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which does has a different port
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        ponger1.reply(new Endpoint(RouterService.getExternalAddress(),1000));
        
        Thread.sleep(1000);
        
        cmStub.setConnected(true);
        assertFalse(UDPService.instance().canDoFWT());
        
        // our external port should still point to our router service port
        assertEquals(RouterService.getPort(),UDPService.instance().getStableUDPPort());
        
        // send a second pong.. now we should be able to do FWT
        ponger1.reply(new Endpoint(RouterService.getExternalAddress(),1000));
        Thread.sleep(1000);
        
        cmStub.setConnected(true);
        assertTrue(UDPService.instance().canDoFWT());
        
        // and our external port should become the new port
        assertEquals(1000,UDPService.instance().getStableUDPPort());
    }
    
    /**
     * tests the scenario where we have a forced port different from our 
     * external port
     */
    public void testForcedPort() throws Exception {
        int localPort = RouterService.getAcceptor().getPort(false);
        ConnectionSettings.FORCED_PORT.setValue(1000);
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue(InetAddress.getLocalHost().getHostAddress());
        assertEquals(1000,RouterService.getPort());
        assertNotEquals(1000,localPort);
        cmStub.setConnected(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        UDPService.instance().setReceiveSolicited(true);
        assertEquals(RouterService.getPort(),UDPService.instance().getStableUDPPort());
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which carries our local ip address
        ponger1.reply(new Endpoint(RouterService.getExternalAddress(),localPort));
        
        Thread.sleep(1000);
        
        // now we should be able to do FWT, and our port should be our local port
        cmStub.setConnected(true);
        assertTrue(UDPService.instance().canDoFWT());
        assertEquals(localPort,UDPService.instance().getStableUDPPort());
        
        
        // reset the value of num received pongs and send another pong, 
        // carrying our forced address
        PrivilegedAccessor.setValue(UDPService.instance(),"_numReceivedIPPongs",
                new Integer(0));
        ponger1.reply(new Endpoint(RouterService.getExternalAddress(),1000));
        
        assertTrue(UDPService.instance().canDoFWT());
        assertEquals(1000,UDPService.instance().getStableUDPPort());
        
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
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        assertTrue(
                NetworkUtils.isValidAddress(RouterService.getExternalAddress()));
        
        //if we can't do solicited, we're out
        UDPService.instance().setReceiveSolicited(false);
        assertFalse(UDPService.instance().canReceiveSolicited());
        assertFalse(UDPService.instance().canDoFWT());
        
        UDPService.instance().setReceiveSolicited(true);
        assertTrue(UDPService.instance().canReceiveSolicited());
        assertTrue(UDPService.instance().canDoFWT());
    }
    
    public void testInvalidExternal() throws Exception {
        cmStub.setConnected(true);
        
        // make sure we can receive solicited
        UDPService.instance().setReceiveSolicited(true);
        assertTrue(UDPService.instance().canReceiveSolicited());
        
        // send a pong
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //send one pong which does has a different port
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        ponger1.reply(new Endpoint(RouterService.getExternalAddress(),RouterService.getPort()));
        
        Thread.sleep(1000);
        
        // try with valid external address
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        assertTrue(
                NetworkUtils.isValidAddress(RouterService.getExternalAddress()));
        assertTrue(UDPService.instance().canDoFWT());
        
        // and with an invalid one
        RouterService.getAcceptor().setExternalAddress(InetAddress.getByName("0.0.0.0"));
        assertFalse(
                NetworkUtils.isValidAddress(RouterService.getExternalAddress()));
        assertFalse(UDPService.instance().canDoFWT());
        
    }
    /**
     * tests if the pings are requesting ip:port check properly
     */
    public void testPingsRequesting() throws Exception {
        
        // make sure we have not received incoming connection in the past
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n");
        
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        // if we have received incoming, pings should not be requesting
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        assertFalse(ponger1.listen().requestsIP());
        
    }
    
    
    /**
     * tests the case where both pinged hosts reply with the same
     * ip:port.
     */
    public void testPongsCarryGoodInfo() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        assertTrue(ponger1.listen().requestsIP());
        assertTrue(ponger2.listen().requestsIP());
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        Endpoint myself = new Endpoint(RouterService.getExternalAddress(),
                RouterService.getPort());
        ponger1.reply(myself);
        ponger2.reply(myself);
        Thread.sleep(500);
        cmStub.setConnected(true);
        assertTrue(UDPService.instance().canDoFWT());
        assertFalse(ConnectionSettings.LAST_FWT_STATE.getValue());
        cmStub.setConnected(false);
    }
    
    
    /**
     * tests the case where a pong says we have a different port
     */
    public void testPongCarriesBadPort() throws Exception{
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        assertTrue(ponger1.listen().requestsIP());
        assertTrue(ponger2.listen().requestsIP());
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        
        Endpoint badPort1 = new Endpoint(InetAddress.getLocalHost().getAddress(),12345);
        Endpoint badPort2 = new Endpoint(InetAddress.getLocalHost().getAddress(),12346);
        ponger1.reply(badPort1);
        ponger2.reply(badPort2);
        Thread.sleep(500);
        cmStub.setConnected(true);
        assertFalse(UDPService.instance().canDoFWT());
        assertTrue(ConnectionSettings.LAST_FWT_STATE.getValue());
        cmStub.setConnected(false);
    }
    
    /**
     * tests the scenario where a (malicious) pong does not affect our
     * status
     */
    public void testMaliciousPongDoesNotDisable() throws Exception {
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        cmStub.setConnected(true);
        assertTrue(UDPService.instance().canDoFWT());
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        cmStub.setConnected(false);
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        ponger1.listen();
        Endpoint badAddress = new Endpoint("1.2.3.4",RouterService.getPort());
        PingReply badGuid = PingReply.create(GUID.makeGuid(),(byte)1,badAddress);
        ponger1.replyPong(badGuid);
        Thread.sleep(1000);
        cmStub.setConnected(true);
        assertTrue(UDPService.instance().canDoFWT());
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
                new InetSocketAddress(InetAddress.getLocalHost(),RouterService.getPort()));
        
        sock.send(pack);
        
        Thread.sleep(100);
        DatagramPacket read = new DatagramPacket(new byte[100],100);
        sock.receive(read);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(read.getData());
        
        PingReply replyWith = (PingReply)Message.read(bais);
        
        assertNotNull(replyWith.getMyInetAddress());
        assertEquals(InetAddress.getLocalHost(),replyWith.getMyInetAddress());
        assertEquals(20000,replyWith.getMyPort());
        
        // test a ping without
        baos = new ByteArrayOutputStream();
        without.write(baos);
        
        data = baos.toByteArray();
        pack = new DatagramPacket(data,data.length,
                new InetSocketAddress(InetAddress.getLocalHost(),RouterService.getPort()));
        
        sock.send(pack);
        
        Thread.sleep(100);
        read = new DatagramPacket(new byte[100],100);
        sock.receive(read);
        bais = new ByteArrayInputStream(read.getData());
        
        PingReply replyWithout = (PingReply)Message.read(bais);
        
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
        
        private GUID _solGUID;
        
        public UDPPonger(int port) {
            try {
                _solGUID=(GUID) PrivilegedAccessor.getValue(
                    UDPService.instance(),"SOLICITED_PING_GUID");
            
            
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
                    
                return (PingRequest) Message.read(bais); 
        }
        
        /**
         * send a pong with the specified address back to the pinger.
         * it uses the solicited ping
         */
        public void reply(IpPort reply) throws Exception{
            PingReply toSend;
            if (reply==null)
                toSend = PingReply.create(_solGUID.bytes(),(byte)1);
            else
                toSend = PingReply.create(_solGUID.bytes(),(byte)1,reply);
         
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
