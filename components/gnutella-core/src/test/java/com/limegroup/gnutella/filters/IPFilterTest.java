package com.limegroup.gnutella.filters;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.FilterSettings;

import org.jmock.Expectations;
import org.jmock.Mockery;

public class IPFilterTest extends BaseTestCase {

    private byte[] whiteListedAddress;
    private byte[] blackListedAddress;
    private byte[] toBanAddress;
    private InetAddress whiteListedIP;
    private InetAddress blackListedIP;
    private InetAddress toBanIP;
    
    private Mockery context;
    
    PingReply pingReply;
    QueryReply queryReply;
    QueryRequest queryRequest;

    public IPFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(IPFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {
                "18.239.0.*", "13.0.0.0" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES
                .setValue(new String[] { "18.239.0.144" });
        
        
        whiteListedAddress = new byte[] { (byte) 18, (byte) 239, (byte) 0, (byte) 144 };
        blackListedAddress = new byte[] { (byte) 18, (byte) 239, (byte) 0, (byte) 143 };
        toBanAddress       = new byte[] { (byte) 66, (byte) 66, (byte) 66, (byte) 66 };
        
        whiteListedIP = InetAddress.getByAddress(whiteListedAddress);
        blackListedIP = InetAddress.getByAddress(blackListedAddress);
        toBanIP       =  InetAddress.getByAddress(toBanAddress);
        
        context = new Mockery();
        
        pingReply    = context.mock(PingReply.class);
        queryReply   = context.mock(QueryReply.class);
        queryRequest = context.mock(QueryRequest.class);
    }

    
    public void testFilterWithBan() {
        IPFilter filter = new IPFilter();
        InetSocketAddress toBanSock = new InetSocketAddress(toBanIP, 666);
        assertTrue(filter.allow(toBanSock.getAddress().getHostAddress()));
        filter.ban(toBanSock);    
        
        // This has to be called before the filter applies, is it correct?
        filter.refreshHosts();
        
        assertFalse(filter.allow(toBanSock.getAddress().getHostAddress()));
        
    }

    
    public void testFilterByAddress() {
        IPFilter filter = new IPFilter();
        assertTrue(filter.allow("18.240.0.0"));
        assertFalse(filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertFalse(filter.allow("13.0.0.0"));
        assertTrue(filter.allow("13.0.0.1"));
    }

    public void testFilterByPingReply_WhiteListed() {
        IPFilter filter = new IPFilter();
     
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
    
    public void testFilterByPingReply_BlackListed() {
        IPFilter filter = new IPFilter();
        
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

    public void testFilterByQuery() {
        IPFilter filter = new IPFilter();

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
        
        // Ensure that the message is ignored as 
        //  QueryRequests should always pass through the 
        //  filter.
        context.checking(new Expectations()
        {{ never(queryRequest);
        }});
        
        assertTrue(filter.allow(queryRequest));
        context.assertIsSatisfied();
    }

    public void testFilterByPushRequest() {
        IPFilter filter = new IPFilter();
        PushRequest push1 = new PushRequest(new byte[16], (byte) 3,
                new byte[16], 0l, whiteListedAddress, 6346);
        assertTrue(filter.allow(push1));

        PushRequest push2 = new PushRequest(new byte[16], (byte) 3,
                new byte[16], 0l, blackListedAddress, 6346);
        assertTrue(!filter.allow(push2));
    }
        
}
