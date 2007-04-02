package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.search.QueryHandler;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.Sockets;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideOutOfBandReplyTest extends ClientSideTestCase {

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    public ClientSideOutOfBandReplyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideOutOfBandReplyTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private static void doSettings() {
	ConnectionSettings.DO_NOT_BOOTSTRAP.setValue(true);
    }
    
    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testBasicProtocol() throws Exception {
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(2000);
        
        for (int i = 0; i < testUP.length; i++) {
            assertTrue("should be open", testUP[i].isOpen());
            assertTrue("should be up -> leaf",
                testUP[i].isSupernodeClientConnection());
            drain(testUP[i], 500);
            // OOB client side needs server side leaf guidance
            testUP[i].send(MessagesSupportedVendorMessage.instance());
            testUP[i].flush();
        }

        // first we need to set up GUESS capability
        // ----------------------------------------

        // set up solicited UDP support
        PrivilegedAccessor.setValue( rs.getUdpService(), "_acceptedSolicitedIncoming", Boolean.TRUE );
        // set up unsolicited UDP support
        PrivilegedAccessor.setValue( rs.getUdpService(), "_acceptedUnsolicitedIncoming", Boolean.TRUE );
        
        // ----------------------------------------

        Thread.sleep(250);
        // we should now be guess capable and tcp incoming capable....
        assertTrue(RouterService.isGUESSCapable());
        
        keepAllAlive(testUP);
        // clear up any messages before we begin the test.
        drainAll();

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(RouterService.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                                       RouterService.getAddress(), 
                                       RouterService.getPort()));
        RouterService.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
           new ReplyNumberVendorMessage(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  RouterService.getPort());
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
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
            Message m = MessageFactory.read(in);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());
    }


    public void testRemovedQuerySemantics() throws Exception {
        DatagramPacket pack = null;
        // send a query and make sure that after it is removed (i.e. stopped by
        // the user) we don't request OOB replies for it

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(RouterService.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                        RouterService.getAddress(), 
                        RouterService.getPort()));
        RouterService.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
           new ReplyNumberVendorMessage(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  RouterService.getPort());
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
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
            Message m = MessageFactory.read(in);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());

        // now stop the query
        RouterService.stopQuery(queryGuid);
        keepAllAlive(testUP);
        drainAll();

        // send another ReplyNumber
        vm = new ReplyNumberVendorMessage(queryGuid, 5);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  RouterService.getPort());
        UDP_ACCESS.send(pack);

        // we should NOT get a LimeACK in response
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (InterruptedIOException expected) {
                break;
            }
            catch (IOException bad) {
                bad.printStackTrace();
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = MessageFactory.read(in);
            if (m instanceof LimeACKVendorMessage)
                assertTrue("we got an ack, weren't supposed to!!", false);
        }

    }


    public void testExpiredQuerySemantics() throws Exception {
        DatagramPacket pack = null;
        // send a query and make sure that after it is expired (i.e. enough
        // results are recieved) we don't request OOB replies for it
        
        // clear up messages before we test.
        keepAllAlive(testUP);

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(RouterService.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                                       RouterService.getAddress(), 
                                       RouterService.getPort()));
        RouterService.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
           new ReplyNumberVendorMessage(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  RouterService.getPort());
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
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
            Message m = MessageFactory.read(in);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());

        // now expire the query by routing hundreds of replies back
        int respsPerUP = QueryHandler.ULTRAPEER_RESULTS/testUP.length + 5;
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[respsPerUP];
            for (int j = 0; j < res.length; j++)
                res[j] = new Response(10, 10, "susheel"+i+j);
            Message m = 
                new QueryReply(queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                               GUID.makeGuid(), new byte[0], false, false, true,
                               true, false, false, null, null);

            testUP[i].send(m);
            testUP[i].flush();
        }
        Thread.sleep(2000); // lets process these results...

        // send another ReplyNumber
        vm = new ReplyNumberVendorMessage(queryGuid, 5);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  RouterService.getPort());
        UDP_ACCESS.send(pack);

        // we should NOT get a LimeACK in response
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (InterruptedIOException expected) {
                break;
            }
            catch (IOException bad) {
                bad.printStackTrace();
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = MessageFactory.read(in);
            if (m instanceof LimeACKVendorMessage)
                assertTrue("we got an ack, weren't supposed to!!", false);
        }

    }

    public void testFirewalledReplyLogic() throws Exception {

        // one of the UPs should send a PushProxyAck cuz we don't send the 'FW'
        // header unless we have proxies
        PushProxyAcknowledgement ppAck = 
            new PushProxyAcknowledgement(InetAddress.getLocalHost(), 
                                         testUP[2].getPort(),
                                         new GUID(RouterService.getMessageRouter()._clientGUID));
        testUP[2].send(ppAck); testUP[2].flush();

        { // this should not go through because of firewall/firewall
            drain(testUP[0]);

            QueryRequest query = 
                new QueryRequest(GUID.makeGuid(), (byte)2, 
                                 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                                 QueryRequest.SPECIAL_FIREWALL_MASK,
                                 "berkeley", "", null, 
                                 null, null, false, Message.N_UNKNOWN, false, 
                                 0, false, 0);

            testUP[0].send(query);testUP[0].flush();
            QueryReply reply = getFirstQueryReply(testUP[0]);
            assertNull(reply);
        }

        { // this should go through because of firewall transfer/solicited
            drain(testUP[0]);

            QueryRequest query = 
                new QueryRequest(GUID.makeGuid(), (byte)2, 
                                 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                                 QueryRequest.SPECIAL_FIREWALL_MASK |
                                 QueryRequest.SPECIAL_FWTRANS_MASK,
                                 "susheel", "", null, 
                                 null, null, false, Message.N_UNKNOWN, false, 
                                 0, false, 0);

            testUP[0].send(query);testUP[0].flush();
            QueryReply reply = getFirstQueryReply(testUP[0]);
            assertTrue(reply.getSupportsFWTransfer());
            assertNotNull(reply);
        }

        { // this should go through because the source isn't firewalled
            drain(testUP[1]);

            QueryRequest query = 
                new QueryRequest(GUID.makeGuid(), (byte)2, 
                                 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                                 QueryRequest.SPECIAL_XML_MASK,
                                 "susheel", "", null, 
                                 null, null, false, Message.N_UNKNOWN, false, 
                                 0, false, 0);

            testUP[1].send(query);testUP[1].flush();
            QueryReply reply = getFirstQueryReply(testUP[1]);
            assertFalse(reply.getSupportsFWTransfer());
            assertNotNull(reply);
        }

        // open up incoming to the test node
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

        Thread.sleep(250);
        assertTrue(RouterService.acceptedIncomingConnection());
        { // this should go through because test node is not firewalled
            drain(testUP[2]);

            QueryRequest query = 
                new QueryRequest(GUID.makeGuid(), (byte)2, 
                                 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                                 QueryRequest.SPECIAL_FIREWALL_MASK |
                                 QueryRequest.SPECIAL_FWTRANS_MASK,
                                 "susheel", "", null, 
                                 null, null, false, Message.N_UNKNOWN, false, 
                                 0, false, 0);

            testUP[2].send(query);testUP[2].flush();
            QueryReply reply = getFirstQueryReply(testUP[2]);
            assertTrue(!reply.getSupportsFWTransfer());
            assertNotNull(reply);
        }



    }
    
    
    //////////////////////////////////////////////////////////////////

    public static Integer numUPs() {
        return new Integer(3);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

}

