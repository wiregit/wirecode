package com.limegroup.gnutella.downloader;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.IncompleteFileDescStub;
import com.limegroup.gnutella.stubs.UploadManagerStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;


/**
 * tests the functioning of the ping ranker, i.e. how it sends out headpings
 * and how it ranks hosts based on the returned results.
 *
 */
public class PingRankerTest extends BaseTestCase {

    public PingRankerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PingRankerTest.class);
    }
    
    
    static PingRanker ranker;
    static MockPinger pinger;
    
    static HeadPong completeNoLocs;
    
    /**
     * file descs for the partial and complete files
     */
    static IncompleteFileDescStub _partial;
    static FileDescStub _complete;
    
    static FileManagerStub _fm;
    static UploadManagerStub _um;
    
    public static void globalSetUp()  {
        pinger = new MockPinger();
    }
    
    public void setUp() throws Exception {
        pinger.messages.clear();
        pinger.hosts.clear();
        ranker = new PingRanker(pinger);
    }
    
    /**
     * Tests that the ranker sends out a HeadPing requesting ranges to given hosts.
     */
    public void testPingsNewHosts() throws Exception {
        for (int i =1;i <= 10;i++) 
            ranker.addToPool(newRFDWithURN("1.2.3."+i,3));
        
        Thread.sleep(100);
        
        assertEquals(10,pinger.hosts.size());
        assertEquals(10,pinger.messages.size());
        
        for (int i = 0 ;i < 10; i++) {
            pinger.hosts.contains(newRFDWithURN("1.2.3."+i,3));
            HeadPing ping = (HeadPing) pinger.messages.get(i);
            assertTrue(ping.requestsRanges());
            assertFalse(ping.requestsAltlocs());
        }
    }
    
    /**
     * Tests that the ranker prefers hosts that have sent a pong back.
     */
    public void testPrefersPongedHost() throws Exception {
        assertFalse(ranker.hasMore());
        
        for (int i =0;i < 10;i++) 
            ranker.addToPool(newRFDWithURN("1.2.3."+i,3));
        
        assertTrue(ranker.hasMore());
        
        Thread.sleep(100);
        
        // send a pong back from a single host
    }
    
    
    private static RemoteFileDesc newRFD(String host, int speed){
        return new RemoteFileDesc(host, 1,
                                  0, "asdf",
                                  TestFile.length(), new byte[16],
                                  speed, false, 4, false, null, null,
                                  false,false,"",0,null, -1);
    }

    private static RemoteFileDesc newRFDWithURN() {
        return newRFDWithURN();
    }

    private static RemoteFileDesc newRFDWithURN(String host, int speed) {
        Set set = new HashSet();
        try {
            // for convenience, don't require that they pass the urn.
            // assume a null one is the TestFile's hash.
            set.add(TestFile.hash());
        } catch(Exception e) {
            fail("SHA1 not created");
        }
        return new RemoteFileDesc(host, 1,
                                  0, "asdf",
                                  TestFile.length(), new byte[16],
                                  speed, false, 4, false, null, set,
                                  false, false,"",0,null, -1);
    }
    
    /**
     * a mock pinger.  Note that the base code will still register messsage listeners
     * but we don't care because they will never be used.
     */
    static class MockPinger extends UDPPinger {

        /**
         * the list of messages that was sent
         */
        public List messages = new ArrayList();
        
        /**
         * the list of hosts that we pinged, same order as messages
         */
        public List hosts = new ArrayList();
        
        protected synchronized void sendSingleMessage(IpPort host, Message message) {
            messages.add(message);
            hosts.add(host);
        }
        
    }

}
