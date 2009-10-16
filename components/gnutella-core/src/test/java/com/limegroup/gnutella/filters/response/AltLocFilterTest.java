package com.limegroup.gnutella.filters.response;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.IpPort;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.QueryReply;

public class AltLocFilterTest extends BaseTestCase {

    private Mockery context;
    private IPFilter ipFilter;
    private QueryReply queryReply;
    private Response response;
    private Set<IpPort> alts;
    private AltLocFilter alf;
    private byte[] good1 = new byte[] {1, 2, 3, 4};
    private byte[] good2 = new byte[] {2, 3, 4, 5};
    private byte[] bad1 = new byte[] {3, 4, 5, 6};
    private byte[] bad2 = new byte[] {4, 5, 6, 7};

    public AltLocFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AltLocFilterTest.class);
    }

    @Override
    public void setUp() {
        context = new Mockery();
        ipFilter = context.mock(IPFilter.class);
        queryReply = context.mock(QueryReply.class);
        response = context.mock(Response.class);
        alts = new LinkedHashSet<IpPort>();
        alf = new AltLocFilter(ipFilter);
    }

    public void testResponseWithGoodAltLocsAllowed()
    throws UnknownHostException {
        alts.add(new Endpoint(InetAddress.getByAddress(good1), 1));
        alts.add(new Endpoint(InetAddress.getByAddress(good2), 2));
        context.checking(new Expectations() {{
            one(response).getLocations();
            will(returnValue(alts));
            one(ipFilter).allow(good1);
            will(returnValue(true));
            one(ipFilter).allow(good2);
            will(returnValue(true));
        }});
        assertTrue(alf.allow(queryReply, response));
        context.assertIsSatisfied();
    }

    public void testResponseWithBadAltLocsNotAllowed()
    throws UnknownHostException {
        alts.add(new Endpoint(InetAddress.getByAddress(bad1), 1));
        alts.add(new Endpoint(InetAddress.getByAddress(bad2), 2));
        context.checking(new Expectations() {{
            one(response).getLocations();
            will(returnValue(alts));
            one(ipFilter).allow(bad1);
            will(returnValue(false));
        }});
        assertFalse(alf.allow(queryReply, response));
        context.assertIsSatisfied();
    }

    public void testResponseWithGoodAndBadAltLocsNotAllowed()
    throws UnknownHostException {
        alts.add(new Endpoint(InetAddress.getByAddress(good1), 1));
        alts.add(new Endpoint(InetAddress.getByAddress(bad1), 2));
        context.checking(new Expectations() {{
            one(response).getLocations();
            will(returnValue(alts));
            one(ipFilter).allow(good1);
            will(returnValue(true));
            one(ipFilter).allow(bad1);
            will(returnValue(false));
        }});
        assertFalse(alf.allow(queryReply, response));
        context.assertIsSatisfied();
    }
}
