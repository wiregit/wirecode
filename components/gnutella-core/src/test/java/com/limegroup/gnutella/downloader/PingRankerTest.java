package com.limegroup.gnutella.downloader;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UDPReplyHandler;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.PrivilegedAccessor;


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
        ranker = new PingRanker();
        PrivilegedAccessor.setValue(ranker,"pinger",pinger);
        DownloadSettings.MAX_VERIFIED_HOSTS.revertToDefault();
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",Boolean.FALSE);
    }
    
    /**
     * Tests that the ranker sends out a HeadPing requesting ranges and alts to given hosts.
     */
    public void testPingsNewHosts() throws Exception {
        for (int i =1;i <= 10;i++) 
            ranker.addToPool(newRFDWithURN("1.2.3."+i,3));
        
        
        assertEquals(10,pinger.hosts.size());
        assertEquals(10,pinger.messages.size());
        
        for (int i = 0 ;i < 10; i++) {
            pinger.hosts.contains(newRFDWithURN("1.2.3."+i,3));
            HeadPing ping = (HeadPing) pinger.messages.get(i);
            assertTrue(ping.requestsRanges());
            assertTrue(ping.requestsAltlocs());
        }
    }
    
    /**
     * Tests that the ranker stops looking for new hosts once it has found enough.
     */
    public void testStopsPinging() throws Exception {
        DownloadSettings.MAX_VERIFIED_HOSTS.setValue(1);
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        Thread.sleep(100);
        assertEquals(1,pinger.hosts.size());
        
        // get a reply from that host
        MockPong pong = new MockPong(true,true,-1,false,false,true,null,null,null);
        ranker.processMessage(pong,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        
        // add some more hosts
        for (int i =1;i <= 10;i++) 
            ranker.addToPool(newRFDWithURN("1.2.3."+i,3));
        
        
        // no more pings should have been sent out.
        assertEquals(1,pinger.hosts.size());
        
        // consume the host we know about
        ranker.getBest();
        
        // we should send out some more pings
        assertGreaterThan(1,pinger.hosts.size());
    }
    
    /**
     * Tests that the ranker learns about new hosts from altlocs
     */
    public void testLearnsFromAltLocs() throws Exception {
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",Boolean.TRUE);
        RemoteFileDesc original = newRFDWithURN("1.2.3.4",3); 
        ranker.addToPool(original);
        assertEquals(1,pinger.hosts.size());
        pinger.hosts.clear();
        
        // send one altloc 
        IpPort ip = new IpPortImpl("1.2.3.5",1);
        Set alts = new HashSet();
        alts.add(ip);
        
        //and one push loc
        PushEndpoint pe =new PushEndpoint((new GUID(GUID.makeGuid())).toHexString()+";1.2.3.6:7");
        Set push = new HashSet();
        push.add(pe);
        
        MockPong pong = new MockPong(true,true,1,false,false,false,null,alts,push);
        ranker.processMessage(pong,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        
        // now the ranker should know about more than one host.
        // the best host should be the one that actually replied.
        RemoteFileDesc best = ranker.getBest();
        assertEquals(original,best);
        
        // the ranker should have more available hosts, even if we haven't
        // pinged any.
        assertTrue(ranker.hasMore());
        
        // after a while, the ranker should have pinged the other two hosts.
        assertEquals(2,pinger.hosts.size());
    }
    
    /**
     * Tests that the ranker sends a HeadPing to the push proxies of a firewalled
     * source as long as we are not firewalled or can do FWT
     */
    public void testPingsFirewalledHosts() throws Exception {
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",Boolean.TRUE);
        assertTrue(RouterService.acceptedIncomingConnection());
        GUID g = new GUID(GUID.makeGuid());
        ranker.addToPool(newPushRFD(g.bytes(),"1.2.2.2:3","2.2.2.3:5"));
        assertEquals(1,pinger.hosts.size());
        assertIpPortEquals(new IpPortImpl("1.2.2.2",3),pinger.hosts.get(0));
        HeadPing ping = (HeadPing)pinger.messages.get(0);
        assertEquals(g,ping.getClientGuid());
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",Boolean.FALSE);
    }
    
    /**
     * tests that we do not ping firewalled hosts if we cannot do FWT and are firewalled
     */
    public void testSkipsFirewalledHosts() throws Exception {
        assertFalse(RouterService.acceptedIncomingConnection());
        assertFalse(RouterService.getUdpService().canDoFWT());
        GUID g = new GUID(GUID.makeGuid());
        ranker.addToPool(newPushRFD(g.bytes(),"1.2.2.2:3","2.2.2.3:5"));
        assertEquals(0,pinger.hosts.size());
        assertEquals(0,pinger.hosts.size());
    }
    
    /**
     * We should drop unsolicited pongs.
     */
    public void testIgnoresUnsolicitedPongs() throws Exception {
        // add a host
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        
        // receive a pong from another host
        MockPong pong = new MockPong(true,true,0,true,false,true,null,null,null);
        ranker.processMessage(pong, new UDPReplyHandler(InetAddress.getByName("1.2.3.5"),1));
        
        // consume the first guy
        assertTrue(ranker.hasMore());
        ranker.getBest();
        
        // we shouldn't have any more hosts available
        assertFalse(ranker.hasMore());
    }
    
    /**
     * When sending a ping to several push proxies, we may get replies
     * from more than one - only one should be processed. 
     */
    public void testMultipleProxyReplies() throws Exception {
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",Boolean.TRUE);
        assertTrue(RouterService.acceptedIncomingConnection());
        GUID g = new GUID(GUID.makeGuid());
        ranker.addToPool(newPushRFD(g.bytes(),"1.2.2.2:3;1.3.3.3:4","2.2.2.3:5"));
        Thread.sleep(100);
        
        // two pings should be sent out
        assertEquals(2,pinger.hosts.size());
        Set s = new TreeSet(IpPort.COMPARATOR);
        s.addAll(pinger.hosts);
        assertTrue(s.contains(new IpPortImpl("1.2.2.2",3)));
        assertTrue(s.contains(new IpPortImpl("1.3.3.3",4)));
        
        // receive one pong from each proxy
        MockPong pong = new MockPong(true,true,-1,true,false,true,null,null,null);
        ranker.processMessage(pong,new UDPReplyHandler(InetAddress.getByName("1.3.3.3"),4));
        ranker.processMessage(pong,new UDPReplyHandler(InetAddress.getByName("1.2.2.2"),3));
        
        // there should be only one host available to try
        assertTrue(ranker.hasMore());
        ranker.getBest();
        assertFalse(ranker.hasMore());
    }
    
    /**
     * Tests that the ranker prefers hosts that have sent a pong back but in 
     * case it runs out of verified hosts it returns a non-verified one.
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
     
        // but if we ask for more hosts we'll get some of the unverified ones
        assertTrue(ranker.hasMore());
        rfd = ranker.getBest();
        assertNotEquals("1.2.3.5",rfd.getHost());
        assertTrue(rfd.getHost().startsWith("1.2.3."));
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
        MockPong busy = new MockPong(true,true,20,true,true,true,null,null,null);
        MockPong notBusy = new MockPong(true,true,0,true,false,true,null,null,null);
        
        ranker.processMessage(busy,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        ranker.processMessage(notBusy,new UDPReplyHandler(InetAddress.getByName("1.2.3.5"),1));
        
        RemoteFileDesc best = ranker.getBest();
        assertEquals("1.2.3.5",best.getHost()); // not busy
        best = ranker.getBest();
        assertEquals("1.2.3.4",best.getHost()); // busy
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
        assertEquals("1.2.3.5",best.getHost()); // one free slot
        best = ranker.getBest();
        assertEquals("1.2.3.6",best.getHost()); // no free slots
        best = ranker.getBest();
        assertEquals("1.2.3.4",best.getHost()); // one queued
    }
    
    /**
     * tests that within the same queue rank the firewalled hosts are preferred
     * if we can't do fwt
     */
    public void testFirewalledPreferred() throws Exception {
        
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",Boolean.TRUE);
        assertTrue(RouterService.acceptedIncomingConnection());
        
        RemoteFileDesc open = newRFDWithURN("1.2.3.4",3);
        RemoteFileDesc openMoreSlots = newRFDWithURN("1.2.3.5",3);
        RemoteFileDesc push = newPushRFD(GUID.makeGuid(),"1.2.3.6:6",null);
        
        ranker.addToPool(open);
        ranker.addToPool(openMoreSlots);
        ranker.addToPool(push);
        
        Thread.sleep(100);
        
        MockPong openPong = new MockPong(true,true,-1,false,false,true,null,null,null);
        MockPong pushPong = new MockPong(true,true,-1,true,false,true,null,null,null);
        MockPong openMorePong = new MockPong(true,true,-2,true,false,true,null,null,null);
        
        ranker.processMessage(openPong,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        ranker.processMessage(openMorePong,new UDPReplyHandler(InetAddress.getByName("1.2.3.5"),1));
        ranker.processMessage(pushPong,new UDPReplyHandler(InetAddress.getByName("1.2.3.6"),6));
        
        RemoteFileDesc best = ranker.getBest();
        assertEquals("1.2.3.5",best.getHost()); // open with more slots
        best = ranker.getBest();
        assertTrue(best.getPushProxies().contains(new IpPortImpl("1.2.3.6",6))); // firewalled
        best = ranker.getBest();
        assertEquals("1.2.3.4",best.getHost()); // open
        
    }
    
    /**
     * tests that within the same rank and firewall status, partial sources are preferred
     */
    public void testPartialPreferred() throws Exception {
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming",Boolean.TRUE);
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        ranker.addToPool(newRFDWithURN("1.2.3.5",3));
        ranker.addToPool(newRFDWithURN("1.2.3.6",3));
        ranker.addToPool(newPushRFD(GUID.makeGuid(),"1.2.3.7:7",null));
        
        MockPong oneFree = new MockPong(true,true,-1,true,false,true,null,null,null);
        MockPong oneFreePartial = new MockPong(true,false,-1,true,false,true,new IntervalSet(),null,null);
        MockPong noSlotsFull = new MockPong(true,true,0,true,false,true,null,null,null);
        MockPong oneFreeOpen= new MockPong(true,true,-1,false,false,true,null,null,null);
        
        ranker.processMessage(noSlotsFull,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        ranker.processMessage(oneFree,new UDPReplyHandler(InetAddress.getByName("1.2.3.5"),1));
        ranker.processMessage(oneFreePartial,new UDPReplyHandler(InetAddress.getByName("1.2.3.6"),1));
        ranker.processMessage(oneFreeOpen,new UDPReplyHandler(InetAddress.getByName("1.2.3.7"),7));
        
        RemoteFileDesc best = ranker.getBest();
        assertEquals("1.2.3.6",best.getHost()); // partial, firewalled, one slot
        best = ranker.getBest();
        assertEquals("1.2.3.5",best.getHost()); // full, firewalled, one slot 
        best = ranker.getBest();
        assertTrue(best.getPushProxies().contains(new IpPortImpl("1.2.3.7",7))); // full, open, one slot
        best = ranker.getBest();
        assertEquals("1.2.3.4",best.getHost()); // full, no slots, firewalled
    }
    
    /**
     * Tests that the ranker passes on to other rankers all the hosts it has been
     * told or learned about.
     */
    public void testGetShareable() throws Exception {
        RemoteFileDesc rfd1, rfd2;
        rfd1 = newRFDWithURN("1.2.3.4",3);
        rfd2 = newRFDWithURN("1.2.3.5",3);
        ranker.addToPool(rfd1);
        ranker.addToPool(rfd2);
        
        Collection c = ranker.getShareableHosts();
        assertTrue(c.contains(rfd1));
        assertTrue(c.contains(rfd2));
        assertEquals(2,c.size());
        
        Thread.sleep(100);
        
        // tell the ranker about some altlocs through a headpong
        IpPort ip1, ip2;
        ip1 = new IpPortImpl("1.2.3.6",3);
        ip2 = new IpPortImpl("1.2.3.7",3);
        Set alts = new HashSet();
        alts.add(ip1);
        alts.add(ip2);
        MockPong oneFreeOpen= new MockPong(true,true,-1,false,false,true,null,alts,null);
        ranker.processMessage(oneFreeOpen,new UDPReplyHandler(InetAddress.getByName("1.2.3.4"),1));
        
        // the ranker should pass on the altlocs it discovered as well.
        c = ranker.getShareableHosts();
        assertEquals(4,c.size());
        TreeSet s = new TreeSet(IpPort.COMPARATOR);
        s.addAll(c);
        assertEquals(4,s.size());
        assertTrue(s.contains(ip1));
        assertTrue(s.contains(ip2));
        assertTrue(s.contains(rfd1));
        assertTrue(s.contains(rfd2));
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
     * constructs an rfd for testing.  if the host parameter is not null, the 
     * rfd indicates FWT capability
     */
    private static RemoteFileDesc newPushRFD(byte [] guid,String proxy, String host) 
    throws IOException{
        GUID g = new GUID(guid);
        String s = g.toHexString();
        if (host != null)
            s = s+";fwt/1.0;" +host.substring(host.indexOf(":")+1)+host.substring(0,host.indexOf(":"));
        else 
            host = "1.1.1.1";
         s =s+ ";"+proxy;
        
        PushEndpoint pe = new PushEndpoint(s);
        RemoteFileDesc ret = newRFDWithURN(host,3);
        ret = new RemoteFileDesc(ret,pe);
        return ret;
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
        
        public void rank(Collection hosts, MessageListener listener, 
                Cancellable canceller, Message message) {
            for (Iterator iter = hosts.iterator(); iter.hasNext();) {
                this.hosts.add(iter.next());
                messages.add(message);
            }
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
                IntervalSet ranges, Set altLocs, Set pushLocs) 
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

    private static void assertIpPortEquals(IpPort a, Object b) {
        assertTrue(IpPort.COMPARATOR.compare(a,b) == 0);
    }
}
