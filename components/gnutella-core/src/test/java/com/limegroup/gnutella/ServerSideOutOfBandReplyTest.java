package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import junit.framework.Test;

import org.limewire.security.SecurityToken;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messagehandlers.OOBQueryKey;
import com.limegroup.gnutella.messagehandlers.OOBTokenData;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 *  Tests that an Ultrapeer correctly handles out-of-band queries.  Essentially 
 *  tests the following methods of MessageRouter: handleQueryRequest,
 *  handleUDPMessage, handleLimeAckMessage, Expirer, QueryBundle, sendQueryReply
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER[1]
 *                              |
 *                              |
 *                              |
 *                             LEAF[0]
 *
 *  This test should cover the case for leaves too, since there is no difference
 *  between Leaf and UP when it comes to this behavior.
 */
public final class ServerSideOutOfBandReplyTest extends ServerSideTestCase {

    protected static int TIMEOUT = 2000;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    public ServerSideOutOfBandReplyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideOutOfBandReplyTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public static Integer numUPs() {
        return new Integer(3);
    }

    public static Integer numLeaves() {
        return new Integer(1);
    }
	
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

    public static void setUpQRPTables() throws Exception {
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
			LEAF[0].flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[0].send((RouteTableMessage)iter.next());
			ULTRAPEER[0].flush();
        }
    }

    // BEGIN TESTS
    // ------------------------------------------------------


    // tests basic out of band functionality
    // this tests solicited UDP support - it should participate in ACK exchange
    public void testBasicOutOfBandRequest() throws Exception {
        PrivilegedAccessor.setValue( RouterService.getUdpService(), "_acceptedSolicitedIncoming", new Boolean(true));
        PrivilegedAccessor.setValue( RouterService.getUdpService(), "_acceptedUnsolicitedIncoming", new Boolean(true));
        
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket();
        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = MessageFactory.read(in);

            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());
        assertTrue(reply.canReceiveUnsolicited());
        
        //rince and repeat, this time pretend to be firewalled
        
        query = 
            QueryRequest.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();
        
        UDPService service = RouterService.getUdpService();
        PrivilegedAccessor.setValue(
        		service,"_acceptedUnsolicitedIncoming",new Boolean(false));
        
        assertFalse(RouterService.getUdpService().canReceiveUnsolicited());
        assertTrue(query.desiresOutOfBandReplies());

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[1].send(query);
        ULTRAPEER[1].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = MessageFactory.read(in);

            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());
        assertFalse(reply.canReceiveUnsolicited());
        
        //restore our un-firewalled status and repeat
        query = 
            QueryRequest.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();
        
        PrivilegedAccessor.setValue(
        		service,"_acceptedUnsolicitedIncoming",new Boolean(true));
        ULTRAPEER[2].send(query);
        ULTRAPEER[2].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = MessageFactory.read(in);

            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());
        assertTrue(reply.canReceiveUnsolicited());
        
        SecurityToken token = new OOBQueryKey(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), reply.getNumResults())); 
        
        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()),
                                     reply.getNumResults(), token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should get TWO replies, each with one response
        //1)
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = MessageFactory.read(in);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());
        byte[] receivedTokenBytes = ((QueryReply)message).getSecurityToken(); 
        assertNotNull(receivedTokenBytes);
        SecurityToken<OOBTokenData> receivedToken = new OOBQueryKey(receivedTokenBytes);
        assertTrue(receivedToken.isFor(new OOBTokenData(pack.getAddress(),
                pack.getPort(), message.getGUID(), receivedToken.getBytes()[0] & 0xFF)));

        //2) null out 'message' so we can get the next reply....
        message = null;
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = MessageFactory.read(in);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());

        // make sure that if we send the ACK we don't get another reply - this
        // is current policy but we may want to change it in the future
        baos = new ByteArrayOutputStream();
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = MessageFactory.read(in);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}


    }

    // makes sure that if we ack back with less results than what the server
    // has we only get back what we asked for.
    public void testPiecemealOutOfBandResults() throws Exception {
        DatagramPacket pack = null;

        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = MessageFactory.read(in);
            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());

        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        OOBQueryKey token = new OOBQueryKey(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), 1));
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 1, token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should get the reply!
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = MessageFactory.read(in);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());
        byte[] receivedTokenBytes = ((QueryReply)message).getSecurityToken(); 
        assertNotNull(receivedTokenBytes);
        SecurityToken<OOBTokenData> receivedToken = new OOBQueryKey(receivedTokenBytes);
        assertTrue(receivedToken.isFor(new OOBTokenData(pack.getAddress(),
                pack.getPort(), message.getGUID(), receivedToken.getBytes()[0] & 0xFF)));
        assertEquals(1, receivedToken.getBytes()[0] & 0XFF);

        // make sure that if we send the ACK we don't get another reply - this
        // is current policy but we may want to change it in the future
        baos = new ByteArrayOutputStream();
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = MessageFactory.read(in);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}
    }

    // makes sure that if we ACK back with a 0 for the number of results we
    // don't get a query reply
    public void testSimpleACKOutOfBandResults() throws Exception {
        DatagramPacket pack = null;

        drainAll();

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = MessageFactory.read(in);
            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());

        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OOBQueryKey token = new OOBQueryKey(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), 0));
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 0, token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = MessageFactory.read(in);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}
    }

    // tests that MessageRouter expires GUIDBundles/Replies in a timely fashion
    // this test requires solicited support
    public void testExpirer() throws Exception {
        drainAll();
        DatagramPacket pack = null;

        // THIS TESTS ASSUMES SOLICITED SUPPORT - set up in a previous test

        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("berkeley",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[1].send(query);
        ULTRAPEER[1].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = MessageFactory.read(in);
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(1, reply.getNumResults());

        // WAIT for the expirer to expire the query reply
        Thread.sleep(60 * 1000); // 1 minute - expirer must run twice

        // ok - we should ACK the ReplyNumberVM and NOT get a reply
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OOBQueryKey token = new OOBQueryKey(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), reply.getNumResults()));
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 
                                     reply.getNumResults(), token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!  keep reading until buffer is empty
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = MessageFactory.read(in);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(ack.getGUID(), message.getGUID()))));
            }
        }
        catch (IOException expected) {}

        // good - now lets test that if we send a LOT of out-of-band queries,
        // we get a lot of ReplyNumberVMs but at the 251st we don't get a
        // ReplyNumberVM - this test may be fragile because i'm hardcoding
        // MAX_BUFFERED_REPLIES from MessageRouter
        final int MAX_BUFFERED_REPLIES = 15;

        // ok, we need to set MAX_BUFFERED_REPLIES in MessageRouter
        MessageRouter.MAX_BUFFERED_REPLIES = MAX_BUFFERED_REPLIES;

        // send 15 queries
        Random rand = new Random();
        int numReplyNumberVMs = 0;
        for (int i = 0; i < MAX_BUFFERED_REPLIES; i++) {
            query = 
            QueryRequest.createOutOfBandQuery((i%2==0) ? "berkeley" : "susheel",
                                                  InetAddress.getLocalHost().getAddress(),
                                                  UDP_ACCESS.getLocalPort());
            query.hop();

            if (rand.nextInt(2) == 0) {
                ULTRAPEER[1].send(query);
                ULTRAPEER[1].flush();
            }
            else {
                ULTRAPEER[0].send(query);
                ULTRAPEER[0].flush();
            }

            Thread.sleep(1250);
            // count 15 ReplyNumberVMs
            try {
                while (true) {
                    UDP_ACCESS.setSoTimeout(500);
                    pack = new DatagramPacket(new byte[1000], 1000);
                    UDP_ACCESS.receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    message = MessageFactory.read(in);
                    if (message instanceof ReplyNumberVendorMessage)
                        numReplyNumberVMs++;
                }
            }
            catch (IOException expected) {}
        }

        assertEquals("Didn't get all VMs!!", MAX_BUFFERED_REPLIES,
            numReplyNumberVMs);

        // send 2 new queries that shouldn't be ACKed
        for (int i = 0; i < 2; i++) {
            query = 
            QueryRequest.createOutOfBandQuery((i%2==0) ? "berkeley" : "susheel",
                                                  InetAddress.getLocalHost().getAddress(),
                                                  UDP_ACCESS.getLocalPort());
            query.hop();

            ULTRAPEER[0].send(query);
            ULTRAPEER[0].flush();
        }

        // count NO ReplyNumberVMs
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = MessageFactory.read(in);
                assertNotInstanceof( ReplyNumberVendorMessage.class, message );
            }
        }
        catch (IOException expected) {}
    }

    // make sure that the Ultrapeer discards out-of-band query requests that
    // have an improper address associated with it
    public void testIdentity() throws Exception {
        drainAll();

        byte[] crapIP = {(byte)192,(byte)168,(byte)1,(byte)1};
        QueryRequest query = QueryRequest.createOutOfBandQuery("berkeley", 
                                                               crapIP, 6346);
        LEAF[0].send(query);
        LEAF[0].flush();

        // ultrapeers should NOT get the QR
        assertNull(getFirstQueryRequest(ULTRAPEER[0]));
        assertNull(getFirstQueryRequest(ULTRAPEER[1]));
        
        
        Socket socket = 
            (Socket)PrivilegedAccessor.getValue(LEAF[0], "_socket");
        // try a good query
        query = 
            QueryRequest.createOutOfBandQuery("berkeley", 
                                              socket.getLocalAddress().getAddress(),
                                              6346);
        LEAF[0].send(query);
        LEAF[0].flush();

        Thread.sleep(4000);

        // ultrapeers should get the QR
        assertNotNull(getFirstQueryRequest(ULTRAPEER[0]) );
        assertNotNull(getFirstQueryRequest(ULTRAPEER[1]) );

        // LEAF[0] should get the reply
        assertNotNull(getFirstQueryReply(LEAF[0]));
    }

    // a node should NOT send a reply out of band via UDP if it is not
    // far away (low hop)
    public void testLowHopOutOfBandRequest() throws Exception {
        drainAll();

        byte[] meIP = {(byte)127,(byte)0,(byte)0,(byte)1};
        QueryRequest query = 
            QueryRequest.createOutOfBandQuery("susheel", meIP, 
                                              UDP_ACCESS.getLocalPort());
        ULTRAPEER[1].send(query);
        ULTRAPEER[1].flush();

        // ULTRAPEER[1] should get a reply via TCP
        assertNotNull(getFirstQueryReply(ULTRAPEER[1]));
    }

    

}
