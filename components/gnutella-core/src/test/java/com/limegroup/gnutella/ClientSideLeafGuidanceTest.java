package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.sun.java.util.collections.Set;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideLeafGuidanceTest extends ClientSideTestCase {

    private static MyActivityCallback callback;

    private final int REPORT_INTERVAL = SearchResultHandler.REPORT_INTERVAL;
    private final int MAX_RESULTS = SearchResultHandler.MAX_RESULTS;

    public ClientSideLeafGuidanceTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideLeafGuidanceTest.class);
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
    
    // THIS TEST SHOULD BE RUN FIRST!!
    public void testBasicGuidance() throws Exception {
        
        for (int i = 0; i < testUP.length; i++)
            // send a MessagesSupportedMessage
            testUP[i].send(MessagesSupportedVendorMessage.instance());

        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(RouterService.newQueryGUID());
        RouterService.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Message m = null;
        assertGreaterThan(REPORT_INTERVAL, 4*testUP.length);
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
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(4, stat.getNumResults());
        }

        // shut off the query....
        RouterService.stopQuery(queryGuid);

        // all UPs should get a QueryStatusResponse with 65535
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(65535, stat.getNumResults());
        }
    }


    public void testAdvancedGuidance1() throws Exception {

        for (int i = 0; i < testUP.length; i++)
            drain(testUP[i]);
        
        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(RouterService.newQueryGUID());
        RouterService.query(queryGuid.bytes(), "susheel daswanu");

        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Message m = null;
        for (int i = 0; i < testUP.length; i++) {
            // since i know REPORT_INTERVAL is 15, i'm sending 45, then 90, then
            // 135, then 180 - after 180 leaf guidance should shut off....
            Response[] res = new Response[REPORT_INTERVAL*3];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10, 10, "susheel good"+i+j);

            m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);

            testUP[i].send(m);
            testUP[i].flush();
        }
        
        // all UPs should get a QueryStatusResponse
        boolean maxResultsEncountered = false;
        for (int i = 0; i < testUP.length; i++) {
            for (int j = 0; j < testUP.length; j++) {
                QueryStatusResponse stat = getFirstQueryStatus(testUP[j]);
                assertNotNull(stat);
                assertEquals(new GUID(stat.getGUID()), queryGuid);
                // depending on how far along the query is we could have a
                // number or 65535 - the number 11 depends on settings such as
                // REPORT_INTERVAL
                assertTrue((stat.getNumResults() == 11*(i+1)) ||
                           (stat.getNumResults() == MAX_RESULTS));
                if (stat.getNumResults() == MAX_RESULTS) {
                    assertTrue(i == testUP.length-1);
                    maxResultsEncountered = true;
                }
            }
        }
        assertTrue(maxResultsEncountered);

        // now, even though we send more responses, we shoudl NOT get any more
        // leaf guidance...
        Response[] res = new Response[REPORT_INTERVAL*4];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is pretty"+j);
        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUP[0].send(m);
        testUP[0].flush();

        // no UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            final int index = i;
            Thread newThread = new Thread() {
                    public void run() {
                        try {
                            QueryStatusResponse stat = 
                                getFirstQueryStatus(testUP[index]);
                            assertNull(stat);
                        }
                        catch (Exception e) {
                            assertNull(e);
                        }
                    }
                };
            newThread.start();
        }
    }


    public void testAdvancedGuidance2() throws Exception {

        Message m = null;

        for (int i = 0; i < testUP.length; i++)
            drain(testUP[i]);
        
        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(RouterService.newQueryGUID());
        RouterService.query(queryGuid.bytes(), "anita kesavan");

        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Response[] res = new Response[REPORT_INTERVAL*4];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is pretty"+j);

        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUP[0].send(m);
        testUP[0].flush();

        // all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(REPORT_INTERVAL, stat.getNumResults());
        }


        // now send just a few responses - less than the number of
        // REPORT_INTERVAL - and confirm we don't get messages
        res = new Response[REPORT_INTERVAL-1];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is sweet"+j);

        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUP[2].send(m);
        testUP[2].flush();

        // no UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNull(stat);
        }

        // simply send 2 more responses....
        res = new Response[2];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is young"+j);

        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUP[1].send(m);
        testUP[1].flush();

        // and all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(REPORT_INTERVAL+((REPORT_INTERVAL+1)/4), 
                         stat.getNumResults());
        }

        // shut off the query....
        RouterService.stopQuery(queryGuid);

        // all UPs should get a QueryStatusResponse with 65535
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(65535, stat.getNumResults());
        }

        // more results should not result in more status messages...
        res = new Response[REPORT_INTERVAL*2];
        for (int j = 0; j < res.length; j++)
            res[j] = new Response(10, 10, "anita is pretty"+j);

        m = new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                           GUID.makeGuid(), new byte[0], false, false, true,
                           true, false, false, null);
        
        testUP[0].send(m);
        testUP[0].flush();

        // no UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            final int index = i;
            Thread newThread = new Thread() {
                    public void run() {
                        try {
                            QueryStatusResponse stat = 
                                getFirstQueryStatus(testUP[index]);
                            assertNull(stat);
                        }
                        catch (Exception e) {
                            assertNull(e);
                        }
                    }
                };
            newThread.start();
        }
    }

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

        public void handleQueryResult(RemoteFileDesc rfdParam,
                                      HostData data,
                                      Set locs) {
            this.rfd = rfdParam;
        }
    }


}

