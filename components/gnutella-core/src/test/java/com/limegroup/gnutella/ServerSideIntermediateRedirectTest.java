package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;

import junit.framework.Test;

import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackRedirect;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.EmptyResponder;

/**
 *  Tests that an Ultrapeer correctly handles connect back redirect messages.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  This test only covers Ultrapeer behavior - leaves don't participate in
 *  server side connect back stuff.
 */
public final class ServerSideIntermediateRedirectTest 
    extends ServerSideTestCase {

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

    public ServerSideIntermediateRedirectTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideIntermediateRedirectTest.class);
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

        //3. routed leaf, with route table for "test"
        qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(HugeTestUtils.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
			LEAF[0].flush();
        }
    }

    // BEGIN TESTS
    // ------------------------------------------------------

    public void testNoRedirectCandidates() throws Exception {
        UDP_ACCESS = new DatagramSocket();
        TCP_ACCESS = new ServerSocket(TCP_ACCESS_PORT);

        // ok, currently there are no redirect ultrapeers, so we've changed
        // the behavior - the Ultrapeer should just drop them on the floor
        TCPConnectBackVendorMessage tcp = 
            new TCPConnectBackVendorMessage(TCP_ACCESS_PORT);
        LEAF[0].send(tcp);
        LEAF[0].flush();

        try {
            TCP_ACCESS.setSoTimeout(TIMEOUT);
            Socket s = TCP_ACCESS.accept();
            s.close();
            assertTrue(false);
        }
        catch (InterruptedIOException expected) {
        }

        GUID cbGuid = new GUID(GUID.makeGuid());
        UDPConnectBackVendorMessage udp =
            new UDPConnectBackVendorMessage(UDP_ACCESS.getLocalPort(), cbGuid);
        LEAF[0].send(udp);
        LEAF[0].flush();

        try {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            UDP_ACCESS.setSoTimeout(TIMEOUT);
            UDP_ACCESS.receive(pack);
            ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
            PingRequest ping = (PingRequest) Message.read(bais);
            assertTrue(false);
        }
        catch (InterruptedIOException expected) {
        }
        catch (ClassCastException fine) {
        }
    }


    public void testSendsRedirect() throws Exception {

        Connection redirUP = new Connection("localhost", PORT,
                                            new UltrapeerHeaders("localhost"),
                                            new EmptyResponder());
        
        redirUP.initialize();
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            redirUP.send((RouteTableMessage)iter.next());
			redirUP.flush();
        }
        assertTrue(redirUP.isOpen());
        drain(redirUP);

        MessagesSupportedVendorMessage msvm = 
            MessagesSupportedVendorMessage.instance();
        redirUP.send(msvm);
        redirUP.flush();

        Thread.sleep(1000);
        ConnectionManager cm = ROUTER_SERVICE.getConnectionManager();
        // now the guy knows i support the redirect message
        assertGreaterThan(0, cm.getTCPRedirectUltrapeers().size());
        assertGreaterThan(0, cm.getUDPRedirectUltrapeers().size());

        // now actually test....
        { // make sure tcp vm's are redirected
            TCPConnectBackVendorMessage tcp = 
                new TCPConnectBackVendorMessage(TCP_ACCESS_PORT);
            LEAF[0].send(tcp);
            LEAF[0].flush();

            TCPConnectBackRedirect tcpR = (TCPConnectBackRedirect)
                getFirstInstanceOfMessageType(redirUP, 
                                              TCPConnectBackRedirect.class,
                                              TIMEOUT);
            assertNotNull(tcpR);
            assertEquals(TCP_ACCESS_PORT, tcpR.getConnectBackPort());
            assertTrue(tcpR.getConnectBackAddress().getHostAddress(),
                       tcpR.getConnectBackAddress().getHostAddress().indexOf("127.0.0.1") >= 0);
        }

        { // make sure udp vm's are redirected
            GUID cbGuid = new GUID(GUID.makeGuid());
            UDPConnectBackVendorMessage udp =
                new UDPConnectBackVendorMessage(UDP_ACCESS.getLocalPort(), 
                                                cbGuid);
            LEAF[0].send(udp);
            LEAF[0].flush();

            UDPConnectBackRedirect udpR = (UDPConnectBackRedirect)
                getFirstInstanceOfMessageType(redirUP, 
                                              UDPConnectBackRedirect.class, 
                                              TIMEOUT);
            assertNotNull(udpR);
            assertEquals(UDP_ACCESS.getLocalPort(), udpR.getConnectBackPort());
            assertTrue(udpR.getConnectBackAddress().getHostAddress().indexOf("127.0.0.1") >= 0);
            assertEquals(cbGuid, udpR.getConnectBackGUID());
        }

        redirUP.close();
    }


    public void testSendsRedirectMultiple() throws Exception {

        Connection redirUP1 = new Connection("localhost", PORT,
                                            new UltrapeerHeaders("localhost"),
                                            new EmptyResponder());
        
        redirUP1.initialize();
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            redirUP1.send((RouteTableMessage)iter.next());
			redirUP1.flush();
        }
        assertTrue(redirUP1.isOpen());
        drain(redirUP1);

        MessagesSupportedVendorMessage msvm = 
            MessagesSupportedVendorMessage.instance();
        redirUP1.send(msvm);
        redirUP1.flush();

        Connection redirUP2 = new Connection("localhost", PORT,
                                            new UltrapeerHeaders("localhost"),
                                            new EmptyResponder());
        
        redirUP2.initialize();
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            redirUP2.send((RouteTableMessage)iter.next());
			redirUP2.flush();
        }
        assertTrue(redirUP2.isOpen());
        drain(redirUP2);

        msvm = MessagesSupportedVendorMessage.instance();
        redirUP2.send(msvm);
        redirUP2.flush();


        Thread.sleep(1000);
        ConnectionManager cm = ROUTER_SERVICE.getConnectionManager();
        // now the guy knows i support the redirect message
        assertGreaterThan(0, cm.getTCPRedirectUltrapeers().size());
        assertGreaterThan(0, cm.getUDPRedirectUltrapeers().size());

        // now actually test....
        { // make sure tcp vm's are redirected
            TCPConnectBackVendorMessage tcp = 
                new TCPConnectBackVendorMessage(TCP_ACCESS_PORT);
            LEAF[0].send(tcp);
            LEAF[0].flush();

            TCPConnectBackRedirect tcpR = (TCPConnectBackRedirect)
                getFirstInstanceOfMessageType(redirUP1, 
                                              TCPConnectBackRedirect.class,
                                              TIMEOUT);
            if (tcpR == null)
                tcpR = (TCPConnectBackRedirect)
                    getFirstInstanceOfMessageType(redirUP2, 
                                                  TCPConnectBackRedirect.class,
                                                  TIMEOUT);
            assertNotNull(tcpR);
            assertEquals(TCP_ACCESS_PORT, tcpR.getConnectBackPort());
            assertTrue(tcpR.getConnectBackAddress().getHostAddress(),
                       tcpR.getConnectBackAddress().getHostAddress().indexOf("127.0.0.1") >= 0);
        }

        { // make sure udp vm's are redirected
            GUID cbGuid = new GUID(GUID.makeGuid());
            UDPConnectBackVendorMessage udp =
                new UDPConnectBackVendorMessage(UDP_ACCESS.getLocalPort(), 
                                                cbGuid);
            LEAF[0].send(udp);
            LEAF[0].flush();

            UDPConnectBackRedirect udpR = (UDPConnectBackRedirect)
                getFirstInstanceOfMessageType(redirUP1, 
                                              UDPConnectBackRedirect.class, 
                                              TIMEOUT);
            if (udpR == null)
                udpR = (UDPConnectBackRedirect)
                    getFirstInstanceOfMessageType(redirUP2, 
                                                  UDPConnectBackRedirect.class,
                                                  TIMEOUT);
            assertNotNull(udpR);
            assertEquals(UDP_ACCESS.getLocalPort(), udpR.getConnectBackPort());
            assertTrue(udpR.getConnectBackAddress().getHostAddress().indexOf("127.0.0.1") >= 0);
            assertEquals(cbGuid, udpR.getConnectBackGUID());
        }

        redirUP1.close();
        redirUP2.close();
    }
    

    // ------------------------------------------------------

}
