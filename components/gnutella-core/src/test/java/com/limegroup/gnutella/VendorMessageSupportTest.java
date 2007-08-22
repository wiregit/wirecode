package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Iterator;

import junit.framework.Test;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.HopsFlowVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;

@SuppressWarnings( { "unchecked", "cast" } )
public class VendorMessageSupportTest extends LimeTestCase {
    
    private String _remoteHost = "localhost";
    private int _remotePort = Backend.BACKEND_PORT;
    private static final int TIMEOUT = 2*500;

    public VendorMessageSupportTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(VendorMessageSupportTest.class);
    }

    
    private ServerSocket _tcpSock = null;
    private DatagramSocket _udpSock = null;
    private Connection _leaf1 = null;
    private Connection _leaf2 = null;

    private static boolean _testHopsFlow = true;
    private static boolean _testTCPCB = true;
    private static boolean _testUDPCB = true;

    public void setUp() throws Exception {
        launchBackend();              
              
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);                    

        // Set up a TCP Listening socket....
        _tcpSock = new ServerSocket(0);
        _tcpSock.setReuseAddress(true);

        // Set up a UDP Listening socket....
        _udpSock = new DatagramSocket();
        _udpSock.setReuseAddress(true);

        // Set up QRT
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("susheel");
        qrt.add("daswani");
        qrt.add("foosball");

        // Set up a connection to the host....
        _leaf1=ProviderHacks.getConnectionFactory().createConnection(_remoteHost, _remotePort);
        _leaf1.initialize(ProviderHacks.getHeadersFactory().createLeafHeaders(""), new EmptyResponder(), 1000);
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); )
            _leaf1.send((RouteTableMessage)iter.next());
        _leaf1.flush();
        // don't do postInit() - you don't want him thinking
        // you support any vendor message....
        
        // Set up another connection to the host....
        _leaf2=ProviderHacks.getConnectionFactory().createConnection(_remoteHost, _remotePort);
        _leaf2.initialize(ProviderHacks.getHeadersFactory().createLeafHeaders(""), new EmptyResponder(), 1000);
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); )
            _leaf2.send((RouteTableMessage)iter.next());
        _leaf2.flush();
        // don't do postInit() - you don't want him thinking
        // you support any vendor message....
    }
    
    public void tearDown() throws Exception {
        if ( _leaf1 != null )
            _leaf1.close();
        if ( _leaf2 != null )
            _leaf2.close();
        if ( _tcpSock != null )
            _tcpSock.close();
        if ( _udpSock != null )
            _udpSock.close();
    }

    public void testConfirmSupportedMessages() throws Exception {
        testConnection(_leaf1);
    }

    private void testConnection(Connection c) throws Exception {
        boolean receivedDesiredMessage = false;
        while (true) {
            try {
                Message m = (Message) c.receive(TIMEOUT);
                if (m instanceof VendorMessage) {
                    if (m instanceof MessagesSupportedVendorMessage) {
                        c.handleVendorMessage((VendorMessage) m);
                        receivedDesiredMessage = true;
                    }
                    else if (m instanceof CapabilitiesVM) {
                        // this is expected....
                    }
                    else
                        fail("Unexpected VendorMessage of class" + m.getClass());
                }
            }
            catch (InterruptedIOException iioe) {
                break; // received all messages it seems....
            }
            catch (Exception ignoreOthers) {}
        }
        if (!receivedDesiredMessage)
            fail("No MessagesSupportedMessage recieved");
        if (c.supportsVendorMessage("BEAR".getBytes(), 4) < 1) {
            _testHopsFlow = false;
        }
        if (c.supportsVendorMessage("GTKG".getBytes(), 7) < 1) {
            _testUDPCB = false;
        }
        if (c.supportsVendorMessage("BEAR".getBytes(), 7) < 1) {
            _testTCPCB = false;
        }
    }

    public void testHopsFlow() throws Exception {
        if ( !_testHopsFlow )
            fail("hops flow not supported - ignoring test.");
        
        drain(_leaf1);
        drain(_leaf2);

        QueryRequest qr = ProviderHacks.getQueryRequestFactory().createQuery("susheel", (byte)3);
        
        // first make sure query gets through.....
        _leaf2.send(qr);
        _leaf2.flush();

        boolean gotQR = false;
        
        while (true) {
            try {
                Message m = _leaf1.receive(TIMEOUT);
                if (m instanceof QueryRequest) 
                    if (((QueryRequest) m).getQuery().equals("susheel"))
                        gotQR = true;
            } 
            catch (InterruptedIOException e) {
                break; // cool, what we want....
            } 
            catch (BadPacketException e) {}
            catch (IOException ioe) {}
        }
        if (!gotQR) 
            fail("Did not get expected QR 1!!");
        

        // now send the hops flow and it shouldn't get through!!
        HopsFlowVendorMessage hops = new HopsFlowVendorMessage((byte)1);
        _leaf1.send(hops);
        _leaf1.flush();

        // wait for the hops flow message to take effect...
        try {
            Thread.sleep(500); 
        }
        catch (Exception whatever) {}

        qr = ProviderHacks.getQueryRequestFactory().createQuery("daswani", (byte)3);
        _leaf2.send(qr);
        _leaf2.flush();
        
        while (true) {
            try {
                Message m = _leaf1.receive(TIMEOUT);
                if (m instanceof QueryRequest) 
                    if (((QueryRequest) m).getQuery().equals("daswani"))
                        fail("Hops Flow message Ineffectual!!!");
            } 
            catch (InterruptedIOException e) {
                break; // cool, what we want....
            } 
            catch (BadPacketException e) {}
            catch (IOException ioe) {}
        }
        
        // reset Hops Flow and make sure a query gets through....
        hops = new HopsFlowVendorMessage((byte)4);
        _leaf1.send(hops);
        _leaf1.flush();

        try {
            // wait for hops flow to be turned off....
            Thread.sleep(2*1000);
        }
        catch (InterruptedException ignored) {}

        qr = ProviderHacks.getQueryRequestFactory().createQuery("foosball", (byte)3);
        _leaf2.send(qr);
        _leaf2.flush();

        gotQR = false;
        while (true) {
            try {
                Message m = _leaf1.receive(TIMEOUT);
                if (m instanceof QueryRequest) 
                    if (((QueryRequest) m).getQuery().equals("foosball"))
                        gotQR = true;
            } 
            catch (InterruptedIOException e) {
                break; // cool, what we want....
            } 
            catch (BadPacketException e) {}
            catch (IOException ioe) {}
        }
        if (!gotQR) 
            fail("Did not get expected QR 2!!");
    }

    public void testTCPConnectBack() throws Exception {
        if (!_testTCPCB) 
            fail("TCP ConnectBack not supported - ignoring test.");
        
        drain(_leaf1);
        drain(_leaf2);
        
        _tcpSock.setSoTimeout(5*1000); // wait for up to 5 seconds...
        final TCPConnectBackVendorMessage tcp = 
           new TCPConnectBackVendorMessage(_tcpSock.getLocalPort());
        final Connection c = _leaf1;
        Thread sendThread = new Thread() {
            public void run() {
                try {
                    sleep(2*1000);
                }
                catch (InterruptedException whatever) {}
                try {
                    c.send(tcp);
                    c.flush();
                }
                catch (Exception e) {
                    fail("Couldn't send tcp!!", e);
                }
            }
        };
        sendThread.start();

        try {
            _tcpSock.accept();  // wait for the TCP ConnectBack...
            fail("Did recieve TCP ConnectBack!!");
        }
        catch (Exception good) {
        }

        // we be golden dawg!!!
    }

    public void testUDPConnectBack() throws Exception {
        if(!_testUDPCB)
            fail("UDP Connectback not supported - ignoring test");
        
        drain(_leaf1);
        drain(_leaf2);

        _udpSock.setSoTimeout(5*1000); // wait for up to 5 seconds...
        GUID guid = new GUID(GUID.makeGuid());
        DatagramPacket dp = new DatagramPacket(new byte[200], 200);

        final UDPConnectBackVendorMessage udp = 
           new UDPConnectBackVendorMessage(_udpSock.getLocalPort(),
                                           guid);
        final Connection c = _leaf1;
        Thread sendThread = new Thread() {
            public void run() {
                try {
                    sleep(2*1000);
                }
                catch (InterruptedException whatever) {}
                try {
                    c.send(udp);
                    c.flush();
                }
                catch (Exception e) {
                    fail("Couldn't send udp!!", e);
                }
            }
        };
        sendThread.start();

        try {
            _udpSock.receive(dp);  // wait for the UDP ConnectBack...
            ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData());
            ProviderHacks.getMessageFactory().read(bais);
            fail("Did recieve UDP ConnectBack!!");
        } catch (InterruptedIOException good) {
        }

        // we be golden dawg!!!
    }
}
    
