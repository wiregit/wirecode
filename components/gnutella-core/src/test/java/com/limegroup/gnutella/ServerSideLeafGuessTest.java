package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;

import junit.framework.Test;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.Sockets;

/**
 * Checks whether a leaf node handles GUESS queries correctly.  Tests the
 * Server side of GUESS.
 */
public class ServerSideLeafGuessTest extends ClientSideTestCase {
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    public ServerSideLeafGuessTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideLeafGuessTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testBasicProtocol() throws Exception {
        DatagramPacket pack = null;
        UDP_ACCESS = new DatagramSocket();

        for (int i = 0; i < testUP.length; i++) {
            drain(testUP[i]);
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

        InetAddress localHost = InetAddress.getLocalHost();
        // first send a QueryKey request....
        send(PingRequest.createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a QueryKey....
        Message m = null;
        QueryKey qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // we should be able to send a query
        QueryRequest goodQuery = QueryRequest.createQueryKeyQuery("susheel", 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, SERVER_PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // followed by a query hit
        m = receive();
        assertTrue(m instanceof QueryReply);
        QueryReply qRep = (QueryReply) m;
        assertEquals(new GUID(guid), new GUID(qRep.getGUID()));

    }

    public void testGoodURNQuery() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        // first send a QueryKey request....
        send(PingRequest.createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a QueryKey....
        Message m = null;
        QueryKey qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // now send a URN query, make sure that works....
        File berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        Iterator iter = FileDesc.calculateAndCacheURN(berkeley).iterator();
        URN berkeleyURN = (URN) iter.next();
        
        // we should be able to send a URN query
        QueryRequest goodQuery = QueryRequest.createQueryKeyQuery(berkeleyURN, 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, SERVER_PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // followed by a query hit with a URN
        m = receive();
        assertTrue(m instanceof QueryReply);
        QueryReply qRep = (QueryReply) m;
        assertEquals(new GUID(guid), new GUID(qRep.getGUID()));
        iter = qRep.getResults();
        Response first = (Response) iter.next();
        assertEquals(first.getUrns(),
                     FileDesc.calculateAndCacheURN(berkeley));
    }


    public void testQueryWithNoHit() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        // first send a QueryKey request....
        send(PingRequest.createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a QueryKey....
        Message m = null;
        QueryKey qkToUse = null;
        while (true) {
            m = receive();
            if (m instanceof PingReply) {
                PingReply rep = (PingReply) m;
                qkToUse = rep.getQueryKey();
                if (rep.getQueryKey() != null)
                    break;
            }
        }
        assertNotNull(qkToUse);

        // send a query that shouldn't get results....
        QueryRequest goodQuery = QueryRequest.createQueryKeyQuery("anita", 
                                                                  qkToUse);
        byte[] guid = goodQuery.getGUID();
        send(goodQuery, localHost, SERVER_PORT);
        
        // now we should get an ack
        m = receive();
        assertTrue(m instanceof PingReply);
        PingReply pRep = (PingReply) m;
        assertEquals(new GUID(guid), new GUID(pRep.getGUID()));
        
        // but not a query hit
        try { 
            m = receive();
            assertTrue(false);
        }
        catch (InterruptedIOException expected) {};
    }


    public void testBadQueryKey() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        Message m = null;

        QueryKey qkToUse = QueryKey.getQueryKey(localHost, 0);
        assertNotNull(qkToUse);

        {
            // we shouldn't get any response to our query...
            QueryRequest goodQuery = QueryRequest.createQueryKeyQuery("susheel", 
                                                                      qkToUse);
            byte[] guid = goodQuery.getGUID();
            send(goodQuery, localHost, SERVER_PORT);
            
            try {
                // now we should NOT get an ack            
                m = receive();
                assertTrue(false);
            }
            catch (InterruptedIOException expected) {}
        }
    }
    


    //////////////////////////////////////////////////////////////////

    private Message receive() throws Exception {
		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                     BUFFER_SIZE);
        UDP_ACCESS.receive(datagram);
        byte[] data = datagram.getData();
        // construct a message out of it...
        InputStream in = new ByteArrayInputStream(data);
        Message message = Message.read(in);		
        return message;
    }

    private void send(Message msg, InetAddress ip, int port) 
        throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.write(baos);

		byte[] data = baos.toByteArray();
		DatagramPacket dg = new DatagramPacket(data, data.length, ip, port); 
        UDP_ACCESS.send(dg);
	}

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    public static Integer numUPs() {
        return new Integer(3);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

}

