package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.search.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.bitzi.util.*;

import junit.framework.*;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideMixedOOBGuidanceTest extends ClientSideTestCase {

    private static MyActivityCallback callback;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    public ClientSideMixedOOBGuidanceTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideMixedOOBGuidanceTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /** @return The first QueyrRequest received from this connection.  If null
     *  is returned then it was never recieved (in a timely fashion).
     */
    private static QueryStatusResponse getFirstQueryStatus(Connection c) {
        return (QueryStatusResponse)
            getFirstInstanceOfMessageType(c, QueryStatusResponse.class, TIMEOUT);
    }

    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testMixedProtocol() throws Exception {
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket();

        for (int i = 0; i < testUP.length; i++) {
            assertTrue("not open", testUP[i].isOpen());
            assertTrue("not up->leaf", testUP[i].isSupernodeClientConnection());
            drain(testUP[i], 500);
            if ((i==2)) { // i'll send 0 later....
                testUP[i].send(MessagesSupportedVendorMessage.instance());
                testUP[i].flush();
            }
        }

        // first we need to set up GUESS capability
        // ----------------------------------------
        // set up solicited UDP support
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
            UDP_ACCESS.setSoTimeout(TIMEOUT*2);
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

        // set up unsolicited UDP support
        {
            // resend this to start exchange
            testUP[0].send(MessagesSupportedVendorMessage.instance());
            testUP[0].flush();

            byte[] cbGuid = null;
            int cbPort = -1;
            while (cbGuid == null) {
                try {
                    Message m = testUP[0].receive(TIMEOUT);
                    if (m instanceof UDPConnectBackVendorMessage) {
                        UDPConnectBackVendorMessage udp = 
                            (UDPConnectBackVendorMessage) m;
                        cbGuid = udp.getConnectBackGUID().bytes();
                        cbPort = udp.getConnectBackPort();
                    }
                }
                catch (Exception ie) {
                    fail("did not get the UDP CB message!", ie);
                }
            }

            // ok, now just do a connect back to the up so unsolicited support
            // is all set up
            PingRequest pr = new PingRequest(cbGuid, (byte) 1, (byte) 0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pr.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      testUP[0].getInetAddress(), cbPort);
            UDP_ACCESS.send(pack);
        }

        // you also have to set up TCP incoming....
        {
            Socket sock = null;
            OutputStream os = null;
            try {
                sock=Sockets.connect(InetAddress.getLocalHost().getHostAddress(),
                                     SERVER_PORT, 12);
                os = sock.getOutputStream();
                os.write("\n\n".getBytes());
            } catch (IOException ignored) {
            } catch (SecurityException ignored) {
            } catch (Throwable t) {
                ErrorService.error(t);
            } finally {
                if(sock != null)
                    try { sock.close(); } catch(IOException ignored) {}
                if(os != null)
                    try { os.close(); } catch(IOException ignored) {}
            }
        }        

        // ----------------------------------------

        Thread.sleep(250);
        // we should now be guess capable and tcp incoming capable....
        assertTrue(rs.isGUESSCapable());
        assertTrue(rs.acceptedIncomingConnection());
        
        // get rid of any messages that are stored up.
        drainAll();

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(rs.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), rs.getAddress(), 
                                       rs.getPort()));
        rs.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // some connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUP[i], TIMEOUT);
            assertNotNull("up " + i + " didn't get query", qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            if ((i==0) || (i==2))
                assertTrue(qr.desiresOutOfBandReplies());
            else
                assertTrue(!qr.desiresOutOfBandReplies());
        }

        // now confirm that we leaf guide the 'even' guys but not the others.
        Message m = null;
        assertGreaterThan(SearchResultHandler.REPORT_INTERVAL, 4*testUP.length);
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[4];
            res[0] = new Response(10, 10, "susheel"+i);
            res[1] = new Response(10, 10, "susheel smells good"+i);
            res[2] = new Response(10, 10, "anita is sweet"+i);
            res[3] = new Response(10, 10, "anita is prety"+i);
            m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);

            testUP[i].send(m);
            testUP[i].flush();
        }
        
        // all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            if ((i==0) || (i==2)) {
                assertNotNull(stat);
                assertEquals(new GUID(stat.getGUID()), queryGuid);
                assertEquals(4, stat.getNumResults());
            }
            else
                assertNull(stat);
        }

        // shut off the query....
        rs.stopQuery(queryGuid);

        // all UPs should get a QueryStatusResponse with 65535
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            if ((i==0) || (i==2)) {
                assertNotNull(stat);
                assertEquals(new GUID(stat.getGUID()), queryGuid);
                assertEquals(65535, stat.getNumResults());
            }
            else
                assertNull(stat);
        }

    }


    //////////////////////////////////////////////////////////////////

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    public static Integer numUPs() {
        return new Integer(4);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
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
