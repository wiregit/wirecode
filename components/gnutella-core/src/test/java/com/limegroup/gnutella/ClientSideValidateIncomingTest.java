package com.limegroup.gnutella;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.Set;

import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Checks whether leaves request redirects properly.  
 * The test includes a leaf attached to two ultrapeers.
 */
public class ClientSideValidateIncomingTest extends ClientSideTestCase {
    protected static final int PORT=6669;
    protected static int TIMEOUT=1000; // should override super
    private static final long MY_EXPIRE_TIME = 6 * 1000;
    private static final long MY_WAIT_TIME = 500;
    private static final long MY_VALIDATE_TIME = 3 * 1000;

    private static byte[] cbGuid = null;

    public ClientSideValidateIncomingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideValidateIncomingTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        super.setUp();
        ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    }
    
    ///////////////////////// Actual Tests ////////////////////////////
    
    // THIS TEST SHOULD BE RUN FIRST!!
    public void testConnectBackRequestsSent() throws Exception {
        // send a MessagesSupportedMessage
        testUP[0].send(MessagesSupportedVendorMessage.instance());
        testUP[0].flush();
        testUP[1].send(MessagesSupportedVendorMessage.instance());
        testUP[1].flush();

        // we expect to get a TCPConnectBack request
        Message m = null;
        boolean gotTCP = false, gotUDP = false;
        do {
            m = testUP[0].receive(TIMEOUT);
            if (m instanceof TCPConnectBackVendorMessage) gotTCP = true;
            if (m instanceof UDPConnectBackVendorMessage) {
                cbGuid = m.getGUID();
                gotUDP = true;
            }
        } while (!gotTCP || !gotUDP);

        // client side seems to follow the setup process A-OK
    }

    // This test checks that if _acceptedIncoming is false, connect back
    // messages are NOT sent
    public void testTCPExpireRequestsNotSent() throws Exception {
        drainAll();
        for (int i = 0; i < 2; i++) {
            assertFalse(RouterService.acceptedIncomingConnection());

            try {
                testUP[0].receive(TIMEOUT);
            }
            catch (InterruptedIOException expected) {}
            
            Thread.sleep(MY_EXPIRE_TIME+MY_VALIDATE_TIME);
            assertFalse(RouterService.acceptedIncomingConnection());
            Thread.sleep(100);
            // now we should get the connect backs cuz it has been a while
            Message m = null;
            do {
                m = testUP[0].receive(TIMEOUT);
            } while (!(m instanceof TCPConnectBackVendorMessage));
        }
    }

    // make an incoming to the servent, wait a little, and then make sure
    // it asks for a connect back again
    public void testTCPExpireRequestsSent() throws Exception {
        drainAll();
        for (int i = 0; i < 2; i++) {
            Socket s = new Socket("localhost", PORT);
            s.close();
            Thread.sleep(100); 
            // Socket must have said CONNECT BACK
            assertFalse(RouterService.acceptedIncomingConnection());
            
            s = new Socket("localhost", PORT);
            s.getOutputStream().write("CONNECT BACK\r\r".getBytes());
            Thread.sleep(500);
            s.close(); 
            // Socket must have said CONNECT BACK
            assertTrue(RouterService.acceptedIncomingConnection());
            
            // wait until the expire time is realized
            Thread.sleep(MY_EXPIRE_TIME + MY_VALIDATE_TIME + 1000);
            
            // it should send off more requests
            assertFalse(RouterService.acceptedIncomingConnection());
            Message m = null;
            do {
                m = testUP[0].receive(TIMEOUT);
            } while (!(m instanceof TCPConnectBackVendorMessage)) ;
        }
    }
    
    /**
     * Tests that if the leaf is connected to only one ultrapeer it will 
     * send a few redundant requests
     */
    public void testTCPRedundantRequestsSent() throws Exception {
        drainAllParallel(testUP);
        // wait some time - both UPs should get a single connect back
        
        //sleep
        Thread.sleep(MY_VALIDATE_TIME+1000);
        readNumConnectBacks(1,testUP[0], TIMEOUT);
        readNumConnectBacks(1,testUP[1], TIMEOUT);
        
        // leave only one connection open
        assertGreaterThan(1,RouterService.getConnectionManager().getNumInitializedConnections());
        testUP[1].close();
        Thread.sleep(500);
        assertEquals(1,RouterService.getConnectionManager().getNumInitializedConnections());
        
        drainAll();
        // sleep
        Thread.sleep(MY_VALIDATE_TIME+1000);
        
        // we should receive more than one connect back redirects
        readNumConnectBacks(ConnectionManager.CONNECT_BACK_REDUNDANT_REQUESTS,testUP[0],TIMEOUT);
        
    }
    
    private void readNumConnectBacks(int num,Connection conn, int timeout) throws Exception {
        Message m;
        for (int i = 0; i < num; i++) {
            do {
                m = conn.receive(timeout);
            } while (!(m instanceof TCPConnectBackVendorMessage));
        }
        try {
            do {
                m = conn.receive(timeout);
            } while (!(m instanceof TCPConnectBackVendorMessage));
            fail ("got extra message on "+conn);
        } catch (IOException expected) {}
    }

    // This test checks that if _acceptedUnsolicitedIncoming is false, connect
    // back messages are NOT sent
    public void testUDPExpireRequestsNotSent() throws Exception {
        drainAll();
        UDPService udpServ = UDPService.instance();
        for (int i = 0; i < 2; i++) {
            assertFalse(udpServ.canReceiveUnsolicited());

            try {
                testUP[0].receive(TIMEOUT);
            }
            catch (InterruptedIOException expected) {}
            
            Thread.sleep(MY_EXPIRE_TIME+MY_VALIDATE_TIME);
            assertFalse(udpServ.canReceiveUnsolicited());
            Thread.sleep(100);
            // now we should get the connect backs cuz it has been a while
            Message m = null;
            do {
                m = testUP[0].receive(TIMEOUT);
            } while (!(m instanceof UDPConnectBackVendorMessage));
            cbGuid = m.getGUID();
        }
    }

    // make an incoming to the servent, wait a little, and then make sure
    // it asks for a connect back again
    public void testUDPExpireRequestsSent() throws Exception {
        drainAll();
        UDPService udpServ = UDPService.instance();
        for (int i = 0; i < 2; i++) {
            // wait until the expire time is realized
            
            // drain the UDP buffer
            Message m = null;
            try {
                while (true) {
                    m = testUP[0].receive(200);
                }
            }
            catch (InterruptedIOException drained) {}
            // get the UDP message
            do {
                m = testUP[0].receive();
            } while (!(m instanceof UDPConnectBackVendorMessage)) ;
            Thread.sleep(1000);
            assertFalse(udpServ.canReceiveUnsolicited());
            // get the UDP message but this time answer it
            do {
                m = testUP[0].receive();
            } while (!(m instanceof UDPConnectBackVendorMessage)) ;
            cbGuid = 
                ((UDPConnectBackVendorMessage)m).getConnectBackGUID().bytes();

            // now connect back and it should switch on unsolicited
            DatagramSocket s = new DatagramSocket();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PingRequest ping = new PingRequest(cbGuid, (byte)1, (byte)1);
            ping.write(baos);
            DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                                     baos.toByteArray().length,
                                                     InetAddress.getLocalHost(),
                                                     PORT);
            s.send(pack);
            s.close();

            Thread.sleep(1000);
            assertTrue(udpServ.canReceiveUnsolicited());
        }
    }

    // make an incoming to the servent, wait a little, and then make sure
    // it asks for a connect back again
    public void testUDPInterleavingRequestsSent() throws Exception {
        drainAll();
        UDPService udpServ = UDPService.instance();
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            if (rand.nextBoolean()) {
                DatagramSocket s = new DatagramSocket();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ReplyNumberVendorMessage vm  = 
                    new ReplyNumberVendorMessage(new GUID(cbGuid), 1);
                vm.write(baos);
                DatagramPacket pack = 
                    new DatagramPacket(baos.toByteArray(), 
                                       baos.toByteArray().length,
                                       InetAddress.getLocalHost(), PORT);
                s.send(pack);
                s.close();
                Thread.sleep(100);
            }
            
            // wait until the expire time is realized
            Thread.sleep(MY_EXPIRE_TIME + MY_VALIDATE_TIME);
            
            // throw some randomness in there - if we get an incoming we should
            // not send messages out
            if (udpServ.canReceiveUnsolicited()) {
                DatagramSocket s = new DatagramSocket();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ReplyNumberVendorMessage vm  = 
                    new ReplyNumberVendorMessage(new GUID(cbGuid), 1);
                vm.write(baos);
                DatagramPacket pack = 
                    new DatagramPacket(baos.toByteArray(), 
                                       baos.toByteArray().length,
                                       InetAddress.getLocalHost(), PORT);
                s.send(pack);
                s.close();
                Thread.sleep(100);
                assertTrue(udpServ.canReceiveUnsolicited());
                try {
                    testUP[0].receive(TIMEOUT);
                }
                catch (InterruptedIOException expected) {}
            }
            else {
                Thread.sleep(MY_VALIDATE_TIME);
                // query the Acceptor - it should send off more requests
                Thread.sleep(100);
                Message m = null;
                do {
                    m = testUP[0].receive(TIMEOUT);
                } while (!(m instanceof UDPConnectBackVendorMessage)) ;
                cbGuid = 
                ((UDPConnectBackVendorMessage)m).getConnectBackGUID().bytes();

                // now connect back and it should switch on unsolicited
                DatagramSocket s = new DatagramSocket();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PingRequest ping = new PingRequest(cbGuid, (byte)1, (byte)1);
                ping.write(baos);
                DatagramPacket pack = 
                    new DatagramPacket(baos.toByteArray(), 
                                       baos.toByteArray().length,
                                       InetAddress.getLocalHost(), PORT);
                s.send(pack);
                s.close();
            }
        }
    }



    //////////////////////////////////////////////////////////////////
    public static void doSettings() throws Exception {
        PrivilegedAccessor.setValue(Acceptor.class, "INCOMING_EXPIRE_TIME",
                                    new Long(MY_EXPIRE_TIME));
        PrivilegedAccessor.setValue(Acceptor.class, "WAIT_TIME_AFTER_REQUESTS",
                                    new Long(MY_WAIT_TIME));
        PrivilegedAccessor.setValue(Acceptor.class, "TIME_BETWEEN_VALIDATES",
                                    new Long(MY_VALIDATE_TIME));
    }

    public static Integer numUPs() {
        return new Integer(2);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    public static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc rfd = null;
        public RemoteFileDesc getRFD() {
            return rfd;
        }

        public void handleQueryResult(RemoteFileDesc rfd,
                                      HostData data,
                                      Set locs) {
            this.rfd = rfd;
        }
    }


}

