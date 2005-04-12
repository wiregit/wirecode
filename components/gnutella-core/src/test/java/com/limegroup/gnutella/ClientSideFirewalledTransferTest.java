package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.Test;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.udpconnect.SynMessage;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.IpPortSet;
import com.limegroup.gnutella.util.PrivilegedAccessor;

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
        UDP_ACCESS = new DatagramSocket(9000);

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

        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
                                    "_externalAddress",
                                    new byte[] {(byte) 10, (byte) 07,
                                                (byte) 19, (byte) 76});

        drain(testUP[0]);
        drainUDP();

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
        assertEquals(reply.getIP(), "10.7.19.76", reply.getIP());
        assertTrue(reply.getSupportsFWTransfer());
        assertEquals(UDPConnection.VERSION, reply.getFWTransferVersion());
        assertNotNull(reply.getPushProxies());

        // check out PushProxy info
        Set proxies = reply.getPushProxies();
        assertEquals(1, proxies.size());
        Iterator iter = proxies.iterator();
        IpPort ppi = (IpPort)iter.next();
        assertEquals(ppi.getPort(), 6355);
        assertTrue(ppi.getInetAddress().equals(InetAddress.getLocalHost()));

        // set up a ServerSocket to get give on
        ServerSocket ss = new ServerSocket(9000);
        ss.setReuseAddress(true);        
        ss.setSoTimeout(TIMEOUT);

        // test that the client responds to a PushRequest
        PushRequest pr = new PushRequest(GUID.makeGuid(), (byte) 1, 
                                         RouterService.getMessageRouter()._clientGUID,
                                         PushRequest.FW_TRANS_INDEX, 
                                         InetAddress.getLocalHost().getAddress(),
                                         9000);
        
        // send the PR off
        testUP[0].send(pr);
        testUP[0].flush();

        // we should get an incoming UDP transmission
        while (true) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get SYN", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            Message syn = Message.read(in);
            if (syn instanceof SynMessage) break;
        }

        // but we should NOT get a incoming GIV
        try {
            Socket givSock = ss.accept();
            assertTrue(false);
        }
        catch (InterruptedIOException expected) {}
        ss.close();
    }


    public void testHTTPRequest() throws Exception {
        drain(testUP[0]);
        drainUDP();
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "boalt.org");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        ss.setReuseAddress(true);        
        ss.setSoTimeout(15*TIMEOUT);

        // send a reply with some PushProxy info
        Set proxies = new IpPortSet();
        proxies.add(new IpPortImpl("127.0.0.1", 7000));
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "boalt.org");
        m = new QueryReply(m.getGUID(), (byte) 1, 9000, myIP(), 0, res, 
                           clientGUID, new byte[0], true, false, true,
                           true, false, false, true, proxies);
        testUP[0].send(m);
        testUP[0].flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        final RemoteFileDesc rfd =((MyActivityCallback)getCallback()).getRFD(); 
        assertNotNull(rfd);
        assertNotNull(rfd.getPushAddr());
        assertTrue(rfd.needsPush());

        // tell the leaf to download the file, should result in push proxy
        // request
        final GUID fGuid = new GUID(m.getGUID());
        Thread runLater = new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                    RouterService.download((new RemoteFileDesc[]{rfd}), 
                                           true, fGuid);
                }
                catch (Exception damn) {
                    assertTrue(false);
                }
            }
        };
        runLater.start();

        // wait for the incoming HTTP request
        Socket httpSock = ss.accept();
        assertNotNull(httpSock);

        // start reading and confirming the HTTP request
        String currLine = null;
        BufferedReader reader = 
            new BufferedReader(new
                               InputStreamReader(httpSock.getInputStream()));

        // confirm a GET/HEAD pushproxy request
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("GET /gnutella/push-proxy") ||
                   currLine.startsWith("HEAD /gnutella/push-proxy"));
        
        // make sure it sends the correct client GUID
        int beginIndex = currLine.indexOf("ID=") + 3;
        String guidString = currLine.substring(beginIndex, beginIndex+26);
        GUID guidFromBackend = new GUID(clientGUID);
        GUID guidFromNetwork = new GUID(Base32.decode(guidString));
        assertEquals(guidFromNetwork, guidFromBackend);

        // make sure it sends back the correct index
        beginIndex = currLine.indexOf("&file=") + 6;
        String longString = currLine.substring(beginIndex, beginIndex+10);
        long index = Long.parseLong(longString);
        assertEquals(index, PushRequest.FW_TRANS_INDEX);

        // make sure the node sends the correct X-Node
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("X-Node:"));
        StringTokenizer st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "X-Node");
        InetAddress addr = InetAddress.getByName(st.nextToken().trim());
        assertEquals(addr.getAddress(), RouterService.getExternalAddress());
        assertEquals(Integer.parseInt(st.nextToken()), PORT);

        // send back a 202 and make sure no PushRequest is sent via the normal
        // way
        BufferedWriter writer = 
            new BufferedWriter(new
                               OutputStreamWriter(httpSock.getOutputStream()));
        
        writer.write("HTTP/1.1 202 gobbledygook");
        writer.flush();
        httpSock.close();

        Thread.sleep(500);

        // we should get an incoming UDP transmission
        while (true) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get SYN", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            Message syn = Message.read(in);
            if (syn instanceof SynMessage) break;
        }

        try {
            do {
                m = testUP[0].receive(TIMEOUT);
                assertTrue(!(m instanceof PushRequest));
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}

        // awesome - everything checks out!
        ss.close();
    }


    //////////////////////////////////////////////////////////////////
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
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

    public void drainUDP() throws Exception {
        while (true) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (InterruptedIOException bad) {
                break;
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            Message syn = Message.read(in);
        }
    }



}
