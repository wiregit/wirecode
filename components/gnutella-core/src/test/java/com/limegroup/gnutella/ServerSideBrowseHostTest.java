package com.limegroup.gnutella;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Iterator;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 *  Tests that an Ultrapeer correctly handles all aspects of PushProxy.  For
 *  example:
 *  1) handles the VendorMessage exchange as expected
 *  2) handles HTTP requests as expected, forwarding on a PushRequest
 *
 *  This class tests a lot of different pieces of code.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  The leaf must be connected in the first test.
 */
public final class ServerSideBrowseHostTest extends ServerSideTestCase {

    protected static int TIMEOUT = 2000;

    public ServerSideBrowseHostTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideBrowseHostTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static Integer numLeaves() {
        return new Integer(1);
    }
	
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

    public static void setUpQRPTables() throws Exception {
        // for Ultrapeer 1
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[0].send((RouteTableMessage)iter.next());
            assertTrue(ULTRAPEER[0].isOpen());
			ULTRAPEER[0].flush();
        }
    }

    // BEGIN TESTS
    // ------------------------------------------------------

    public void testResultsIndicateBrowseHostSupport() throws Exception {
        drainAll();

        // make sure leaf is sharing
        assertEquals(2, RouterService.getFileManager().getNumFiles());

        // send a query that should be answered
        QueryRequest query = new QueryRequest(GUID.makeGuid(), (byte) 1,
                                              "berkeley", null, null, null,
                                              null, false, Network.UNKNOWN, false, 0);
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // await a response
        Message m = null;
        do {
            m = ULTRAPEER[0].receive(TIMEOUT);
        } while (!(m instanceof QueryReply)) ;

        // confirm it supports browse host
        QueryReply reply = (QueryReply) m;
        assertTrue(reply.getSupportsBrowseHost());
    }

    public void testHTTPRequest() throws Exception {
        String result = null;

        Socket s = new Socket("localhost", PORT);
        ByteReader in = new ByteReader(s.getInputStream());
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        // first test a GET
        out.write("GET /  HTTP/1.1\r\n");
        out.write("Accept: application/x-gnutella-packets\r\n");
        out.write("\r\n");
        out.flush();

        // check opcode
        result = in.readLine();
        assertGreaterThan(result, -1, result.indexOf("200"));

        // get to the replies....
        String currLine = null;
        do {
            currLine = in.readLine();
        } while ((currLine != null) && !currLine.equals(""));

        QueryReply qr = (QueryReply) MessageFactory.read(s.getInputStream());
        assertEquals(2, qr.getResultCount());

        assertNull(in.readLine());
        s.close();
        in.close();
    }


    public void testBadHTTPRequest1() throws Exception {
        String result = null;

        Socket s = new Socket("localhost", PORT);
        ByteReader in = new ByteReader(s.getInputStream());
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        // first test a GET
        out.write("GET /  HTTP/1.1\r\n");
        out.write("\r\n");
        out.flush();

        // check opcode
        result = in.readLine();
        assertGreaterThan(result, -1, result.indexOf("406"));
        s.close();
        in.close();
    }

    
}

