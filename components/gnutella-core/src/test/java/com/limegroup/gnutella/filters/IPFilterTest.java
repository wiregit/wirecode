package com.limegroup.gnutella.filters;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.settings.FilterSettings;

public class IPFilterTest extends BaseTestCase {

    private byte[] whiteListedAddress;

    private byte[] blackListedAddress;

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
    }

    public void testFilterByAddress() {
        IPFilter filter = new IPFilter();
        assertTrue(filter.allow("18.240.0.0"));
        assertTrue(!filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertTrue(!filter.allow("13.0.0.0"));
        assertTrue(filter.allow("13.0.0.1"));
    }

    public void testFilterByPingReply() {
        IPFilter filter = new IPFilter();
        assertTrue(filter.allow((Message) ProviderHacks.getPingReplyFactory()
                .createExternal(new byte[16], (byte) 3, 6346, whiteListedAddress, false)));
    }

    public void testFilterByQuery() {
        IPFilter filter = new IPFilter();

        assertTrue(!filter.allow(ProviderHacks.getQueryReplyFactory()
                .createQueryReply(new byte[16], (byte) 3, 6346, blackListedAddress, 0,
                        new Response[0], new byte[16], false)));
        assertTrue(filter.allow(ProviderHacks.getQueryRequestFactory()
                .createQuery("test", (byte) 3)));
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
