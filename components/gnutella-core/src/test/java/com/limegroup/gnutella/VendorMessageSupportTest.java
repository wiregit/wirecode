package com.limegroup.gnutella;

import junit.framework.*;
import java.net.*;
import java.io.*;
import java.util.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.stubs.*;

/** This test should NOT be run by the nightly tests build.
 */
public class VendorMessageSupportTest extends TestCase {
    
    private String _remoteHost = "localhost";
    private int _remotePort = 6000;
    private static final int TIMEOUT = 500;
    private boolean _localTest = true;

    public VendorMessageSupportTest(String name) {
        super(name);
    }

    public VendorMessageSupportTest(String name, String host, int port) {
        this(name);
        _remoteHost = host;
        _remotePort = port;
        _localTest = false;
    }

    public static Test suite() {
        return new TestSuite(VendorMessageSupportTest.class);
    }

    
    private ServerSocket _tcpSock = null;
    private DatagramSocket _udpSock = null;
    private Connection _ultrapeer1 = null;
    private Connection _ultrapeer2 = null;

    private boolean _testHopsFlow = true;
    private boolean _testTCPCB = true;
    private boolean _testUDPCB = true;

    public void connect() {
        debug("Expecting to test Gnutella host on " +
              _remoteHost + ":" + _remotePort);

        // Set up a TCP Listening socket....
        try {
            _tcpSock = new ServerSocket(0);
        }
        catch (Exception unexpected) {
            unexpected.printStackTrace();
            assertTrue(false);
        }

        // Set up a UDP Listening socket....
        try {
            _udpSock = new DatagramSocket();
        }
        catch (Exception unexpected) {
            unexpected.printStackTrace();
            assertTrue(false);
        }

        try {
            // Set up a connection to the host....
            _ultrapeer1=new Connection(_remoteHost, _remotePort, 
                                       new SupernodeProperties(""),
                                       new EmptyResponder());
            _ultrapeer1.initialize();
            // don't do postInit() - you don't want him thinking
            // you support any vendor message....
            
            // Set up another connection to the host....
            _ultrapeer2=new Connection(_remoteHost, _remotePort, 
                                       new SupernodeProperties(""),
                                       new EmptyResponder());
            _ultrapeer2.initialize();
            // don't do postInit() - you don't want him thinking
            // you support any vendor message....

        }
        catch (Exception unexpected) {
            unexpected.printStackTrace();
            assertTrue(false);
        }
    }

    public void testAll() {
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);        
        connect();
        try {
            confirmSupportedMessages();
        }
        catch (Exception e) {
            debug(e);
            if (_localTest)
                assertTrue(false);
            else
                System.out.println("Missing Vendor Message Support!!");
            return;
        }
        try {
            if (_testHopsFlow)
                tryHopsFlow();
        }
        catch (Exception e) {
            debug(e);
            if (_localTest)
                assertTrue(false);
            else
                System.out.println("HOPS FLOW NOT SUPPORTED!!!!!!!!");
            return;
        }
        try {
            if (_testTCPCB)
                tryTCPConnectBack();
        }
        catch (Exception e) {
            debug(e);
            if (_localTest)
                assertTrue(false);
            else
                System.out.println("TCP CONNECTBACK NOT SUPPORTED!!!!!!!!");
            return;
        }
        try {
            if (_testUDPCB)
                tryUDPConnectBack();
        }
        catch (Exception e) {
            debug(e);
            if (_localTest)
                assertTrue(false);
            else
                System.out.println("UDP CONNECTBACK NOT SUPPORTED!!!!!!!!");
            return;
        }
        disconnect();
    }


    private void confirmSupportedMessages() throws Exception {
        testConnection(_ultrapeer1);
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
                    else
                        throw new Exception("Unexpected VendorMessage of class" +
                                            m.getClass());
                }
            }
            catch (InterruptedIOException iioe) {
                break; // received all messages it seems....
            }
            catch (Exception ignoreOthers) {}
        }
        if (!receivedDesiredMessage)
            throw new Exception("No MessagesSupportedMessage received");
        if (c.supportsVendorMessage("BEAR".getBytes(), 4) < 1) {
            _testHopsFlow = false;
            System.out.println("Does not seem to support Hops Flow!!");
        }
        if (c.supportsVendorMessage("GTKG".getBytes(), 7) < 1) {
            _testUDPCB = false;
            System.out.println("Does not seem to support UDP ConnectBack!!");
        }
        if (c.supportsVendorMessage("BEAR".getBytes(), 7) < 1) {
            _testTCPCB = false;
            System.out.println("Does not seem to support TCP ConnectBack!!");
        }
    }

    private void tryHopsFlow() throws Exception {
        drain(_ultrapeer1);
        drain(_ultrapeer2);

        QueryRequest qr = new QueryRequest((byte) 3, 0, "susheel", false);
        
        // first make sure query gets through.....
        _ultrapeer2.send(qr);
        _ultrapeer2.flush();

        boolean gotQR = false;
        
        while (true) {
            try {
                Message m = _ultrapeer1.receive(TIMEOUT);
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
            throw new Exception("Did not get expected QR 1!!");
        

        // now send the hops flow and it shouldn't get through!!
        HopsFlowVendorMessage hops = new HopsFlowVendorMessage((byte)1);
        _ultrapeer1.send(hops);
        _ultrapeer1.flush();

        // wait for the hops flow message to take effect...
        try {
            Thread.sleep(500); 
        }
        catch (Exception whatever) {}

        qr = new QueryRequest((byte) 3, 0, "daswani", false);
        _ultrapeer2.send(qr);
        _ultrapeer2.flush();
        
        while (true) {
            try {
                Message m = _ultrapeer1.receive(TIMEOUT);
                if (m instanceof QueryRequest) 
                    if (((QueryRequest) m).getQuery().equals("daswani"))
                        throw new Exception("Hops Flow Message Ineffectual!!!");
            } 
            catch (InterruptedIOException e) {
                break; // cool, what we want....
            } 
            catch (BadPacketException e) {}
            catch (IOException ioe) {}
        }
        
        // reset Hops Flow and make sure a query gets through....
        hops = new HopsFlowVendorMessage((byte)4);
        _ultrapeer1.send(hops);
        _ultrapeer1.flush();

        qr = new QueryRequest((byte) 3, 0, "foosball", false);
        _ultrapeer2.send(qr);
        _ultrapeer2.flush();

        gotQR = false;
        while (true) {
            try {
                Message m = _ultrapeer1.receive(TIMEOUT);
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
            throw new Exception("Did not get expected QR 2!!");
    }

    private void tryTCPConnectBack() throws Exception {
        drain(_ultrapeer1);
        drain(_ultrapeer2);
        
        _tcpSock.setSoTimeout(5*1000); // wait for up to 5 seconds...
        final TCPConnectBackVendorMessage tcp = 
           new TCPConnectBackVendorMessage(_tcpSock.getLocalPort());
        final Connection c = _ultrapeer1;
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
                    throw new RuntimeException("Couldn't send tcp!!");
                }
            }
        };
        sendThread.start();

        try {
            _tcpSock.accept();  // wait for the TCP ConnectBack...
        }
        catch (Exception broken) {
            throw new Exception("Did not receive TCP ConnectBack!!");
        }

        // we be golden dawg!!!
    }

    private void tryUDPConnectBack() throws Exception {
        drain(_ultrapeer1);
        drain(_ultrapeer2);

        _udpSock.setSoTimeout(5*1000); // wait for up to 5 seconds...
        GUID guid = new GUID(GUID.makeGuid());
        DatagramPacket dp = new DatagramPacket(new byte[200], 200);

        final UDPConnectBackVendorMessage udp = 
           new UDPConnectBackVendorMessage(_udpSock.getLocalPort(),
                                           guid);
        final Connection c = _ultrapeer1;
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
                    throw new RuntimeException("Couldn't send udp!!");
                }
            }
        };
        sendThread.start();

        PingRequest pr = null;
        try {
            _udpSock.receive(dp);  // wait for the UDP ConnectBack...
            ByteArrayInputStream bais =
                new ByteArrayInputStream(dp.getData());
            pr = (PingRequest) Message.read(bais);
        }
        catch (Exception broken) {
            throw new Exception("Did not receive UDP ConnectBack!!");
        }

        if (!Arrays.equals(pr.getGUID(), guid.bytes()))
            throw new Exception("Did not get correct UDP guid back!!");

        // we be golden dawg!!!
    }


    public void disconnect() {
        try {
            _tcpSock.close();
            _udpSock.close();
            _ultrapeer1.close();
            _ultrapeer2.close();
        }
        catch (Exception whatever) {
        }
    }


    /** Tries to receive any outstanding messages on c 
     *  @return true if this got a message */
    private static boolean drain(Connection c) throws IOException {
        boolean ret=false;
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                ret=true;
            } catch (InterruptedIOException e) {
                return ret;
            } catch (BadPacketException e) {
            }
        }
    }
    

    private static final boolean debugOn = true;
    private static final void debug(Exception e) {
        if (debugOn)
            e.printStackTrace();
    }
    private static final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


    public static void main(String[] argv) throws Exception {
        if (argv.length != 2)
            junit.textui.TestRunner.run(suite());
        else {
            String name = "testAll";
            String host = argv[0];
            int port = Integer.parseInt(argv[1]);
            junit.textui.TestRunner.run(new VendorMessageSupportTest(name,
                                                                     host,
                                                                     port));
        }
    }
}
    
