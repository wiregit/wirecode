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
            PrivilegedAccessor.setValue(service,"_tcpAddressInitialized",new Boolean(false));
            PrivilegedAccessor.setValue(service,"_previousIP",null);
            PrivilegedAccessor.setValue(service,"_lastReportedIP",null);
            PrivilegedAccessor.setValue(service,"_lastReportedPort",
                    new Integer(RouterService.getPort()));
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
        ConnectionSettings.EVER_DISABLED_FWT.setValue(false);
        assertTrue(UDPService.instance().canDoFWT());
        ConnectionSettings.EVER_DISABLED_FWT.setValue(true);
        assertFalse(UDPService.instance().canDoFWT());
        
    }
    
    /**
     * tets the scenario where we have and have not received a pong 
     */
    public void testNotReceivedPong() throws Exception {
        cmStub.setConnected(true);
        ConnectionSettings.EVER_DISABLED_FWT.setValue(false);
        assertTrue(UDPService.instance().canDoFWT());
        ConnectionSettings.EVER_DISABLED_FWT.setValue(true);
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
        ConnectionSettings.EVER_DISABLED_FWT.setValue(false);
        assertTrue(UDPService.instance().canDoFWT());
        ConnectionSettings.EVER_DISABLED_FWT.setValue(true);
        assertFalse(UDPService.instance().canDoFWT());
        
        //reply with a pong that does carry info
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        Endpoint myself = new Endpoint(RouterService.getExternalAddress(),
                RouterService.getPort());
        ponger1.reply(myself);
        Thread.sleep(1000);
        
        cmStub.setConnected(true);
        ConnectionSettings.EVER_DISABLED_FWT.setValue(false);
        assertTrue(UDPService.instance().canDoFWT());
        ConnectionSettings.EVER_DISABLED_FWT.setValue(true);
        assertTrue(UDPService.instance().canDoFWT());
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
        
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        assertTrue(
                NetworkUtils.isValidAddress(RouterService.getExternalAddress()));
        assertTrue(UDPService.instance().canDoFWT());
        
        RouterService.getAcceptor().setExternalAddress(InetAddress.getByName("0.0.0.0"));
        assertFalse(
                NetworkUtils.isValidAddress(RouterService.getExternalAddress()));
        assertFalse(UDPService.instance().canDoFWT());
        
    }
    /**
     * tests if the pings are requesting ip:port check properly
     *
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
        assertFalse(ConnectionSettings.EVER_DISABLED_FWT.getValue());
        cmStub.setConnected(false);
    }
    
    /**
     * tests the case where both pinged hosts reply with the same
     * ip:port.  Between the first and the second ping we learn our
     * external address from a tcp connection
     */
    public void testPongsCarryGoodInfoBetweenConnect() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        assertTrue(ponger1.listen().requestsIP());
        assertTrue(ponger2.listen().requestsIP());
        
        Endpoint myself = new Endpoint(InetAddress.getLocalHost().getAddress(),
                RouterService.getPort());
        ponger1.reply(myself);
        
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        
        ponger2.reply(myself);
        
        Thread.sleep(500);
        cmStub.setConnected(true);
        assertTrue(UDPService.instance().canDoFWT());
        assertFalse(ConnectionSettings.EVER_DISABLED_FWT.getValue());
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
        
        Endpoint badPort = new Endpoint(InetAddress.getLocalHost().getAddress(),12345);
        ponger1.reply(badPort);
        ponger2.reply(badPort);
        Thread.sleep(500);
        cmStub.setConnected(true);
        assertFalse(UDPService.instance().canDoFWT());
        assertTrue(ConnectionSettings.EVER_DISABLED_FWT.getValue());
        cmStub.setConnected(false);
    }
    
    /**
     * tests the case where the pong says we have different address
     */
    public void testPongCarriesBadAddress() throws Exception {
        
        Endpoint badAddress = new Endpoint("1.2.3.4",RouterService.getPort());
        ponger1.reply(badAddress);
        Thread.sleep(500);
        cmStub.setConnected(true);
        assertFalse(UDPService.instance().canDoFWT());
    }
    
    public void testPongCarriesBadAddressBeforeConnect() throws Exception {
        Endpoint badAddress = new Endpoint("1.2.3.4",RouterService.getPort());
        ponger1.reply(badAddress);
        Thread.sleep(500);
        cmStub.setConnected(true);
        assertFalse(UDPService.instance().canDoFWT());
        
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        
        assertFalse(UDPService.instance().canDoFWT());
    }
    
    /**
     * tests the case where our external address gets updated 
     * and the next pong we receive with the new address does not 
     * disable FWT, nor does a late pong with the old address.
     */
    public void testTCPUpdatePreventsDisabling() throws Exception {
        
        RouterService.getAcceptor().setExternalAddress(InetAddress.getLocalHost());
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        RouterService.getHostCatcher().expire();
        RouterService.getHostCatcher().sendUDPPings();
        
        assertTrue(ponger1.listen().requestsIP());
        assertTrue(ponger2.listen().requestsIP());
        cmStub.setConnected(true);
        
        
        Endpoint myself = new Endpoint(InetAddress.getLocalHost().getAddress(), RouterService.getPort());
        ponger1.reply(myself);
        Thread.sleep(1000);
        
        assertTrue(UDPService.instance().canDoFWT());
        RouterService.getAcceptor().setExternalAddress(InetAddress.getByName("127.0.0.1"));
        
        Endpoint myNewSelf = new Endpoint("127.0.0.1",RouterService.getPort());
        ponger1.reply(myNewSelf);
        
        Thread.sleep(1000);
        assertTrue(UDPService.instance().canDoFWT());
        
        ponger1.reply(myself);
        assertTrue(UDPService.instance().canDoFWT());
        
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
