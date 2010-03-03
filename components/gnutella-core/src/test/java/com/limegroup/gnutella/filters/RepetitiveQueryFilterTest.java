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
            one(qr).getQuery();
            will(returnValue("one"));
            one(qr).getQuery();
            will(returnValue("two"));
            one(qr).getQuery();
            will(returnValue("three"));
            one(qr).getQuery();
            will(returnValue("four"));
            one(qr).getQuery();
            will(returnValue("five"));
            one(qr).getQuery();
            will(returnValue("six"));
            one(qr).getQuery();
            will(returnValue("seven"));
            one(qr).getQuery();
            will(returnValue("eight"));
            one(qr).getQuery();
            will(returnValue("nine"));
            one(qr).getQuery();
            will(returnValue("ten"));
            one(qr).getQuery();
            will(returnValue("one"));
            exactly(11).of(qr).getTTL();
            will(returnValue((byte)1));
            exactly(11).of(qr).isBrowseHostQuery();
            will(returnValue(false));
            exactly(11).of(qr).isWhatIsNewRequest();
            will(returnValue(false));
        }});
        for(int i = 0; i < 10; i++) {
            assertTrue(filter.allow(qr));
        }
        assertFalse(filter.allow(qr));
        context.assertIsSatisfied();
    }

    public void testRepetitionAllowedAfterEleven() {
        context.checking(new Expectations() {{
            one(qr).getQuery();
            will(returnValue("one"));
            one(qr).getQuery();
            will(returnValue("two"));
            one(qr).getQuery();
            will(returnValue("three"));
            one(qr).getQuery();
            will(returnValue("four"));
            one(qr).getQuery();
            will(returnValue("five"));
            one(qr).getQuery();
            will(returnValue("six"));
            one(qr).getQuery();
            will(returnValue("seven"));
            one(qr).getQuery();
            will(returnValue("eight"));
            one(qr).getQuery();
            will(returnValue("nine"));
            one(qr).getQuery();
            will(returnValue("ten"));
            one(qr).getQuery();
            will(returnValue("eleven"));
            one(qr).getQuery();
            will(returnValue("one"));
            exactly(12).of(qr).getTTL();
            will(returnValue((byte)1));
            exactly(12).of(qr).isBrowseHostQuery();
            will(returnValue(false));
            exactly(12).of(qr).isWhatIsNewRequest();
            will(returnValue(false));
        }});
        for(int i = 0; i < 12; i++) {
            assertTrue(filter.allow(qr));
        }
        context.assertIsSatisfied();
    }

    public void testRepetitionAllowedWithDifferentTTL() {
        context.checking(new Expectations() {{
            one(qr).getQuery();
            will(returnValue("one"));
            one(qr).getQuery();
            will(returnValue("two"));
            one(qr).getQuery();
            will(returnValue("three"));
            one(qr).getQuery();
            will(returnValue("four"));
            one(qr).getQuery();
            will(returnValue("five"));
            one(qr).getQuery();
            will(returnValue("six"));
            one(qr).getQuery();
            will(returnValue("seven"));
            one(qr).getQuery();
            will(returnValue("eight"));
            one(qr).getQuery();
            will(returnValue("nine"));
            one(qr).getQuery();
            will(returnValue("ten"));
            one(qr).getQuery();
            will(returnValue("one"));
            exactly(10).of(qr).getTTL();
            will(returnValue((byte)1));
            // The last query request will have a different TTL
            one(qr).getTTL();
            will(returnValue((byte)2));
            exactly(11).of(qr).isBrowseHostQuery();
            will(returnValue(false));
            exactly(11).of(qr).isWhatIsNewRequest();
            will(returnValue(false));
        }});
        for(int i = 0; i < 11; i++) {
            assertTrue(filter.allow(qr));
        }
        context.assertIsSatisfied();
    }
        
    public void testSettingAdjustsFilterSize() {
        FilterSettings.REPETITIVE_QUERY_FILTER_SIZE.setValue(5);
        filter = new RepetitiveQueryFilter();
        context.checking(new Expectations() {{
            one(qr).getQuery();
            will(returnValue("one"));
            one(qr).getQuery();
            will(returnValue("two"));
            one(qr).getQuery();
            will(returnValue("three"));
            one(qr).getQuery();
            will(returnValue("four"));
            one(qr).getQuery();
            will(returnValue("five"));
            one(qr).getQuery();
            will(returnValue("one"));
            exactly(6).of(qr).getTTL();
            will(returnValue((byte)1));
            exactly(6).of(qr).isBrowseHostQuery();
            will(returnValue(false));
            exactly(6).of(qr).isWhatIsNewRequest();
            will(returnValue(false));
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

    public void testBrowseHostQueriesAreAllowed() {
        context.checking(new Expectations() {{
            exactly(11).of(qr).isBrowseHostQuery();
            will(returnValue(true));
        }});
        for(int i = 0; i < 11; i++) {
            assertTrue(filter.allow(qr));
        }
        context.assertIsSatisfied();
    }

    public void testWhatIsNewQueriesAreAllowed() {
        context.checking(new Expectations() {{
            exactly(11).of(qr).isBrowseHostQuery();
            will(returnValue(false));
            exactly(11).of(qr).isWhatIsNewRequest();
            will(returnValue(true));
        }});
        for(int i = 0; i < 11; i++) {
            assertTrue(filter.allow(qr));
        }
        context.assertIsSatisfied();
    }

    public void testOtherMessagesAreAllowed() {
        PingRequest pr = context.mock(PingRequest.class);
        assertTrue(filter.allow(pr));
    }
}    
