package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import junit.framework.Test;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Set;
import com.sun.java.util.collections.Random;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideValidateIncomingTest extends ClientSideTestCase {
    protected static final int PORT=6669;
    protected static int TIMEOUT=1000; // should override super
    private static final long MY_EXPIRE_TIME = 6 * 1000;
    private static final long MY_WAIT_TIME = 500;
    private static final long MY_VALIDATE_TIME = 3 * 1000;

    public ClientSideValidateIncomingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideValidateIncomingTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    ///////////////////////// Actual Tests ////////////////////////////
    
    // THIS TEST SHOULD BE RUN FIRST!!
    public void testTCPConnectRequestSent() throws Exception {
        // send a MessagesSupportedMessage
        testUP[0].send(MessagesSupportedVendorMessage.instance());
        testUP[0].flush();

        // we expect to get a TCPConnectBack request
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof TCPConnectBackVendorMessage)) ;

        // client side seems to follow the setup process A-OK
    }

    // This test checks that if _acceptedIncoming is false, connect back
    // messages are NOT sent
    public void testTCPExpireRequestsNotSent() throws Exception {
        drainAll();
        for (int i = 0; i < 2; i++) {
            assertFalse(rs.acceptedIncomingConnection());

            try {
                testUP[0].receive(TIMEOUT);
            }
            catch (InterruptedIOException expected) {}
            
            Thread.sleep(MY_EXPIRE_TIME+MY_VALIDATE_TIME);
            assertFalse(rs.acceptedIncomingConnection());
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
            assertTrue(rs.acceptedIncomingConnection());
            
            // wait until the expire time is realized
            Thread.sleep(MY_EXPIRE_TIME + MY_VALIDATE_TIME + 1000);
            
            // query the Acceptor - it should send off more requests
            assertFalse(rs.acceptedIncomingConnection());
            Thread.sleep(100);
            Message m = null;
            do {
                m = testUP[0].receive(TIMEOUT);
            } while (!(m instanceof TCPConnectBackVendorMessage)) ;
        }
    }

    // make an incoming to the servent, wait a little, and then make sure
    // it asks for a connect back again
    public void testTCPInterleavingRequestsSent() throws Exception {
        drainAll();
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            if (rand.nextBoolean()) {
                Socket s = new Socket("localhost", PORT);
                s.close();
                Thread.sleep(100);
                assertTrue(rs.acceptedIncomingConnection());
            }
            
            // wait until the expire time is realized
            Thread.sleep(MY_EXPIRE_TIME + MY_VALIDATE_TIME);
            
            // throw some randomness in there - if we get an incoming we should
            // not send messages out
            if (rand.nextBoolean()) {
                Socket s = new Socket("localhost", PORT);
                s.close();
                Thread.sleep(100);
                assertTrue(rs.acceptedIncomingConnection());
                try {
                    testUP[0].receive(TIMEOUT);
                }
                catch (InterruptedIOException expected) {}
            }
            else {
                // query the Acceptor - it should send off more requests
                assertFalse(rs.acceptedIncomingConnection());
                Thread.sleep(100);
                Message m = null;
                do {
                    m = testUP[0].receive(TIMEOUT);
                } while (!(m instanceof TCPConnectBackVendorMessage)) ;
            }
        }
    }

    //////////////////////////////////////////////////////////////////
    public static void doSettings() {
        try {
        PrivilegedAccessor.setValue(Acceptor.class, "INCOMING_EXPIRE_TIME",
                                    new Long(MY_EXPIRE_TIME));
        PrivilegedAccessor.setValue(Acceptor.class, "WAIT_TIME_AFTER_REQUESTS",
                                    new Long(MY_WAIT_TIME));
        PrivilegedAccessor.setValue(Acceptor.class, "TIME_BETWEEN_VALIDATES",
                                    new Long(MY_VALIDATE_TIME));
        }
        catch (Exception bad) {
            assertTrue(false);
        }
    }

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    private static byte[] myIP() {
        return new byte[] { (byte)192, (byte)168, 0, 1 };
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

