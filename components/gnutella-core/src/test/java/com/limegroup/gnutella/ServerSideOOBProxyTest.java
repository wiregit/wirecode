package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.List;
import java.util.Map;

/**
 *  Tests that an Ultrapeer correctly proxies for a Leaf.
 *
 *  ULTRAPEER[0]  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER[1]
 *                              |
 *                              |
 *                              |
 *                             LEAF[0]
 *
 */
public final class ServerSideOOBProxyTest extends ServerSideTestCase {
    private final int MAX_RESULTS = SearchResultHandler.MAX_RESULTS;
    private static final long EXPIRE_TIME = 20 * 1000;

    protected static int TIMEOUT = 2000;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    public ServerSideOOBProxyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideOOBProxyTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public static Integer numUPs() {
        return new Integer(2);
    }

    public static Integer numLeaves() {
        return new Integer(2);
    }
	
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

    public static void setSettings() throws Exception {
        // we want to test the expirer so make the expire period small
        PrivilegedAccessor.setValue(ManagedConnection.class,
                                    "TIMED_GUID_LIFETIME",
                                    new Long(EXPIRE_TIME));
        ConnectionSettings.MULTICAST_PORT.setValue(10100);
    }

    public static void setUpQRPTables() throws Exception {
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("stanford");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
			LEAF[0].flush();
        }

        qrt = new QueryRouteTable();
        qrt.add("stanford");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[1].send((RouteTableMessage)iter.next());
			LEAF[1].flush();
        }

        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("stanford");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[0].send((RouteTableMessage)iter.next());
			ULTRAPEER[0].flush();
        }

        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[1].send((RouteTableMessage)iter.next());
			ULTRAPEER[1].flush();
        }
    }

    // BEGIN TESTS
    // ------------------------------------------------------

    // PLEASE RUN THIS TEST FIRST!!!
    public void testProxiesOnlyWhenSupposedTo() throws Exception {

        // before we set up GUESS we should see that the UP does not proxy
        //------------------------------
        {
        drainAll();    
        sendF(LEAF[1], MessagesSupportedVendorMessage.instance());
        Thread.sleep(100); // wait for processing of msvm

        QueryRequest query = QueryRequest.createQuery("stanford");
        sendF(LEAF[1], query);
        
        Thread.sleep(1000);

        // the Ultrapeer should get it.
        QueryRequest queryRec = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                         QueryRequest.class);
        assertNotNull(queryRec);
        assertEquals(new GUID(query.getGUID()), new GUID(queryRec.getGUID()));

        // shut off query
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
        sendF(LEAF[1], resp);
        }
        //------------------------------


        // need to set up GUESS
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket();
        // set up solicited UDP support
        {
            drainAll();
            PingReply pong = 
                PingReply.create(GUID.makeGuid(), (byte) 4,
                                 UDP_ACCESS.getLocalPort(), 
                                 InetAddress.getLocalHost().getAddress(), 
                                 10, 10, true, 900, true);
            ULTRAPEER[0].send(pong);
            ULTRAPEER[0].flush();

            // wait for the ping request from the test UP
            UDP_ACCESS.setSoTimeout(500);
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
            // tell the UP i can support UDP connect back
            MessagesSupportedVendorMessage support = 
                MessagesSupportedVendorMessage.instance();
            ULTRAPEER[0].send(support);
            ULTRAPEER[0].flush();

            byte[] cbGuid = null;
            int cbPort = -1;
            while (cbGuid == null) {
                try {
                    Message m = ULTRAPEER[0].receive(TIMEOUT);
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
                                      ULTRAPEER[0].getInetAddress(), cbPort);
            UDP_ACCESS.send(pack);
        }
        
        Thread.sleep(500);
        assertTrue(ROUTER_SERVICE.isGUESSCapable());
        assertTrue(ROUTER_SERVICE.isOOBCapable());
        
        //no one has sent a MessagesSupportedVM yet so no queries should be
        //proxied
        //------------------------------
        {
        drainAll();    
        QueryRequest query = QueryRequest.createQuery("stanford");
        sendF(LEAF[0], query);
        
        Thread.sleep(1000);

        // the Ultrapeer should get it.
        QueryRequest queryRec = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                         QueryRequest.class);
        assertNotNull(queryRec);
        assertEquals(new GUID(query.getGUID()), new GUID(queryRec.getGUID()));

        // shut off query
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
        sendF(LEAF[0], resp);
        }
        //------------------------------

        //now send a MSM and make sure that the query is proxied
        //------------------------------
        {
        drainAll();    
        sendF(LEAF[0], MessagesSupportedVendorMessage.instance());
        Thread.sleep(100); // wait for processing of msvm

        QueryRequest query = QueryRequest.createQuery("stanford");
        sendF(LEAF[0], query);
        
        Thread.sleep(1000);

        // the Ultrapeer should get it and proxy it
        QueryRequest queryRec = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                         QueryRequest.class);
        assertNotNull(queryRec);
        assertTrue(queryRec.desiresOutOfBandReplies());
        byte[] proxiedGuid = new byte[queryRec.getGUID().length];
        System.arraycopy(queryRec.getGUID(), 0, proxiedGuid, 0, 
                         proxiedGuid.length);
        GUID.addressEncodeGuid(proxiedGuid, ROUTER_SERVICE.getAddress(),
                               ROUTER_SERVICE.getPort());
        assertEquals(new GUID(proxiedGuid), new GUID(queryRec.getGUID()));

        // shut off query
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
        sendF(LEAF[0], resp);
        }
        //------------------------------

        //now send a OOB query and make sure it isn't proxied
        //------------------------------
        {
        drainAll();    
        QueryRequest query = 
        QueryRequest.createOutOfBandQuery("leehsus",
                                          LEAF[0].getInetAddress().getAddress(),
                                          LEAF[0].getPort());
        sendF(LEAF[0], query);
        
        Thread.sleep(1000);

        // the Ultrapeer should get it.
        QueryRequest queryRec = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[1],
                                                         QueryRequest.class);
        assertNotNull(queryRec);
        assertTrue(queryRec.desiresOutOfBandReplies());
        assertEquals(new GUID(query.getGUID()), new GUID(queryRec.getGUID()));
        assertEquals(LEAF[0].getPort(), queryRec.getReplyPort());

        // shut off query
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
        sendF(LEAF[0], resp);
        }
        //------------------------------

        //now send a 'no proxy' query and make sure it isn't proxied
        //------------------------------
        {
        drainAll();    
        QueryRequest query = new QueryRequest(GUID.makeGuid(), (byte) 3,  
                                              "whatever", null, null, null, 
                                              null, false, 
                                              Message.N_UNKNOWN, false, 0,
                                              true, 0);
        sendF(LEAF[0], query);
        
        Thread.sleep(1000);

        // the Ultrapeer should get it.
        QueryRequest queryRec = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[1],
                                                         QueryRequest.class);
        assertNotNull(queryRec);
        assertTrue(queryRec.doNotProxy());
        assertEquals(new GUID(query.getGUID()), new GUID(queryRec.getGUID()));

        // shut off query
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
        sendF(LEAF[0], resp);
        }
        //------------------------------

        //we should never proxy for a Ultrapeer
        //now send a MSM and make sure that the query is proxied
        //------------------------------
        {
        drainAll();    
        sendF(ULTRAPEER[0], MessagesSupportedVendorMessage.instance());
        Thread.sleep(100); // wait for processing of msvm

        QueryRequest query = QueryRequest.createQuery("stanford");
        sendF(ULTRAPEER[0], query);
        
        Thread.sleep(1000);

        // the Leaf should get the non-OOB query
        QueryRequest queryRec = 
            (QueryRequest) getFirstInstanceOfMessageType(LEAF[0],
                                                         QueryRequest.class);
        assertNotNull(queryRec);
        assertEquals(new GUID(query.getGUID()), new GUID(queryRec.getGUID()));

        // no need shut off query
        }
        //------------------------------
    }

    // tests that:
    // 1) routed TCP results are mapped
    // 2) OOB results are acked and mapped
    public void testBasicProxy() throws Exception {
        drainAll();    

        QueryRequest query = QueryRequest.createQuery("stanford");
        sendF(LEAF[0], query);
        
        Thread.sleep(1000);

        // the Ultrapeer should get it and proxy it
        QueryRequest queryRec = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                         QueryRequest.class);
        assertNotNull(queryRec);
        assertTrue(queryRec.desiresOutOfBandReplies());
        byte[] proxiedGuid = new byte[queryRec.getGUID().length];
        System.arraycopy(queryRec.getGUID(), 0, proxiedGuid, 0, 
                         proxiedGuid.length);
        GUID.addressEncodeGuid(proxiedGuid, ROUTER_SERVICE.getAddress(),
                               ROUTER_SERVICE.getPort());
        assertEquals(new GUID(proxiedGuid), new GUID(queryRec.getGUID()));

        // 1) route some TCP results back and make sure they are mapped back to
        // the leaf
        {
            Response[] res = new Response[1];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10, 10, "stanford0");
            Message m = 
                new QueryReply(proxiedGuid, (byte) 3, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            sendF(ULTRAPEER[0], m);
            
            Thread.sleep(1000); // processing wait
            
            // leaf should get a reply with the correct guid
            QueryReply queryRep = 
            (QueryReply) getFirstInstanceOfMessageType(LEAF[0],
                                                       QueryReply.class);
            assertNotNull(queryRep);
            assertEquals(new GUID(query.getGUID()),new GUID(queryRep.getGUID()));
            assertEquals(((Response)queryRep.getResults().next()).getName(),
                         "stanford0");
            assertEquals(2, queryRep.getTTL());
            assertEquals(1, queryRep.getHops());
        }

        // 2) participate in a OOB exchange and make sure results are mapped
        // back to the leaf
        {
            Response[] res = new Response[1];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10, 10, "stanford1");
            Message m = 
                new QueryReply(proxiedGuid, (byte) 3, 6356, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            
            // send a ReplyNumberVM
            ReplyNumberVendorMessage replyNum = 
                new ReplyNumberVendorMessage(new GUID(proxiedGuid), 1);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            replyNum.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     InetAddress.getLocalHost(),
                                                     PORT);
            UDP_ACCESS.send(pack);

            // we expect an ACK
            LimeACKVendorMessage ack = null;
            while (ack == null) {
                pack = new DatagramPacket(new byte[1000], 1000);
                try {
                    UDP_ACCESS.receive(pack);
                }
                catch (IOException bad) {
                    fail("Did not get ack", bad);
                }
                InputStream in = new ByteArrayInputStream(pack.getData());
                Message mTemp = Message.read(in);
                if (mTemp instanceof LimeACKVendorMessage)
                    ack = (LimeACKVendorMessage) mTemp;
            }
            assertEquals(new GUID(proxiedGuid), new GUID(ack.getGUID()));
            assertEquals(1, ack.getNumResults());
            
            // send off the reply
            baos = new ByteArrayOutputStream();
            m.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      InetAddress.getLocalHost(),
                                      PORT);
            UDP_ACCESS.send(pack);

            // now we should get a reply at the leaf
            QueryReply queryRep = 
            (QueryReply) getFirstInstanceOfMessageType(LEAF[0],
                                                       QueryReply.class);
            assertNotNull(queryRep);
            assertEquals(new GUID(query.getGUID()),new GUID(queryRep.getGUID()));
            assertEquals(((Response)queryRep.getResults().next()).getName(),
                         "stanford1");
            assertEquals(2, queryRep.getTTL());
            assertEquals(1, queryRep.getHops());
        }

        // 3) shut off the query, make sure the OOB is bypassed but TCP is still
        // sent
        {
            // shut off query
            QueryStatusResponse resp = 
                new QueryStatusResponse(new GUID(queryRec.getGUID()), 
                                        MAX_RESULTS);
            sendF(LEAF[0], resp);
            Thread.sleep(500);  // let it process

            Response[] res = new Response[1];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10, 10, "stanford2");
            Message m = 
                new QueryReply(proxiedGuid, (byte) 3, 6356, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null);
            
            // send a ReplyNumberVM
            ReplyNumberVendorMessage replyNum = 
                new ReplyNumberVendorMessage(new GUID(proxiedGuid), 1);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            replyNum.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     InetAddress.getLocalHost(),
                                                     PORT);
            UDP_ACCESS.send(pack);

            // we better not get an ACK
            try {
                while (true) {
                    pack = new DatagramPacket(new byte[1000], 1000);
                    UDP_ACCESS.receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    Message mTemp = Message.read(in);
                    if (mTemp instanceof LimeACKVendorMessage)
                        fail("Should not get ACK!!!");
                }
            }
            catch (InterruptedIOException expected) {}

            // send via TCP, we better get it...                
            sendF(ULTRAPEER[0], m);
            
            Thread.sleep(1000); // processing wait
            
            // leaf should get a reply with the correct guid
            QueryReply queryRep = 
            (QueryReply) getFirstInstanceOfMessageType(LEAF[0],
                                                       QueryReply.class);
            assertNotNull(queryRep);
            assertEquals(new GUID(query.getGUID()),new GUID(queryRep.getGUID()));
            assertEquals(((Response)queryRep.getResults().next()).getName(),
                         "stanford2");
            assertEquals(2, queryRep.getTTL());
            assertEquals(1, queryRep.getHops());
        }
    }

    // tests that the expirer works
    public void testExpirer() throws Exception {
        // see if anything is going to be expired
        Class guidMapExpirer = 
            PrivilegedAccessor.getClass(ManagedConnection.class,
                                        "GuidMapExpirer");
        List expireList = (List) PrivilegedAccessor.getValue(guidMapExpirer, 
                                                             "toExpire");
        assertNotNull(expireList);
        Thread.sleep(EXPIRE_TIME*2);  // old guids should be expired...
        synchronized (expireList) {
            assertEquals(1, expireList.size());
            // iterator through all the maps and confirm they are empty
            Iterator iter = expireList.iterator();
            while (iter.hasNext()) {
                Map currMap = (Map) iter.next();
                synchronized (currMap) {
                    assertTrue(currMap.isEmpty());
                }
            }
        }

        // now add a few queries and make sure some are expired but others not
        {
        QueryRequest query = QueryRequest.createQuery("sumeet");
        sendF(LEAF[0], query);
        Thread.sleep(500);
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(query.getGUID()), MAX_RESULTS);
        sendF(LEAF[0], resp);
        Thread.sleep(500);
        }
        {
        QueryRequest query = QueryRequest.createQuery("berlin");
        sendF(LEAF[0], query);
        Thread.sleep(500);
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(query.getGUID()), MAX_RESULTS);
        sendF(LEAF[0], resp);
        Thread.sleep(500);
        }
        Thread.sleep(EXPIRE_TIME*2);
        
        synchronized (expireList) {
            // iterator through all the maps and confirm they are empty
            Iterator iter = expireList.iterator();
            while (iter.hasNext()) {
                Map currMap = (Map) iter.next();
                synchronized (currMap) {
                    assertTrue(currMap.isEmpty());
                }
            }
        }
        
        // close the leaf and make sure the MC purges it's guidmap
        LEAF[0].close();
        ROUTER_SERVICE.query(GUID.makeGuid(), "stanford");
        Thread.sleep(2000);
        assertTrue(expireList.isEmpty());       
    }
 
   
    private final void sendF(Connection c, Message m) throws Exception {
        c.send(m);
        c.flush();
    }

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }


}