package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.messages.QueryReply;

public class ClientGuidFilterTest extends BaseTestCase {

    Mockery context;
    QueryReply reply;

    public static Test suite() {
        return buildTestSuite(ClientGuidFilterTest.class);
    }

    @Override
    public void setUp() {
        context = new Mockery();
        reply = context.mock(QueryReply.class);
    }

    public void testDoesNotAllowAllZeroClientGuid() {
        final byte[] clientGUID =
            StringUtils.fromHexString("00000000000000000000000000000000");
        context.checking(new Expectations() {{
            one(reply).getClientGUID();
            will(returnValue(clientGUID));
        }});
        assertFalse(new ClientGuidFilter().allow(reply));
    }

    public void testDoesNotAllowAllAToFClientGuid() {
        final byte[] clientGUID =
            StringUtils.fromHexString("ABCDEFABCDEFABCDEFABCDEFABCDEFAB");
        context.checking(new Expectations() {{
            one(reply).getClientGUID();
            will(returnValue(clientGUID));
        }});
        assertFalse(new ClientGuidFilter().allow(reply));
    }

    public void testAllowsZeroesThenAToFClientGuid() {
        final byte[] clientGUID =
            StringUtils.fromHexString("000000ABCDEFABCDEFABCDEFABCDEFAB");
        final byte[] messageGUID =
            StringUtils.fromHexString("0123456789ABCDEF0123456789ABCDEF");
        context.checking(new Expectations() {{
            one(reply).getClientGUID();
            will(returnValue(clientGUID));
            one(reply).getGUID();
            will(returnValue(messageGUID));
        }});
        assertTrue(new ClientGuidFilter().allow(reply));
    }

    public void testAllowsAToFThenZeroesClientGuid() {
        final byte[] clientGUID =
            StringUtils.fromHexString("ABCDEFABCDEFABCDEFABCDEFAB000000");
        final byte[] messageGUID =
            StringUtils.fromHexString("0123456789ABCDEF0123456789ABCDEF");
        context.checking(new Expectations() {{
            one(reply).getClientGUID();
            will(returnValue(clientGUID));
            one(reply).getGUID();
            will(returnValue(messageGUID));
        }});
        assertTrue(new ClientGuidFilter().allow(reply));
    }

    public void testAllowsNormalClientGuid() {
        final byte[] clientGUID =
            StringUtils.fromHexString("ABCDEF0123456789ABCDEF0123456789");
        final byte[] messageGUID =
            StringUtils.fromHexString("0123456789ABCDEF0123456789ABCDEF");
        context.checking(new Expectations() {{
            one(reply).getClientGUID();
            will(returnValue(clientGUID));
            one(reply).getGUID();
            will(returnValue(messageGUID));
        }});
        assertTrue(new ClientGuidFilter().allow(reply));
    }

    public void testDoesNotAllowMatchingGuids() {
        final byte[] clientGUID =
            StringUtils.fromHexString("0123456789ABCDEF0123456789ABCDEF");
        context.checking(new Expectations() {{
            one(reply).getClientGUID();
            will(returnValue(clientGUID));
            one(reply).getGUID();
            will(returnValue(clientGUID));
        }});
        assertFalse(new ClientGuidFilter().allow(reply));
    }
}
