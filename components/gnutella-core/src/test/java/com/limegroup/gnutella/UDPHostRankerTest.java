package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

import junit.framework.Test;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.IpPort;
import com.sun.java.util.collections.*;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class UDPHostRankerTest extends ClientSideTestCase {
    protected static final int PORT=6669;
    protected static int TIMEOUT=1000; // should override super

    public UDPHostRankerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UDPHostRankerTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    ///////////////////////// Actual Tests ////////////////////////////

    public void testRanker() throws Exception {
        DatagramSocket[] udps = new DatagramSocket[20];
        for (int i = 0; i < udps.length; i++) udps[i] = new DatagramSocket();
        
        final List list = new ArrayList();
        for (int i = 0; i < udps.length; i++) list.add(new IpPortImpl(udps[i]));

        final MLImpl ml = new MLImpl();

        Thread newThread = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException no) {}
                    UDPHostRanker.rank(list, ml);
                }
            };
        newThread.start();

        for (int i = 0; i < udps.length; i++) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            udps[i].setSoTimeout(TIMEOUT);
            try {
                udps[i].receive(pack);
            }
            catch (IOException bad) {
               fail("Did not get expected message, i = " + i, bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            PingRequest ping = (PingRequest) Message.read(in);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PingReply pong = PingReply.create(ping.getGUID(), (byte)1);
            pong.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      InetAddress.getLocalHost(), PORT);
            udps[i].send(pack);
        }

        Thread.sleep(2000); //processing
        assertEquals(udps.length, ml.count);

        // wait 20 seconds, make sure MessageRouter's map is clear
        Thread.sleep(20000);
        Map map = 
        (Map) PrivilegedAccessor.getValue(RouterService.getMessageRouter(),
                                          "_messageListeners");
        assertEquals(0, map.size());
    }

    
    public void testTCPMessageListener() throws Exception {
        // UDP was tested above, so just do a Q&D test for TCP stuff.
        final GUID guid = new GUID(GUID.makeGuid());
        final MLImpl ml = new MLImpl();

        PingReply pong = PingReply.create(guid.bytes(), (byte) 2);

        rs.getMessageRouter().registerMessageListener(guid, ml);
        Map map = 
        (Map) PrivilegedAccessor.getValue(RouterService.getMessageRouter(),
                                          "_messageListeners");
        assertEquals(1, map.size());
        assertEquals(0, ml.count);

        // send off the pong for processing
        testUP[0].send(pong);
        testUP[0].flush();
        Thread.sleep(2000);
        
        rs.getMessageRouter().unregisterMessageListener(guid);
        assertEquals(0, map.size());
        assertEquals(1, ml.count);
    }

    //////////////////////////////////////////////////////////////////
    
    private class MLImpl implements MessageListener {
        public int count = 0;
        public void processMessage(Message m) {
            assertTrue(m instanceof PingReply);
            count++;
        }
    }
    
    private class IpPortImpl implements IpPort {
        DatagramSocket _ds;
        public IpPortImpl(DatagramSocket ds) {
            _ds = ds;
        }
        public InetAddress getInetAddress() { 
            try {
                return InetAddress.getLocalHost();
            }
            catch (Exception noway) {}
            return null;
        }
        public int getPort() { return _ds.getLocalPort(); }
        public String getAddress() {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            }
            catch (Exception noway) {}
            return null;
        }
        public boolean isSame(IpPort other) {return false;} //stub
    }

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    private static byte[] myIP() {
        return new byte[] { (byte)192, (byte)168, 0, 1 };
    }

    public static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc rfd = null;
        public RemoteFileDesc getRFD() {
            return rfd;
        }

        public void handleQueryResult(RemoteFileDesc rfd,
                                      HostData data,
                                      Set locs) {
            this.rfd = rfd;
        }
    }


}
