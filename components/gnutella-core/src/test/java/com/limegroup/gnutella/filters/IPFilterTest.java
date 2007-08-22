package com.limegroup.gnutella.filters;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public class IPFilterTest extends LimeTestCase{
    
    public IPFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(IPFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    
    public void testIPFilterLegacy() {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"18.239.0.*", "13.0.0.0"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"18.239.0.144"});
        IPFilter filter = ProviderHacks.getIpFilter();
        filter.refreshHosts();
        assertTrue(filter.allow("18.240.0.0"));
        assertTrue(! filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertTrue(! filter.allow("13.0.0.0"));
        assertTrue(filter.allow("13.0.0.1"));
        byte[] address={(byte)18, (byte)239, (byte)0, (byte)144};
        assertTrue(filter.allow(
            (Message)ProviderHacks.getPingReplyFactory().createExternal(new byte[16], (byte)3, 6346, address, false)));
        byte[] address2=new byte[] {(byte)18, (byte)239, (byte)0, (byte)143};
        assertTrue(! filter.allow(
            ProviderHacks.getQueryReplyFactory().createQueryReply(new byte[16], (byte)3, 6346,
                    address2, 0, new Response[0], new byte[16], false)));
        assertTrue(filter.allow(ProviderHacks.getQueryRequestFactory().createQuery("test", (byte)3)));
        PushRequest push1=new PushRequest( 
            new byte[16], (byte)3, new byte[16], 0l, address, 6346);
        assertTrue(filter.allow(push1));
        PushRequest push2=new PushRequest( 
            new byte[16], (byte)3, new byte[16], 0l, address2, 6346);
        assertTrue(! filter.allow(push2));
    }
}
