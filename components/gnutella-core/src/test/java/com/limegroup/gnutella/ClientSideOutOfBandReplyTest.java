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
        TIMEOUT = 3000;
    }        
    
    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testBasicProtocol() throws Exception {
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket();

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
                sock = Sockets.connect(InetAddress.getLocalHost().getHostAddress(), 
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
        
        keepAllAlive(testUP);
        // clear up any messages before we begin the test.
        drainAll();

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(rs.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), rs.getAddress(), 
                                       rs.getPort()));
        rs.query(queryGuid.bytes(), "susheel");
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
                                  testUP[0].getInetAddress(), rs.getPort());
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
            Message m = Message.read(in);
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
        GUID queryGuid = new GUID(rs.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), rs.getAddress(), 
                                       rs.getPort()));
        rs.query(queryGuid.bytes(), "susheel");
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
                                  testUP[0].getInetAddress(), rs.getPort());
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
            Message m = Message.read(in);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());

        // now stop the query
        rs.stopQuery(queryGuid);
        keepAllAlive(testUP);
        drainAll();

        // send another ReplyNumber
        vm = new ReplyNumberVendorMessage(queryGuid, 5);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), rs.getPort());
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
            Message m = Message.read(in);
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
        GUID queryGuid = new GUID(rs.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), rs.getAddress(), 
                                       rs.getPort()));
        rs.query(queryGuid.bytes(), "susheel");
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
                                  testUP[0].getInetAddress(), rs.getPort());
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
            Message m = Message.read(in);
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
                               true, false, false, null);

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
                                  testUP[0].getInetAddress(), rs.getPort());
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
            Message m = Message.read(in);
            if (m instanceof LimeACKVendorMessage)
                assertTrue("we got an ack, weren't supposed to!!", false);
        }

    }

    
    //////////////////////////////////////////////////////////////////

    public static Integer numUPs() {
        return new Integer(4);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

}

