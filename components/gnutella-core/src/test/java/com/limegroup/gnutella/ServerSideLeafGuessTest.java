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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;

import junit.framework.Test;

import org.limewire.security.AddressSecurityToken;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

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
    
    public void setUp() {
        try {   //  Wait between each test so dup GUIDs don't trigger msg drops
         Thread.sleep(1000*10);     
        } catch (Exception ignored) {}
    }
    
    ///////////////////////// Actual Tests ////////////////////////////

    // MUST RUN THIS TEST FIRST
    public void testBasicProtocol() throws Exception {
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(1000 * 20);

        for (int i = 0; i < testUP.length; i++) {
            drain(testUP[i]);
            // OOB client side needs server side leaf guidance
            testUP[i].send(ProviderHacks.getMessagesSupportedVendorMessage());
            testUP[i].flush();
        }

        // first we need to set up GUESS capability
        // ----------------------------------------
        // set up solicited UDP support
        PrivilegedAccessor.setValue( ProviderHacks.getUdpService(), "_acceptedSolicitedIncoming", Boolean.TRUE );
        // set up unsolicited UDP support
        PrivilegedAccessor.setValue( ProviderHacks.getUdpService(), "_acceptedUnsolicitedIncoming", Boolean.TRUE );
        
        // you also have to set up TCP incoming....
        {
            Socket sock = null;
            OutputStream os = null;
            try {
                sock = ProviderHacks.getSocketsManager().connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                       SERVER_PORT), 1200);
                os = sock.getOutputStream();
                os.write("CONNECT BACK\r\n\r\n".getBytes());
                os.flush();
            } catch (IOException ignored) {
            } finally {
                if(sock != null)
                    try { sock.close(); } catch(IOException ignored) {}
                if(os != null)
                    try { os.close(); } catch(IOException ignored) {}
            }
        }        

        InetAddress localHost = InetAddress.getLocalHost();
        // first send a AddressSecurityToken request....
        send(ProviderHacks.getPingRequestFactory().createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a AddressSecurityToken....
        Message m = null;
        AddressSecurityToken qkToUse = null;
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
        QueryRequest goodQuery = ProviderHacks.getQueryRequestFactory().createQueryKeyQuery("susheel", 
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
        // first send a AddressSecurityToken request....
        send(ProviderHacks.getPingRequestFactory().createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a AddressSecurityToken....
        Message m = null;
        AddressSecurityToken qkToUse = null;
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
        File berkeley = CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        Iterator iter = UrnHelper.calculateAndCacheURN(berkeley, ProviderHacks.getUrnCache()).iterator();
        URN berkeleyURN = (URN) iter.next();
        
        // we should be able to send a URN query
        QueryRequest goodQuery = ProviderHacks.getQueryRequestFactory().createQueryKeyQuery(berkeleyURN, 
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
        assertEquals(first.getUrns(), UrnHelper.calculateAndCacheURN(berkeley, ProviderHacks.getUrnCache()));
    }


    public void testQueryWithNoHit() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        // first send a AddressSecurityToken request....
        send(ProviderHacks.getPingRequestFactory().createQueryKeyRequest(), localHost, SERVER_PORT);

        // we should get a AddressSecurityToken....
        Message m = null;
        AddressSecurityToken qkToUse = null;
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
        QueryRequest goodQuery = ProviderHacks.getQueryRequestFactory().createQueryKeyQuery("anita", 
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

        AddressSecurityToken qkToUse = new AddressSecurityToken(localHost, 0);
        assertNotNull(qkToUse);

        {
            // we shouldn't get any response to our query...
            QueryRequest badQuery = ProviderHacks.getQueryRequestFactory().createQueryKeyQuery("susheel", 
                                                                      qkToUse);
            send(badQuery, localHost, SERVER_PORT);
            
            try {
                // now we should NOT get an ack            
                receive();
                assertTrue(false);
            } catch (InterruptedIOException expected) {}
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
        Message message = ProviderHacks.getMessageFactory().read(in);		
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

    public static Integer numUPs() {
        return new Integer(3);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

}

