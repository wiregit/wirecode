package com.limegroup.gnutella.filters;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.IP;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.filters.IPFilter.IPFilterCallback;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public class IPFilterTest extends LimeTestCase {

    private byte[] whiteListedAddress;
    private byte[] blackListedAddress;
    private InetAddress whiteListedIP;
    private InetAddress blackListedIP;
    
    private Mockery context;
    
    PingReply pingReply;
    QueryReply queryReply;
    QueryRequest queryRequest;
    
    IPFilter filter;

    public IPFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(IPFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private Injector injector;
    
    @Override
    protected void setUp() throws Exception {
        Module m = new AbstractModule() {
            public void configure() {
                bind(IPFilter.class).annotatedWith(Names.named("hostileFilter")).toInstance(new StubFilter());
                bind(Executor.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(new Executor() {
                    public void execute(Runnable r) {
                        r.run();
                    }
                });
                
            }
        };
        injector = LimeTestUtils.createInjector(m);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {
                "18.239.0.*", "13.0.0.0" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES
                .setValue(new String[] { "18.239.0.144" });
        
        
        whiteListedAddress = new byte[] { (byte) 18, (byte) 239, (byte) 0, (byte) 144 };
        blackListedAddress = new byte[] { (byte) 18, (byte) 239, (byte) 0, (byte) 143 };
        
        whiteListedIP = InetAddress.getByAddress(whiteListedAddress);
        blackListedIP = InetAddress.getByAddress(blackListedAddress);
        
        context = new Mockery();
        
        pingReply    = context.mock(PingReply.class);
        queryReply   = context.mock(QueryReply.class);
        queryRequest = context.mock(QueryRequest.class);
        
        filter = getFilter();
        filter.refreshHosts();
    }
    
    private IPFilter getFilter() {
        return injector.getInstance(Key.get(IPFilter.class,Names.named("ipFilter")));
    }

    
    public void testFilterByAddress() {
        assertTrue(filter.allow("18.240.0.0"));
        assertFalse(filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertFalse(filter.allow("13.0.0.0"));
        assertTrue(filter.allow("13.0.0.1"));
    }

    public void testFilterByPingReplyWhiteListed() {
     
        context.checking(new Expectations()
        {{ allowing (pingReply).getTTL();
           will(returnValue(3));
           allowing (pingReply).getPort();
           will(returnValue(6346));
           atLeast(1).of(pingReply).getAddress();
           will(returnValue(whiteListedIP.getHostAddress()));
           allowing(pingReply).isUltrapeer();
           will(returnValue(false));
        }});
        
        assertTrue(filter.allow(pingReply));
        context.assertIsSatisfied();
    }
    
    public void testFilterByPingReplyBlackListed() {
        
        context.checking(new Expectations()
        {{ allowing (pingReply).getTTL();
           will(returnValue(3));
           allowing (pingReply).getPort();
           will(returnValue(6346));
           atLeast(1).of(pingReply).getAddress();
           will(returnValue(blackListedIP.getHostAddress()));
           allowing(pingReply).isUltrapeer();
           will(returnValue(false));
        }});
        
        assertFalse(filter.allow(pingReply));
        context.assertIsSatisfied();
    }

    /** 
     *  Ensure that the message is ignored as
     *   QueryRequests should always pass through the
     *   filter.
     */
    public void testFilterByQueryRequest() {
        context.checking(new Expectations()
        {{ never(queryRequest);
        }});
        
        assertTrue(filter.allow(queryRequest));
        context.assertIsSatisfied();
    }
    
    public void testFilterByQuery() {
        
        context.checking(new Expectations()
        {{ allowing (queryReply).getTTL();
           will(returnValue(3));
           allowing (queryReply).getPort();
           will(returnValue(6346));
           atLeast(1).of(queryReply).getIPBytes();
           will(returnValue(blackListedAddress));
           allowing(queryReply).getIP();
           will(returnValue(blackListedIP.getHostAddress()));
           allowing(queryReply).isMulticast();
           will(returnValue(false));
        }});
                
        assertFalse(filter.allow(queryReply));
        context.assertIsSatisfied();
    }

    public void testFilterByPushRequest() {

        PushRequest push1 = new PushRequest(new byte[16], (byte) 3,
                new byte[16], 0l, whiteListedAddress, 6346);
        assertTrue(filter.allow(push1));

        PushRequest push2 = new PushRequest(new byte[16], (byte) 3,
                new byte[16], 0l, blackListedAddress, 6346);
        assertFalse(filter.allow(push2));
    }
    
    private static class StubFilter implements IPFilter {

        public boolean allow(byte[] addr) {
            return true;
        }

        public boolean allow(IP ip) {
            return true;
        }

        public boolean allow(SocketAddress addr) {
            return true;
        }

        public boolean allow(String addr) {
            return true;
        }

        public boolean hasBlacklistedHosts() {
            return true;
        }

        public int logMinDistanceTo(IP ip) {
            return 0;
        }

        public void refreshHosts() {
        }

        public void refreshHosts(IPFilterCallback callback) {
        }

        public boolean allow(Message m) {
            return true;
        }
        
    }
        
}
