package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

import junit.framework.Test;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.CommonUtils;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Set;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideFirewalledTransferTest extends ClientSideTestCase {
    protected static final int PORT=6669;
    protected static int TIMEOUT=1000; // should override super

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    public ClientSideFirewalledTransferTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideFirewalledTransferTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    ///////////////////////// Actual Tests ////////////////////////////
    
    // THIS TEST SHOULD BE RUN FIRST!!
    public void testPushProxySetup() throws Exception {
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket();

        // send a MessagesSupportedMessage
        testUP[0].send(MessagesSupportedVendorMessage.instance());
        testUP[0].flush();

        // we expect to get a PushProxy request
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof PushProxyRequest)) ;

        // we should answer the push proxy request
        PushProxyAcknowledgement ack = 
        new PushProxyAcknowledgement(InetAddress.getLocalHost(), 
                                     6355, new GUID(m.getGUID()));
        testUP[0].send(ack);
        testUP[0].flush();

        // client side seems to follow the setup process A-OK
        // set up solicted support
        {
            drainAll();
            PingReply pong = 
                PingReply.create(GUID.makeGuid(), (byte) 4,
                                 UDP_ACCESS.getLocalPort(), 
                                 InetAddress.getLocalHost().getAddress(), 
                                 10, 10, true, 900, true);
            testUP[0].send(pong);
            testUP[0].flush();

            // wait for the ping request from the test UP
            UDP_ACCESS.setSoTimeout(2000);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
               fail("Did not get ping", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            PingRequest ping = (PingRequest) Message.read(in);
            
            // send the pong in response to the ping
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong = PingReply.create(ping.getGUID(), (byte) 4,
                                    UDP_ACCESS.getLocalPort(), 
                                    InetAddress.getLocalHost().getAddress(), 
                                    10, 10, true, 900, true);
            pong.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      pack.getAddress(), pack.getPort());
            UDP_ACCESS.send(pack);
        }
        Thread.sleep(1000);
        assertTrue(UDPService.instance().canReceiveSolicited());
    }

    public void testStartsUDPTransfer() throws Exception {

        drain(testUP[0]);

        // make sure leaf is sharing
        assertEquals(2, RouterService.getFileManager().getNumFiles());

        // send a query that should be answered
        QueryRequest query = new QueryRequest(GUID.makeGuid(), (byte)2, 
                                 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                                 QueryRequest.SPECIAL_FIREWALL_MASK |
                                 QueryRequest.SPECIAL_FWTRANS_MASK,
                                 "berkeley", "", null, 
                                 null, null, false, Message.N_UNKNOWN, false, 
                                 0, false, 0);
        testUP[0].send(query);
        testUP[0].flush();

        // await a response
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryReply)) ;

        // confirm it has proxy info
        QueryReply reply = (QueryReply) m;
        assertNotNull(reply.getPushProxies());

        // check out PushProxy info
        Set proxies = reply.getPushProxies();
        assertEquals(1, proxies.size());
        Iterator iter = proxies.iterator();
        PushProxyInterface ppi = (PushProxyInterface)iter.next();
        assertEquals(ppi.getPushProxyPort(), 6355);
        assertTrue(ppi.getPushProxyAddress().equals(InetAddress.getLocalHost()));

        // set up a ServerSocket to get give on
        ServerSocket ss = new ServerSocket(9000);
        ss.setReuseAddress(true);        
        ss.setSoTimeout(TIMEOUT);

        // test that the client responds to a PushRequest
        PushRequest pr = new PushRequest(GUID.makeGuid(), (byte) 1, 
                                         RouterService.getMessageRouter()._clientGUID,
                                         Integer.MAX_VALUE, 
                                         InetAddress.getLocalHost().getAddress(),
                                         9000);
        
        // send the PR off
        testUP[0].send(pr);
        testUP[0].flush();

        // we should NOT get a incoming GIV
        try {
            Socket givSock = ss.accept();
            assertTrue(false);
        }
        catch (InterruptedIOException expected) {}
    }

    //////////////////////////////////////////////////////////////////
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
