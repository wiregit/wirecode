package com.limegroup.gnutella.filters;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;

public class HashFilterTest extends BaseTestCase {
    
    public HashFilterTest(String name){
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HashFilterTest.class);
    }
    
    QueryRequest query;
    PingRequest ping;
    HashFilter filter;
    Mockery context;
    
    @Override
    public void setUp() throws Exception {
        context = new Mockery();
        query = context.mock(QueryRequest.class);
        ping = context.mock(PingRequest.class); 
        filter = new HashFilter();
    }
    
    public void testHasUrn() {
        context.checking(new Expectations() {{
            one(query).hasQueryUrns();
            will(returnValue(true));
        }});
        assertFalse(filter.allow(query));
        context.assertIsSatisfied();
    }
        
    public void testHasNoUrn() {
        context.checking(new Expectations() {{
            one(query).hasQueryUrns();
            will(returnValue(false));
        }});
        assertTrue(filter.allow(query));
        context.assertIsSatisfied();
    }
    
    public void testOtherMessagesAreIgnored() throws Exception {
        context.checking(new Expectations() {{
            never(ping);
        }});        
        assertTrue(filter.allow(ping));
        context.assertIsSatisfied();
    }
}
