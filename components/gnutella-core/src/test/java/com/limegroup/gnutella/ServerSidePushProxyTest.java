package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.limewire.io.NetworkUtils;
import org.limewire.util.Base32;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.EmptyResponder;

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
@SuppressWarnings("unchecked")
public final class ServerSidePushProxyTest extends ServerSideTestCase {

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 2000;

    /**
     * the client guid of the LEAF - please set in the first test.
     */
    private static byte[] clientGUID = null;
    
    /**
     * the client GUID of the leaf as a GUID.
     */
    private static GUID leafGUID = null;

    public ServerSidePushProxyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSidePushProxyTest.class);
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
			ULTRAPEER[0].flush();
        }
    }

    // BEGIN TESTS
    // ------------------------------------------------------

    // THIS TEST MUST BE FIRST - MAKES SURE THE UP SUPPORTS THE PUSHPROXY VM
    // EXCHANGE AND SETS UP OTHER TESTS
    public void testEstablishPushProxy() throws Exception {
        drainAll();
        Message m = null;
        clientGUID = GUID.makeGuid();
        leafGUID = new GUID(clientGUID);

        LEAF[0] = new Connection("localhost", PORT);
        // routed leaf, with route table for "test"
        LEAF[0].initialize(new LeafHeaders("localhost"), new EmptyResponder(), 1000);
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
			LEAF[0].flush();
        }

        // make sure UP is advertised proxy support
        do {
            m = LEAF[0].receive(TIMEOUT);
        } while (!(m instanceof MessagesSupportedVendorMessage)) ;
        assertTrue(((MessagesSupportedVendorMessage)m).supportsPushProxy() > 0);

        // send proxy request
        PushProxyRequest req = new PushProxyRequest(new GUID(clientGUID));
        LEAF[0].send(req);
        LEAF[0].flush();

        // wait for ack
        do {
            m = LEAF[0].receive(TIMEOUT);
        } while (!(m instanceof PushProxyAcknowledgement)) ;
        assertTrue(Arrays.equals(m.getGUID(), clientGUID));
        assertEquals(PORT, ((PushProxyAcknowledgement)m).getListeningPort());

        // ultrapeer supports push proxy setup A-OK
    }
    
    public void testGETWithServerId() throws Exception {
        tRequest("GET", // request method.
                 "/gnutella/push-proxy", // the request
                 "ServerID", // initial param
                 Base32.encode(clientGUID), // initial value
                 "127.0.0.1", // ip
                 6346, // port
                 null, // params
                 202); // opcode expected in return
    }
    
    public void testHEADWithServerId() throws Exception {
        tRequest("HEAD", "/gnutella/push-proxy", "Serverid", 
                Base32.encode(clientGUID), "10.238.1.87", 6350, null, 202);
    }
    
    public void testInvalidGUIDWithServerId() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "serverid",
                Base32.encode(GUID.makeGuid()), "127.0.0.1", 6346, null, 410);
    }
    
    public void testInvalidGUIDWithGuid()  throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "guid", 
                new GUID().toHexString(), "127.0.0.1", 6346, null, 410);
    }
    
    public void testInvalidIP() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "serverid",
                Base32.encode(clientGUID), "www.crapalapadapa.com",
                6346, null, 400);
    }
    
    public void testServerIdWithBase16Fails() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "serverid",
                 leafGUID.toHexString(), "127.0.0.1", 6346, null, 400);
    }
    
    public void testGuidIsBase16() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, null, 202);
    }
    
    public void testGuidWithBase32Fails() throws Exception {
        tRequest("GET", "/gnutella/push-proxy", "guid",
                Base32.encode(clientGUID), "127.0.0.1", 6346, null, 400);
    }
    
    public void testGuidWithGnet() throws Exception {
        tRequest("GET", "/gnet/push-proxy", "guid",
                Base32.encode(clientGUID), "127.0.0.1", 6346, null, 400);
    }
        
    public void testFileChangesIndex() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer(34));
        tRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testFileWithGnet() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer(34));
        tRequest("GET", "/gnet/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testCannotHaveServeridAndGuid() throws Exception {
        Map m = new HashMap();
        m.put("serverid", Base32.encode(clientGUID));
        tRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 400);
    }
    
    public void testMultipleFileFails() throws Exception {
        Map m = new HashMap();
        m.put("FILE", new Integer(1));
        m.put("file", new Integer(2));
        tRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 400);
    }
    
    public void testFirewallTransferPushProxyWorks() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer((int)PushRequest.FW_TRANS_INDEX));
        tRequest("GET", "/gnutella/push-proxy", "ServerID",
                 Base32.encode(clientGUID), "127.0.0.1", 6346, m, 202);
    }
    
    private void tRequest(String reqMethod, String reqKey, String initKey,
                          String guid, String ip, int port, Map params,
                          int opcode)
     throws Exception {
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            
        String result = null;
        Message m = null;

        out.write(reqMethod + " " + reqKey + "?");
        out.write(initKey + "=" + guid);
        if( params != null ) {
            for(Iterator i = params.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                out.write("&" + entry.getKey() + "=" + entry.getValue());
            }
        }
        out.write(" HTTP/1.1.\r\n");
        out.write("X-Node: ");
        out.write(ip + ":" + port + "\r\n");
        out.write("\r\n");
        out.flush();
        
        // check opcode - less important, but might as well
        result = in.readLine();
        assertGreaterThan(result, -1, result.indexOf("" + opcode));
        // clear out other responses
        while (in.readLine() != null) ;
        
        if( opcode != 202 ) {
            // leaf NOT expecting PushRequest.
            try {
                do {
                    m = LEAF[0].receive(TIMEOUT);
                    assertTrue(!(m instanceof PushRequest));
                } while (true) ;
            }
            catch (InterruptedIOException expected) {}
        } else {
            // leaf should get PushRequest
            do {
                m = LEAF[0].receive(TIMEOUT);
            } while (!(m instanceof PushRequest)) ;
            PushRequest pr = (PushRequest) m;
            int idx = 0;
            if(params != null && params.get("file") != null )
                idx = ((Integer)params.get("file")).intValue();
            assertEquals(idx, pr.getIndex());
            assertEquals(new GUID(clientGUID), new GUID(pr.getClientGUID()));
            assertEquals(port, pr.getPort());
            assertEquals(ip, NetworkUtils.ip2string(pr.getIP()));
        }
    }
}
