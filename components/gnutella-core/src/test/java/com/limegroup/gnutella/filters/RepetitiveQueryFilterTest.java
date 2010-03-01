package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.FilterSettings;
import org.limewire.gnutella.tests.LimeTestCase;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;

public class RepetitiveQueryFilterTest extends LimeTestCase {

    private RepetitiveQueryFilter filter;
    private QueryRequest qr;
    private Mockery context;

    public RepetitiveQueryFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RepetitiveQueryFilterTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        FilterSettings.REPETITIVE_QUERY_FILTER_SIZE.setValue(10);
        filter = new RepetitiveQueryFilter();
        context = new Mockery();
        qr = context.mock(QueryRequest.class);
    }

    public void testRepetitionNotAllowedAfterTen() {
        context.checking(new Expectations() {{
            exactly(1).of(qr).getQuery();
            will(returnValue("one"));
            exactly(1).of(qr).getQuery();
            will(returnValue("two"));
            exactly(1).of(qr).getQuery();
            will(returnValue("three"));
            exactly(1).of(qr).getQuery();
            will(returnValue("four"));
            exactly(1).of(qr).getQuery();
            will(returnValue("five"));
            exactly(1).of(qr).getQuery();
            will(returnValue("six"));
            exactly(1).of(qr).getQuery();
            will(returnValue("seven"));
            exactly(1).of(qr).getQuery();
            will(returnValue("eight"));
            exactly(1).of(qr).getQuery();
            will(returnValue("nine"));
            exactly(1).of(qr).getQuery();
            will(returnValue("ten"));
            exactly(1).of(qr).getQuery();
            will(returnValue("one"));
        }});
        for(int i = 0; i < 10; i++) {
            assertTrue(filter.allow(qr));
        }
        assertFalse(filter.allow(qr));
        context.assertIsSatisfied();
    }

    public void testRepetitionAllowedAfterEleven() {
        context.checking(new Expectations() {{
            exactly(1).of(qr).getQuery();
            will(returnValue("one"));
            exactly(1).of(qr).getQuery();
            will(returnValue("two"));
            exactly(1).of(qr).getQuery();
            will(returnValue("three"));
            exactly(1).of(qr).getQuery();
            will(returnValue("four"));
            exactly(1).of(qr).getQuery();
            will(returnValue("five"));
            exactly(1).of(qr).getQuery();
            will(returnValue("six"));
            exactly(1).of(qr).getQuery();
            will(returnValue("seven"));
            exactly(1).of(qr).getQuery();
            will(returnValue("eight"));
            exactly(1).of(qr).getQuery();
            will(returnValue("nine"));
            exactly(1).of(qr).getQuery();
            will(returnValue("ten"));
            exactly(1).of(qr).getQuery();
            will(returnValue("eleven"));
            exactly(1).of(qr).getQuery();
            will(returnValue("one"));
        }});
        for(int i = 0; i < 12; i++) {
            assertTrue(filter.allow(qr));
        }
        context.assertIsSatisfied();
    }

    public void testSettingAdjustsFilterSize() {
        FilterSettings.REPETITIVE_QUERY_FILTER_SIZE.setValue(5);
        filter = new RepetitiveQueryFilter();
        context.checking(new Expectations() {{
            exactly(1).of(qr).getQuery();
            will(returnValue("one"));
            exactly(1).of(qr).getQuery();
            will(returnValue("two"));
            exactly(1).of(qr).getQuery();
            will(returnValue("three"));
            exactly(1).of(qr).getQuery();
            will(returnValue("four"));
            exactly(1).of(qr).getQuery();
            will(returnValue("five"));
            exactly(1).of(qr).getQuery();
            will(returnValue("one"));
        }});
        for(int i = 0; i < 5; i++) {
            assertTrue(filter.allow(qr));
        }
        assertFalse(filter.allow(qr));
        context.assertIsSatisfied();
    }

    public void testSettingOfZeroDisablesFilter() {
        FilterSettings.REPETITIVE_QUERY_FILTER_SIZE.setValue(0);
        filter = new RepetitiveQueryFilter();
        for(int i = 0; i < 1000; i++) {
            assertTrue(filter.allow(qr));
        }
        context.assertIsSatisfied();
    }

    public void testOtherMessagesAreAllowed() {
        PingRequest pr = context.mock(PingRequest.class);
        assertTrue(filter.allow(pr));
    }
}    
