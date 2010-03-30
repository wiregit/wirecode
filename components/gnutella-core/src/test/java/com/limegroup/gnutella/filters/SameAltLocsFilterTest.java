package com.limegroup.gnutella.filters;

import java.util.Collections;
import java.util.Set;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.spam.SpamManager;

public class SameAltLocsFilterTest extends LimeTestCase {

    private Mockery context;
    private QueryReply qr;
    private SpamManager spamManager;
    private SameAltLocsFilter filter;

    public SameAltLocsFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SameAltLocsFilterTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new Mockery();
        qr = context.mock(QueryReply.class);
        spamManager = context.mock(SpamManager.class);
        filter = new SameAltLocsFilter(spamManager);
    }

    public void testAllowsQueryReplyWithNoResponses() throws Exception {
        context.checking(new Expectations() {{
            one(qr).getResultsArray();
            will(returnValue(new Response[0]));
        }});
        assertTrue(filter.allow(qr));
        context.assertIsSatisfied();
    }

    public void testAllowsQueryReplyWithOneResponse() throws Exception {
        context.checking(new Expectations() {{
            one(qr).getResultsArray();
            will(returnValue(new Response[] { null }));
        }});
        assertTrue(filter.allow(qr));
        context.assertIsSatisfied();
    }

    public void testAllowsTwoResponsesWithNoAltLocs() throws Exception {
        final Response r1 = context.mock(Response.class);
        final Response r2 = context.mock(Response.class);
        context.checking(new Expectations() {{
            one(qr).getResultsArray();
            will(returnValue(new Response[] { r1, r2 }));
            one(r1).getLocations();
            will(returnValue(Collections.emptySet()));
        }});
        assertTrue(filter.allow(qr));
        context.assertIsSatisfied();       
    }

    public void testAllowsTwoResponsesWithDifferentAltLocs() throws Exception {
        final Response r1 = context.mock(Response.class);
        final Response r2 = context.mock(Response.class);
        final Set<IpPort> alts1 = new IpPortSet();
        alts1.add(new IpPortImpl("1.2.3.4", 5678));
        alts1.add(new IpPortImpl("2.3.4.5", 6789));
        final Set<IpPort> alts2 = new IpPortSet();
        alts2.add(new IpPortImpl("3.4.5.6", 7890));
        alts2.add(new IpPortImpl("4.5.6.7", 8901));
        context.checking(new Expectations() {{
            one(qr).getResultsArray();
            will(returnValue(new Response[] { r1, r2 }));
            one(r1).getLocations();
            will(returnValue(alts1));
            one(r2).getLocations();
            will(returnValue(alts2));
        }});
        assertTrue(filter.allow(qr));
        context.assertIsSatisfied();
    }    

    public void testDoesNotAllowTwoResponsesWithSameAltLocs() throws Exception {
        final Response r1 = context.mock(Response.class);
        final Response r2 = context.mock(Response.class);
        final Set<IpPort> alts1 = new IpPortSet();
        alts1.add(new IpPortImpl("1.2.3.4", 5678));
        alts1.add(new IpPortImpl("2.3.4.5", 6789));
        context.checking(new Expectations() {{
            one(qr).getResultsArray();
            will(returnValue(new Response[] { r1, r2 }));
            one(r1).getLocations();
            will(returnValue(alts1));
            one(r2).getLocations();
            will(returnValue(alts1));
            one(spamManager).handleSpamQueryReply(qr);
        }});
        assertFalse(filter.allow(qr));
        context.assertIsSatisfied();
    }    

    public void testDoesNotAllowTwoResponsesWithOverlappingAltLocs() throws Exception {
        final Response r1 = context.mock(Response.class);
        final Response r2 = context.mock(Response.class);
        final Set<IpPort> alts1 = new IpPortSet();
        alts1.add(new IpPortImpl("1.2.3.4", 5678));
        alts1.add(new IpPortImpl("2.3.4.5", 6789));
        final Set<IpPort> alts2 = new IpPortSet();
        alts2.add(new IpPortImpl("3.4.5.6", 7890));
        alts2.add(new IpPortImpl("1.2.3.4", 5678));
        context.checking(new Expectations() {{
            one(qr).getResultsArray();
            will(returnValue(new Response[] { r1, r2 }));
            one(r1).getLocations();
            will(returnValue(alts1));
            one(r2).getLocations();
            will(returnValue(alts2));
            one(spamManager).handleSpamQueryReply(qr);
        }});
        assertFalse(filter.allow(qr));
        context.assertIsSatisfied();
    }
}
