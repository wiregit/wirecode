package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.Test;

import org.limewire.io.IOUtils;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackRedirect;
import com.limegroup.gnutella.util.EmptyResponder;

/**
 *  Tests that an Ultrapeer correctly handles connect back redirect messages.
 *
 *  ULTRAPEER[0]  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER[1]
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  This test only covers Ultrapeer behavior - leaves don't participate in
 *  server side connect back stuff.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public final class ServerSideConnectBackRedirectTest extends ServerSideTestCase {

    protected static int TIMEOUT = 2000;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;

    /**
     * Just a TCP connection to use for testing.
     */
    private static ServerSocket TCP_ACCESS;

    /**
     * The port for TCP_ACCESS
     */ 
    private static final int TCP_ACCESS_PORT = 10776;

    public ServerSideConnectBackRedirectTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideConnectBackRedirectTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public static Integer numUPs() {
        return new Integer(2);
    }

    public static Integer numLeaves() {
        return new Integer(1);
    }

    // BEGIN TESTS
    // ------------------------------------------------------

    // RUN THIS TEST FIRST
    public void testConfirmSupport() throws Exception {
        UDP_ACCESS = new DatagramSocket();
        TCP_ACCESS = new ServerSocket(TCP_ACCESS_PORT);

	    LEAF[0] = ProviderHacks.getBlockingConnectionFactory().createConnection("localhost", PORT);
        LEAF[0].initialize(ProviderHacks.getHeadersFactory().createLeafHeaders("localhost"), new EmptyResponder(), 1000);
		assertTrue("LEAF[0] should be connected", LEAF[0].isOpen());

        //  Give the connection a chance to send its initial messages
        Thread.sleep( 1000*2 );        
        
        MessagesSupportedVendorMessage msvm = 
            (MessagesSupportedVendorMessage)getFirstMessageOfType(LEAF[0],
                MessagesSupportedVendorMessage.class, 500);
        assertNotNull(msvm);
        assertGreaterThan(0, msvm.supportsTCPConnectBackRedirect());
        assertGreaterThan(0, msvm.supportsUDPConnectBackRedirect());
    }

    public void testUDPConnectBackRedirect() throws Exception {
        drainAll();
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
            new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                       UDP_ACCESS.getLocalPort());
        
        LEAF[0].send(udp);
        LEAF[0].flush();
        
        // we should NOT get a ping request over our UDP socket....
        UDP_ACCESS.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            UDP_ACCESS.receive(pack);
            fail("got UDP msg");
        } catch (IOException good) {}
 
        cbGuid = new GUID(GUID.makeGuid());
        udp = new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                         UDP_ACCESS.getLocalPort());
        
        ULTRAPEER[0].send(udp);
        ULTRAPEER[0].flush();

        // we should get a ping reply over our UDP socket....
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                Message m = ProviderHacks.getMessageFactory().read(in);
                if (m instanceof PingRequest) {
                    PingRequest reply = (PingRequest) m; 
                    assertEquals(new GUID(reply.getGUID()), cbGuid);
                    break;
                }
            } catch (IOException bad) {
                fail("got IOX", bad);
           }
        }
    }


    public void testUDPConnectBackAlreadyConnected() throws Exception {
        drainAll();

        DatagramSocket tempSock = new DatagramSocket(LEAF[0].getSocket().getLocalPort());
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
        new UDPConnectBackRedirect(cbGuid, LEAF[0].getSocket().getInetAddress(),
                                   LEAF[0].getSocket().getLocalPort());
        
        ULTRAPEER[0].send(udp);
        ULTRAPEER[0].flush();

        // we should NOT get a ping request over our UDP socket....
        tempSock.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            tempSock.receive(pack);
            fail("got UDP msg");
        } catch (IOException good) {}

        tempSock.close();
    }


    public void testTCPConnectBackRedirect() throws Exception {
        drainAll();
        
        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                       TCP_ACCESS.getLocalPort());
        
        LEAF[0].send(tcp);
        LEAF[0].flush();
        
        // we should NOT get a incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            fail("got IOX");
        } catch (IOException good) {}

        tcp = new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                         TCP_ACCESS.getLocalPort());
        
        ULTRAPEER[0].send(tcp);
        ULTRAPEER[0].flush();

        // we should get a incoming connection
        try {
            Socket x = TCP_ACCESS.accept();
            // just like Acceptor reads words.
            String word = IOUtils.readLargestWord(x.getInputStream(), 8);
            assertEquals("CONNECT", word);
        } catch (IOException bad) {
            fail("got IOX", bad);
        }

    }

    public void testTCPConnectBackAlreadyConnected() throws Exception {
        drainAll();

        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(LEAF[0].getSocket().getInetAddress(),
                                       TCP_ACCESS.getLocalPort());
        
        ULTRAPEER[0].send(tcp);
        ULTRAPEER[0].flush();

        // we should NOT get an incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            fail("got TCP connection");
        } catch (IOException good) {}
    }


    public void testConnectBackExpirer() throws Exception {
        drainAll();
        
        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackRedirect udp = 
            new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                       UDP_ACCESS.getLocalPort());
        
        ULTRAPEER[1].send(udp);
        ULTRAPEER[1].flush();
        
        // we should NOT get a ping request over our UDP because we just did this
        UDP_ACCESS.setSoTimeout(1000);
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        try {
            UDP_ACCESS.receive(pack);
            fail("got UDP msg");
        } catch (IOException good) {}

        TCPConnectBackRedirect tcp = 
            new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                       TCP_ACCESS.getLocalPort());
        
        ULTRAPEER[1].send(tcp);
        ULTRAPEER[1].flush();
        
        // we should NOT get a incoming connection since we did this already
        TCP_ACCESS.setSoTimeout(1000);
        try {
            TCP_ACCESS.accept();
            fail("got TCP connection");
        } catch (IOException good) {}

        // simulate the running of the thread - technically i'm not testing
        // the situation precisely, but i'm confident the schedule work so the
        // abstraction isn't terrible
        Thread cbThread = new Thread(new MessageRouterImpl.ConnectBackExpirer());
        cbThread.start();
        cbThread.join();

        // now these two things should work....
        cbGuid = new GUID(GUID.makeGuid());
        udp = new UDPConnectBackRedirect(cbGuid, InetAddress.getLocalHost(),
                                         UDP_ACCESS.getLocalPort());
        
        ULTRAPEER[1].send(udp);
        ULTRAPEER[1].flush();
        
        // we should get a ping request over our UDP
        UDP_ACCESS.setSoTimeout(1000);
        // we should get a ping reply over our UDP socket....
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                Message m = ProviderHacks.getMessageFactory().read(in);
                if (m instanceof PingRequest) {
                    PingRequest reply = (PingRequest) m; 
                    assertEquals(new GUID(reply.getGUID()), cbGuid);
                    break;
                }
            } catch (IOException bad) {
                fail("got IOX", bad);
            }
        }

        tcp = new TCPConnectBackRedirect(InetAddress.getLocalHost(),
                                         TCP_ACCESS.getLocalPort());
        
        ULTRAPEER[1].send(tcp);
        ULTRAPEER[1].flush();
        
        // we should get a incoming connection
        TCP_ACCESS.setSoTimeout(1000);
        try {
            Socket x = TCP_ACCESS.accept();
            byte[] read = new byte["CONNECT BACK\r\n\r\n".length() + 1];
            int n = x.getInputStream().read(read);
            assertEquals(read.length - 1, n);
            assertEquals("CONNECT BACK\r\n\r\n\u0000".getBytes(), read);
        } catch (IOException bad) {
            fail("got IOX", bad);
        }
    }


    // ------------------------------------------------------

}
