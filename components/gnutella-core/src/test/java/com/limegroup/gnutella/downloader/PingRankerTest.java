package com.limegroup.gnutella.downloader;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UDPReplyHandler;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.IpPort;


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
    
    static MockPinger pinger;
    static PingRanker ranker;
     
    
    /**
     * file descs for the partial and complete files
     *
     *
     */
    public static void globalSetUp()  {
        // set up a mock pinger
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
        MockPong pong = new MockPong(true,true,0,true,false,true,null,null,null);
        ranker.processMessage(pong, new UDPReplyHandler(InetAddress.getByName("1.2.3.5"),1));
        
        // now this host should be prefered over other hosts.
        RemoteFileDesc rfd = ranker.getBest();
        assertEquals("1.2.3.5",rfd.getHost());
    }
    
    /**
     * Tests that the ranker discards sources that claim they do not have the file.
     */
    public void testDiscardsNoFile() throws Exception {
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        assertTrue(ranker.hasMore());
        MockPong pong = new MockPong(false,true,0,true,false,true,null,null,null);
        assertFalse(pong.hasFile());
        
        ranker.processMessage(pong,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        assertFalse(ranker.hasMore());
    }
    
    /**
     * Tests that the ranker offers hosts that indicated they were busy last
     */
    public void testBusyOfferedLast() throws Exception {
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        ranker.addToPool(newRFDWithURN("1.2.3.5",3));
        MockPong busy = new MockPong(true,true,0,true,true,true,null,null,null);
        MockPong notBusy = new MockPong(true,true,0,true,false,true,null,null,null);
        
        ranker.processMessage(busy,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        ranker.processMessage(notBusy,new UDPReplyHandler(InetAddress.getByName("1.2.3.5"),1));
        
        RemoteFileDesc best = ranker.getBest();
        assertEquals("1.2.3.5",best.getHost());
        best = ranker.getBest();
        assertEquals("1.2.3.4",best.getHost());
    }
    
    /**
     * Tests that the ranker offers hosts that have more free slots first 
     */
    public void testSortedByQueueRank() throws Exception {
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        ranker.addToPool(newRFDWithURN("1.2.3.5",3));
        ranker.addToPool(newRFDWithURN("1.2.3.6",3));
        
        MockPong oneFree = new MockPong(true,true,-1,true,false,true,null,null,null);
        MockPong noFree = new MockPong(true,true,0,true,false,true,null,null,null);
        MockPong oneQueue = new MockPong(true,true,1,true,false,true,null,null,null);
        
        ranker.processMessage(oneQueue,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        ranker.processMessage(oneFree,new UDPReplyHandler(InetAddress.getByName("1.2.3.5"),1));
        ranker.processMessage(noFree,new UDPReplyHandler(InetAddress.getByName("1.2.3.6"),1));
        
        RemoteFileDesc best = ranker.getBest();
        assertEquals("1.2.3.5",best.getHost());
        best = ranker.getBest();
        assertEquals("1.2.3.6",best.getHost());
        best = ranker.getBest();
        assertEquals("1.2.3.4",best.getHost());
    }
    
    /**
     * tests that within the same rank, partial sources are preferred
     */
    public void testPartialPreferred() throws Exception {
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        ranker.addToPool(newRFDWithURN("1.2.3.5",3));
        ranker.addToPool(newRFDWithURN("1.2.3.6",3));
        
        MockPong oneFree = new MockPong(true,true,-1,true,false,true,null,null,null);
        MockPong oneFreePartial = new MockPong(true,false,-1,true,false,true,new IntervalSet(),null,null);
        MockPong noSlotsFull = new MockPong(true,true,0,true,false,true,null,null,null);
        
        ranker.processMessage(noSlotsFull,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        ranker.processMessage(oneFree,new UDPReplyHandler(InetAddress.getByName("1.2.3.5"),1));
        ranker.processMessage(oneFreePartial,new UDPReplyHandler(InetAddress.getByName("1.2.3.6"),1));
        
        RemoteFileDesc best = ranker.getBest();
        assertEquals("1.2.3.6",best.getHost());
        best = ranker.getBest();
        assertEquals("1.2.3.5",best.getHost());
        best = ranker.getBest();
        assertEquals("1.2.3.4",best.getHost());
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
    
    /**
     * a very customizable HeadPong
     */
    static class MockPong extends HeadPong {
        
        private Set altLocs, pushLocs;
        private boolean have, full, firewalled, busy, downloading;
        private IntervalSet ranges;
        private int queueStatus;
        public MockPong(boolean have, boolean full, int queueStatus, 
                boolean firewalled, boolean busy, boolean downloading,
                IntervalSet ranges, Set altlocs, Set pushLocs) 
        throws IOException{
            super(new HeadPing(URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE")));
            this.altLocs = altLocs;
            this.pushLocs = pushLocs;
            this.queueStatus = queueStatus;
            this.have = have;
            this.full = full;
            this.firewalled = firewalled;
            this.busy = busy;
            this.downloading = downloading;
            this.ranges = ranges;
        }

        public Set getAllLocsRFD(RemoteFileDesc original) {
            Set ret = new HashSet();
            
            if (altLocs!=null)
                for(Iterator iter = altLocs.iterator();iter.hasNext();) {
                    IpPort current = (IpPort)iter.next();
                    ret.add(new RemoteFileDesc(original,current));
                }
            
            if (pushLocs!=null)
                for(Iterator iter = pushLocs.iterator();iter.hasNext();) {
                    PushEndpoint current = (PushEndpoint)iter.next();
                    ret.add(new RemoteFileDesc(original,current));
                }
            
            return ret;
        }

        public Set getAltLocs() {
            return this.altLocs;
        }

        public Set getPushLocs() {
            return pushLocs;
        }
        
        public int getQueueStatus() {
            return queueStatus;
        }
        
        public IntervalSet getRanges() {
            return ranges;
        }
        
        public boolean hasCompleteFile() {
            return full;
        }
        
        public boolean hasFile() {
            return have;
        }
        
        public boolean isBusy() {
            return busy;
        }
        
        public boolean isDownloading() {
            return downloading;
        }
        
        public boolean isFirewalled() {
            return firewalled;
        }
        
        
    }

}
