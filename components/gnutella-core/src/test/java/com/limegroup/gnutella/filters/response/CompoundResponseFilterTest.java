package com.limegroup.gnutella.filters.response;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.QueryReply;

public class CompoundResponseFilterTest extends BaseTestCase {

    private Mockery context;
    private ResponseFilter blackListFilter;
    private ResponseFilter whiteListFilter;
    private CompoundFilter compoundFilter;

    public CompoundResponseFilterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(CompoundResponseFilterTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    public void testWhiteListFilterOverridesBlackListFilter() {
        blackListFilter = context.mock(ResponseFilter.class);
        whiteListFilter = context.mock(ResponseFilter.class);
        
        compoundFilter = new CompoundFilter(Collections.singleton(blackListFilter), 
                Collections.singleton(whiteListFilter),
                Collections.<ResultFilter>emptyList(),
                Collections.<ResultFilter>emptyList());
        
        context.checking(new Expectations() {{
            one(blackListFilter).allow(null, null);
            will(returnValue(false));
            one(whiteListFilter).allow(null, null);
            will(returnValue(true));
        }});
        
        assertTrue(((ResponseFilter)compoundFilter).allow(null, null));
        context.assertIsSatisfied();
    }

    public void testWhiteListFilterIsIgnoredIfBlackListFilterAllowsResponse() {
        blackListFilter = context.mock(ResponseFilter.class);
        whiteListFilter = context.mock(ResponseFilter.class);
        
        compoundFilter = new CompoundFilter(Collections.singleton(blackListFilter), 
                Collections.singleton(whiteListFilter),
                Collections.<ResultFilter>emptyList(),
                Collections.<ResultFilter>emptyList());
        
        context.checking(new Expectations() {{
            one(blackListFilter).allow(null, null);
            will(returnValue(true));
            never(whiteListFilter).allow(null, null);
        }});
        
        assertTrue(((ResponseFilter)compoundFilter).allow(null, null));
        context.assertIsSatisfied();
    }
    
    public void testMultipleBlackListFilterShortCircuit() {
        
        blackListFilter = context.mock(ResponseFilter.class);
        whiteListFilter = context.mock(ResponseFilter.class);
        
        final ResponseFilter blackListFilter2 = context.mock(ResponseFilter.class);
        final ResponseFilter blackListFilter3 = context.mock(ResponseFilter.class);
        
        compoundFilter = new CompoundFilter(Arrays.asList(blackListFilter, blackListFilter2, blackListFilter3),
                Collections.<ResponseFilter>emptyList(),
                Collections.<ResultFilter>emptyList(), 
                Collections.<ResultFilter>emptyList());        
        
        context.checking(new Expectations() {{
            exactly(1).of(blackListFilter).allow(null, null);
            will(returnValue(false));
            
            never(blackListFilter2).allow(null, null);
            never(blackListFilter3).allow(null, null);
            
        }});
        
        assertFalse(((ResponseFilter)compoundFilter).allow(null, null));
        
        context.assertIsSatisfied();
    }
    
    public void testMultipleBlackListFilterVariegated() {
        
        blackListFilter = context.mock(ResponseFilter.class);
        whiteListFilter = context.mock(ResponseFilter.class);
        
        final ResponseFilter blackListFilter2 = context.mock(ResponseFilter.class);
        final ResponseFilter blackListFilter3 = context.mock(ResponseFilter.class);
        
        final QueryReply reply1 = context.mock(QueryReply.class);
        final QueryReply reply2 = context.mock(QueryReply.class);
        
        compoundFilter = new CompoundFilter(Arrays.asList(blackListFilter, blackListFilter2, blackListFilter3),
                Collections.<ResponseFilter>emptyList(),
                Collections.<ResultFilter>emptyList(), 
                Collections.<ResultFilter>emptyList());     
        
        context.checking(new Expectations() {{
            exactly(1).of(blackListFilter).allow(reply1, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter2).allow(reply1, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter3).allow(reply1, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter).allow(reply2, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter2).allow(reply2, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter3).allow(reply2, null);
            will(returnValue(false));

            
        }});
        
        assertTrue(compoundFilter.allow(reply1, null));
        assertFalse(compoundFilter.allow(reply2, null));
        
        context.assertIsSatisfied();
    }
}
