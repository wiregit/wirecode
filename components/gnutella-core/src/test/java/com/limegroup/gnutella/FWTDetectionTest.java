package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import junit.framework.Test;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.*;

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
    
    static File originalNet, tempNet;
    

    
    
    static CMStub cmStub;
    /**
     * the basic testing routine is a node with a few hosts in its gnutella.net
     * the node sends an initial ping to them, and they return various
     * pongs.
     */
    public static void globalSetUp() {
        
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        
        cmStub = new CMStub();
        
        try{
            PrivilegedAccessor.setValue(RouterService.class,"manager",cmStub);
        }catch(Exception bad) {
            ErrorService.error(bad);
        }
        router.start();
        
        cmStub.setConnected(true);
        assertTrue(RouterService.isConnected());
        cmStub.setConnected(false);
        assertFalse(RouterService.isConnected());
        
        
        // move our existing gnutella.net out of the way
        originalNet = new File(CommonUtils.getUserSettingsDir(), 
        	"gnutella.net");
        
        if ( originalNet.exists() ) {
            tempNet = new File("gdotnet.tmp");
            tempNet.delete();
            originalNet.renameTo( tempNet );
        }
        
    }
    
    public static void globalTearDown() {
        // restore our original gnutella.net
        tempNet = new File("gdotnet.tmp");
        if (tempNet.exists()) {
            originalNet = new File(CommonUtils.getUserSettingsDir(), 
        		"gnutella.net");
            originalNet.delete();
            tempNet.renameTo(originalNet);
        }
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
                    new Integer(6348));
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
        
        connectAsync();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        //reply with a pong that does not carry info
        ponger1.reply(null);
        
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
        
        connectAsync();
        
        //we should receive a udp ping requesting ip
        assertTrue(ponger1.listen().requestsIP());
        
        // if we have received incoming, pings should not be requesting
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        connectAsync();
        
        assertFalse(ponger1.listen().requestsIP());
        
    }
    
    
    /**
     * tests the case where both pinged hosts reply with the same
     * ip:port.
     */
    public void testPongsCarryGoodInfo() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        writeToGnet("127.0.0.1:"+REMOTE_PORT1+"\n"+"127.0.0.1:"+REMOTE_PORT2+"\n");
        connectAsync();
        
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
        connectAsync();
        
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
        connectAsync();
        
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
        connectAsync();
        
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
    
    private static void writeToGnet(String hosts) throws Exception {
        File gnet = new File(CommonUtils.getUserSettingsDir(), 
    		"gnutella.net");
        FileOutputStream fos = new FileOutputStream(gnet);
        
        fos.write(hosts.getBytes());fos.flush();fos.close();
    }
    
    private static void connectAsync() {
        Thread t = new Thread() {
            public void run() {
                HostCatcher catcher = new HostCatcher();
                catcher.initialize();
            }
        };
        t.setDaemon(true);
        t.start();
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
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            toSend.write(baos);
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
    }

}
