package com.limegroup.gnutella.downloader;


import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.BaseTestCase;
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
    
    
    static SourceRanker ranker;
    static MockPinger pinger;
    
    public static void globalSetUp()  {
        pinger = new MockPinger();
    }
    
    public void setUp() throws Exception {
        pinger.messages.clear();
        pinger.hosts.clear();
        ranker = new PingRanker(pinger);
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
