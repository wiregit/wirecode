package com.limegroup.gnutella;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

import org.limewire.service.ErrorService;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.SocketsManager;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
@SuppressWarnings("all")
public class ClientSideMixedOOBGuidanceTest extends ClientSideTestCase {

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
    
    /** @return The first QueryRequest received from this connection.  If null
     *  is returned then it was never recieved (in a timely fashion).
     */
    private static QueryStatusResponse getFirstQueryStatus(Connection c) 
                                       throws IOException, BadPacketException {
        return (QueryStatusResponse)
            getFirstInstanceOfMessageType(c, QueryStatusResponse.class, TIMEOUT);
    }

    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testMixedProtocol() throws Exception {
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
        
        testUP[0].send(MessagesSupportedVendorMessage.instance());
        testUP[0].flush();
        
        // first we need to set up GUESS capability
        UDP_ACCESS.setSoTimeout(TIMEOUT*2);
        // ----------------------------------------
        // set up solicited UDP support
        PrivilegedAccessor.setValue( RouterService.getUdpService(), "_acceptedSolicitedIncoming", Boolean.TRUE );

        // set up unsolicited UDP support
        PrivilegedAccessor.setValue( RouterService.getUdpService(), "_acceptedUnsolicitedIncoming", Boolean.TRUE );
 
        // you also have to set up TCP incoming....
        {
            Socket sock = null;
            OutputStream os = null;
            try {
                sock=SocketsManager.getSharedManager().connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                     SERVER_PORT), 12);
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
        assertTrue(RouterService.isGUESSCapable());
        assertTrue(RouterService.acceptedIncomingConnection());
        
        // get rid of any messages that are stored up.
        drainAll();

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(RouterService.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                       RouterService.getAddress(), 
                       RouterService.getPort()));
        RouterService.query(queryGuid.bytes(), "susheel");
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
        // ensure that we'll get a QueryStatusResponse from the Responses
        // we're sending.
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[] {
                // Only the 'susheel' Responses will pass the 
                // ResponseVerifier.matchesQuery() check and 
                // the others wont
                new Response(10, 10, "susheel"+i),
                new Response(10, 10, "susheel smells good"+i),
                new Response(10, 10, "anita is sweet"+i),
                new Response(10, 10, "anita is prety"+i),
                new Response(10, 10, "susheel smells bad" + i),
                new Response(10, 10, "renu is sweet " + i),
                new Response(10, 10, "prety is spelled pretty " + i),
                new Response(10, 10, "go susheel go" + i),
                new Response(10, 10, "susheel runs fast" + i),
                new Response(10, 10, "susheel jumps high" + i),
                new Response(10, 10, "sleepy susheel" + i),
            };
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
                assertEquals(5, stat.getNumResults());
            }
            else
                assertNull(stat);
        }

        // shut off the query....
        RouterService.stopQuery(queryGuid);

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
        return new Integer(3);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    
    public static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc rfd = null;
        public RemoteFileDesc getRFD() {
            return rfd;
        }

        public void handleQueryResult(RemoteFileDesc returnedRfd,
                                      HostData data,
                                      Set locs) {
            this.rfd = returnedRfd;
        }
    }
}
