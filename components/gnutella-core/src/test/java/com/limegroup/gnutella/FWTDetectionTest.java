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

import junit.framework.Test;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
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
    
    static int REMOTE_PORT1 = 10000;
    static int REMOTE_PORT2 = 10001;
    
    static UDPPonger ponger1 = new UDPPonger(REMOTE_PORT1);
    static UDPPonger ponger2 = new UDPPonger(REMOTE_PORT2);
    
    static File originalNet, tempNet;
    
    static RouterService router;
    /**
     * the basic testing routine is a node with a few hosts in its gnutella.net
     * the node sends an initial ping to them, and they return various
     * pongs.
     */
    public static void globalSetUp() {
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        router = new RouterService(new ActivityCallbackStub());
        router.start();
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
        
        public UDPPonger(int port) {
            try {
                _sock = new DatagramSocket(port);
                _sock.setSoTimeout(5000);
            }catch(IOException bad) {
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
         * for the specific test we don't care about matching the guids 
         */
        public void reply(IpPort reply) throws Exception{
            PingReply toSend;
            if (reply==null)
                toSend = PingReply.create(GUID.makeGuid(),(byte)1);
            else
                toSend = PingReply.create(GUID.makeGuid(),(byte)1,reply);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            toSend.write(baos);
            byte []data = baos.toByteArray();
            DatagramPacket pack = new DatagramPacket(data,data.length,_lastAddress);
            _sock.send(pack);
            
        }
    }

}
