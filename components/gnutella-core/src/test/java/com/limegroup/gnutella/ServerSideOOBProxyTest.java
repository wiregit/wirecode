package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.security.SecurityToken;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.OOBProxyControlVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.OOBProxyControlVendorMessage.Control;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

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
@SuppressWarnings( { "unchecked", "cast" } )
public final class ServerSideOOBProxyTest extends ServerSideTestCase {
    private final int MAX_RESULTS = SearchResultHandler.MAX_RESULTS;
    private static final long EXPIRE_TIME = 20 * 1000;

    protected static int TIMEOUT = 2000;

   // private static final Log LOG = LogFactory.getLog(ServerSideOOBProxyTest.class);
    
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
        PrivilegedAccessor.setValue(GuidMapFactory.class, "EXPIRE_POLL_TIME", new Long(EXPIRE_TIME));
        Class clazz = PrivilegedAccessor.getClass(GuidMapFactory.class, "GuidMapImpl");
        PrivilegedAccessor.setValue(clazz, "TIMED_GUID_LIFETIME", new Long(EXPIRE_TIME));
        ConnectionSettings.MULTICAST_PORT.setValue(10100);
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(500);
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

        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
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

        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_acceptedSolicitedIncoming",Boolean.TRUE);
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_acceptedUnsolicitedIncoming",Boolean.TRUE);
        Thread.sleep(500);
        assertTrue(ProviderHacks.getNetworkManager().isGUESSCapable());
        assertTrue(ProviderHacks.getNetworkManager().isOOBCapable());
        
        //no one has sent a MessagesSupportedVM yet so no queries should be
        //proxied
        //------------------------------
        {
        drainAll();    
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
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

        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
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
        GUID.addressEncodeGuid(proxiedGuid, ProviderHacks.getNetworkManager().getAddress(),
                ProviderHacks.getNetworkManager().getPort());
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
        ProviderHacks.getQueryRequestFactory().createOutOfBandQuery("leehsus",
                                          LEAF[0].getInetAddress().getAddress(),
                                          LEAF[0].getPort());
        assertTrue(query.desiresOutOfBandRepliesV3());
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
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQueryRequest(GUID.makeGuid(), (byte) 3,
                "whatever", null, null, null, false, Network.UNKNOWN, false, 0, true,
                0);
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

        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
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

    /** tests that v2 queries are upgraded to v3. */
    public void testProtocolUpgrade() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        drainAll();
        QueryRequest nonOOB = ProviderHacks.getQueryRequestFactory().createQuery("badger");
        assertFalse(nonOOB.desiresOutOfBandReplies());
        nonOOB.getPayload()[0] |= 0x0004; // pretend it wants oob v2
        sendF(LEAF[0], nonOOB);
        
        QueryRequest OOBv2 = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                         QueryRequest.class);
        // upgraded to v3 and proxied
        assertTrue(OOBv2.desiresOutOfBandRepliesV2());
        assertTrue(OOBv2.desiresOutOfBandRepliesV3());
        
        byte[] proxiedGuid = OOBv2.getGUID().clone();
        GUID.addressEncodeGuid(proxiedGuid, ProviderHacks.getNetworkManager().getAddress(),
                ProviderHacks.getNetworkManager().getPort());
        // guid should be address encoded with proxying ultrapeer
        assertEquals(proxiedGuid, OOBv2.getGUID());
    }
    
    /** tests that v2 queries are upgraded to v3 and v2 is disabled */
    public void testProtocolUpgradeDisableV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        drainAll();
        QueryRequest nonOOB = ProviderHacks.getQueryRequestFactory().createQuery("badger");
        assertFalse(nonOOB.desiresOutOfBandReplies());
        nonOOB.getPayload()[0] |= 0x0004; // pretend it wants oob v2
        sendF(LEAF[0], nonOOB);
        
        QueryRequest OOBv2 = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                         QueryRequest.class);
        // upgraded to v3 and proxied, v2 disabled
        assertFalse(OOBv2.desiresOutOfBandRepliesV2());
        assertTrue(OOBv2.desiresOutOfBandRepliesV3());
        
        byte[] proxiedGuid = OOBv2.getGUID().clone();
        GUID.addressEncodeGuid(proxiedGuid, ProviderHacks.getNetworkManager().getAddress(),
                ProviderHacks.getNetworkManager().getPort());
        // guid should be address encoded with proxying ultrapeer
        assertEquals(proxiedGuid, OOBv2.getGUID());
    }
    
    public void testOOBProxyControlMessage() throws Exception {
        
        // default case proxying works
        {
            drainAll();    
            QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
            sendF(LEAF[0], query);
            
            Thread.sleep(1000);

            // the Ultrapeer should get it.
            QueryRequest queryRec = 
                (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                             QueryRequest.class);
            assertNotNull(queryRec);
            byte[] proxiedGuid = query.getGUID().clone();
            GUID.addressEncodeGuid(proxiedGuid, ProviderHacks.getNetworkManager().getAddress(),
                    ProviderHacks.getNetworkManager().getPort());
            // guid should be address encoded with proxying ultrapeer
            assertEquals(proxiedGuid, queryRec.getGUID());

            // shut off query
            QueryStatusResponse resp = 
                new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
            sendF(LEAF[0], resp);
        }
        
        // turn proxying off for all versions by sending control message
        {
            drainAll();
            
            OOBProxyControlVendorMessage control = OOBProxyControlVendorMessage.createDoNotProxyMessage();
            sendF(LEAF[0], control);
            
            // we have to sleep here, otherwise query is handled before control message
            // which is odd since we're sending on the Connection class which does not
            // queue messages by priority
            Thread.sleep(1000);
            
            QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
            sendF(LEAF[0], query);
            
            Thread.sleep(1000);

            // the Ultrapeer should get it.
            QueryRequest queryRec = 
                (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                             QueryRequest.class);
            assertNotNull(queryRec);
            // GUID should be the same and not address encoded by the proxying ultrapeer
            assertEquals(query.getGUID(), queryRec.getGUID());

            // shut off query
            QueryStatusResponse resp = 
                new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
            sendF(LEAF[0], resp);
        }

        // turn proxying off for version 2 => proxying should work, since current version is 3
        {
            drainAll();
            
            OOBProxyControlVendorMessage control = new OOBProxyControlVendorMessage(Control.DISABLE_VERSION_2);
            sendF(LEAF[0], control);
            
            Thread.sleep(1000);
            
            QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
            sendF(LEAF[0], query);
            
            Thread.sleep(1000);

            // the Ultrapeer should get it.
            QueryRequest queryRec = 
                (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                             QueryRequest.class);
            assertNotNull(queryRec);
            byte[] proxiedGuid = query.getGUID().clone();
            GUID.addressEncodeGuid(proxiedGuid, ProviderHacks.getNetworkManager().getAddress(),
                    ProviderHacks.getNetworkManager().getPort());
            // guid should be address encoded with proxying ultrapeer
            assertEquals(proxiedGuid, queryRec.getGUID());

            // shut off query
            QueryStatusResponse resp = 
                new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
            sendF(LEAF[0], resp);
        }
        
        // turn proxying off for version 3 => proxying should not work since we are at version 3
        {
            drainAll();
            
            OOBProxyControlVendorMessage control = new OOBProxyControlVendorMessage(Control.DISABLE_VERSION_3);
            sendF(LEAF[0], control);
            
            Thread.sleep(1000);
            
            QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
            sendF(LEAF[0], query);
            
            Thread.sleep(1000);

            // the Ultrapeer should get it.
            QueryRequest queryRec = 
                (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                             QueryRequest.class);
            assertNotNull(queryRec);
            // GUID should be the same and not address encoded by the proxying ultrapeer
            assertEquals(query.getGUID(), queryRec.getGUID());

            // shut off query
            QueryStatusResponse resp = 
                new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
            sendF(LEAF[0], resp); 
        }
        
        // turn proxying on again
        {
            drainAll();
            
            OOBProxyControlVendorMessage control = OOBProxyControlVendorMessage.createDoProxyMessage();
            sendF(LEAF[0], control);
            
            Thread.sleep(1000);
            
            QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
            sendF(LEAF[0], query);
            
            Thread.sleep(1000);

            // the Ultrapeer should get it.
            QueryRequest queryRec = 
                (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[0],
                                                             QueryRequest.class);
            assertNotNull(queryRec);
            byte[] proxiedGuid = query.getGUID().clone();
            GUID.addressEncodeGuid(proxiedGuid, ProviderHacks.getNetworkManager().getAddress(),
                    ProviderHacks.getNetworkManager().getPort());
            // guid should be address encoded with proxying ultrapeer
            assertEquals(proxiedGuid, queryRec.getGUID());

            // shut off query
            QueryStatusResponse resp = 
                new QueryStatusResponse(new GUID(queryRec.getGUID()), MAX_RESULTS);
            sendF(LEAF[0], resp);
        }
    }
 
    
    // tests that:
    // 1) routed TCP results are mapped
    // 2) OOB results are acked and mapped
    public void testBasicProxy() throws Exception {
        drainAll();    

        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("stanford");
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
        GUID.addressEncodeGuid(proxiedGuid, ProviderHacks.getNetworkManager().getAddress(),
                ProviderHacks.getNetworkManager().getPort());
        assertEquals(new GUID(proxiedGuid), new GUID(queryRec.getGUID()));
        // 1) route some TCP results back and make sure they are mapped back to
        // the leaf
        {
            Response[] res = new Response[1];
            for (int j = 0; j < res.length; j++)
                res[j] = ProviderHacks.getResponseFactory().createResponse(10, 10, "stanford0");
            Message m = 
                ProviderHacks.getQueryReplyFactory().createQueryReply(proxiedGuid, (byte) 3, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
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
                res[j] = ProviderHacks.getResponseFactory().createResponse(10, 10, "stanford1");
            
            
            SecurityToken token = exchangeRNVMACK(proxiedGuid);
            assertNotNull(token);
            
            Message m = 
                ProviderHacks.getQueryReplyFactory().createQueryReply(proxiedGuid, (byte) 3, 6356,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null, token);
            
            // send off the reply
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
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
            assertEquals(token.getBytes(), queryRep.getSecurityToken());
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
                res[j] = ProviderHacks.getResponseFactory().createResponse(10, 10, "stanford2");
            Message m = 
                ProviderHacks.getQueryReplyFactory().createQueryReply(proxiedGuid, (byte) 3, 6356,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            
            // send a ReplyNumberVM
            ReplyNumberVendorMessage replyNum = 
                new ReplyNumberVendorMessageFactory().create(new GUID(proxiedGuid), 1);
            
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
                    Message mTemp = MessageFactory.read(in);
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
    
    /**
     * Tests that results arriving through OOB for non-proxied queries
     * are dropped
     */
    public void testDropUnsolicited() throws Exception {
    	drainAll();
    	QueryRequest query = ProviderHacks.getQueryRequestFactory().createQueryRequest(GUID.makeGuid(), (byte) 3,
                "not proxied", null, null, null, false, Network.UNKNOWN, false, 0, true,
                0);
    	
    	sendF(LEAF[0], query);
        
        Thread.sleep(1000);

        // the Ultrapeer should get it and not modify the guid
        QueryRequest queryRec = 
            (QueryRequest) getFirstInstanceOfMessageType(ULTRAPEER[1],
                                                         QueryRequest.class);
        assertNotNull(queryRec);
        assertTrue(queryRec.doNotProxy());
        assertEquals(new GUID(query.getGUID()), new GUID(queryRec.getGUID()));

        // go through the RNVM and ACK
        // (make sure this test breaks should we decide to intercept
        // unwelcome responses earlier in the protocol)
        SecurityToken token = exchangeRNVMACK(query.getGUID());
        
        // create a bunch of responses for that guid 
        Response[] res = new Response[1];
        for (int j = 0; j < res.length; j++)
            res[j] = ProviderHacks.getResponseFactory().createResponse(10, 10, "not proxied");
        Message m = 
            ProviderHacks.getQueryReplyFactory().createQueryReply(query.getGUID(), (byte) 3, 6356,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null, token);
        
        // and send them OOB 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.write(baos);
        DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                baos.toByteArray().length,
                InetAddress.getLocalHost(),
                PORT);
        UDP_ACCESS.send(pack);
        
        Thread.sleep(1000);
        
        try {
        	LEAF[0].receive(1000);
        	fail("nothing should have arrived");
        } catch (IOException expected){}
    }

    private SecurityToken exchangeRNVMACK(byte[] guid) throws Exception {
    	// send a ReplyNumberVM
    	ReplyNumberVendorMessage replyNum = 
    	    new ReplyNumberVendorMessageFactory().create(new GUID(guid), 1);

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
    		Message mTemp = MessageFactory.read(in);
    		if (mTemp instanceof LimeACKVendorMessage)
    			ack = (LimeACKVendorMessage) mTemp;
    	}
    	assertEquals(new GUID(guid), new GUID(ack.getGUID()));
    	assertEquals(1, ack.getNumResults());
        return ack.getSecurityToken();
    }

    // tests that the expirer works
    public void testExpirer() throws Exception {
        // see if anything is going to be expired
        List expireList = (List) PrivilegedAccessor.getValue(GuidMapFactory.class, 
                                                             "toExpire");
        assertNotNull(expireList);
        Thread.sleep(EXPIRE_TIME*2);  // old guids should be expired...
        synchronized (GuidMapFactory.class) {
            assertEquals(1, expireList.size());
            // iterator through all the maps and confirm they are empty
            for(Iterator i = expireList.iterator(); i.hasNext(); ) {
                Object guidMapImpl = i.next();
                synchronized(guidMapImpl) {
                    Map currMap = (Map)PrivilegedAccessor.invokeMethod(guidMapImpl, "getMap", (Object[])null);
                    assertTrue(currMap.isEmpty());
                }
            }
        }

        // now add a few queries and make sure some are expired but others not
        {
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("sumeet");
        sendF(LEAF[0], query);
        Thread.sleep(500);
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(query.getGUID()), MAX_RESULTS);
        sendF(LEAF[0], resp);
        Thread.sleep(500);
        }
        {
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("berlin");
        sendF(LEAF[0], query);
        Thread.sleep(500);
        QueryStatusResponse resp = 
            new QueryStatusResponse(new GUID(query.getGUID()), MAX_RESULTS);
        sendF(LEAF[0], resp);
        Thread.sleep(500);
        }
        Thread.sleep(EXPIRE_TIME*2);
        
        synchronized (GuidMapFactory.class) {
            // iterator through all the maps and confirm they are empty
            for(Iterator i = expireList.iterator(); i.hasNext(); ) {
                Object guidMapImpl = i.next();
                synchronized(guidMapImpl) {
                    Map currMap = (Map)PrivilegedAccessor.invokeMethod(guidMapImpl, "getMap", (Object[])null);
                    assertTrue(currMap.isEmpty());
                }
            }
        }
        
        // close the leaf and make sure the MC purges it's guidmap
        LEAF[0].close();
        Thread.sleep(2000);
        assertTrue(expireList.toString(), expireList.isEmpty());       
    }
    
    
   
    private final void sendF(Connection c, Message m) throws Exception {
        c.send(m);
        c.flush();
    }

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }


}