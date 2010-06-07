package com.limegroup.gnutella.filters.response;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.search.SearchResult;
import org.limewire.util.BaseTestCase;

public class CompoundResultFilterTest extends BaseTestCase {

    private Mockery context;
    private SearchResultFilter blackListFilter;
    private SearchResultFilter whiteListFilter;
    private CompoundFilter compoundFilter;

    public CompoundResultFilterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(CompoundResultFilterTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    public void testWhiteListFilterOverridesBlackListFilter() {
        
        blackListFilter = context.mock(SearchResultFilter.class);
        whiteListFilter = context.mock(SearchResultFilter.class);
        
        compoundFilter = new CompoundFilter(Collections.<ResponseFilter>emptyList(),
                Collections.<ResponseFilter>emptyList(),
                Collections.singleton(blackListFilter), 
                Collections.singleton(whiteListFilter));
        
        context.checking(new Expectations() {{
            one(blackListFilter).allow(null, null);
            will(returnValue(false));
            one(whiteListFilter).allow(null, null);
            will(returnValue(true));
        }});
        
        assertTrue(((SearchResultFilter)compoundFilter).allow(null, null));
        context.assertIsSatisfied();
    }

    public void testWhiteListFilterIsIgnoredIfBlackListFilterAllowsResponse() {
        
        blackListFilter = context.mock(SearchResultFilter.class);
        whiteListFilter = context.mock(SearchResultFilter.class);
        
        compoundFilter = new CompoundFilter(Collections.<ResponseFilter>emptyList(),
                Collections.<ResponseFilter>emptyList(),
                Collections.singleton(blackListFilter), 
                Collections.singleton(whiteListFilter));
        
        context.checking(new Expectations() {{
            one(blackListFilter).allow(null, null);
            will(returnValue(true));
            never(whiteListFilter).allow(null, null);
        }});
        
        assertTrue(((SearchResultFilter)compoundFilter).allow(null, null));
        context.assertIsSatisfied();
    }
    
    public void testMultipleBlackListFilters() {
        
        blackListFilter = context.mock(SearchResultFilter.class);
        whiteListFilter = context.mock(SearchResultFilter.class);
        
        final SearchResultFilter blackListFilter2 = context.mock(SearchResultFilter.class);
        final SearchResultFilter blackListFilter3 = context.mock(SearchResultFilter.class);
        
        compoundFilter = new CompoundFilter(Collections.<ResponseFilter>emptyList(),
                Collections.<ResponseFilter>emptyList(),
                Arrays.asList(blackListFilter, blackListFilter2, blackListFilter3), 
                Collections.<SearchResultFilter>emptyList());        
        
        context.checking(new Expectations() {{
            exactly(2).of(blackListFilter).allow(null, null);
            will(returnValue(true));
            
            exactly(2).of(blackListFilter2).allow(null, null);
            will(returnValue(true));
            
            one(blackListFilter3).allow(null, null);
            will(returnValue(true));
            
            one(blackListFilter3).allow(null, null);
            will(returnValue(false));
        }});
        
        assertTrue(((SearchResultFilter)compoundFilter).allow(null, null));
        assertFalse(((SearchResultFilter)compoundFilter).allow(null, null));
        
        context.assertIsSatisfied();
    }
    
    public void testMultipleBlackListFilterShortCircuit() {
        
        blackListFilter = context.mock(SearchResultFilter.class);
        whiteListFilter = context.mock(SearchResultFilter.class);
        
        final SearchResultFilter blackListFilter2 = context.mock(SearchResultFilter.class);
        final SearchResultFilter blackListFilter3 = context.mock(SearchResultFilter.class);
        
        compoundFilter = new CompoundFilter(Collections.<ResponseFilter>emptyList(),
                Collections.<ResponseFilter>emptyList(),
                Arrays.asList(blackListFilter, blackListFilter2, blackListFilter3), 
                Collections.<SearchResultFilter>emptyList());        
        
        context.checking(new Expectations() {{
            exactly(1).of(blackListFilter).allow(null, null);
            will(returnValue(false));
            
            never(blackListFilter2).allow(null, null);
            never(blackListFilter3).allow(null, null);
            
        }});
        
        assertFalse(((SearchResultFilter)compoundFilter).allow(null, null));
        
        context.assertIsSatisfied();
    }
    
    public void testMultipleBlackListFilterVariegated() {
        
        blackListFilter = context.mock(SearchResultFilter.class);
        whiteListFilter = context.mock(SearchResultFilter.class);
        
        final SearchResultFilter blackListFilter2 = context.mock(SearchResultFilter.class);
        final SearchResultFilter blackListFilter3 = context.mock(SearchResultFilter.class);
        
        final SearchResult result1 = context.mock(SearchResult.class);
        final SearchResult result2 = context.mock(SearchResult.class);
        
        compoundFilter = new CompoundFilter(Collections.<ResponseFilter>emptyList(),
                Collections.<ResponseFilter>emptyList(),
                Arrays.asList(blackListFilter, blackListFilter2, blackListFilter3), 
                Collections.<SearchResultFilter>emptyList());        
        
        context.checking(new Expectations() {{
            exactly(1).of(blackListFilter).allow(result1, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter2).allow(result1, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter3).allow(result1, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter).allow(result2, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter2).allow(result2, null);
            will(returnValue(true));
            
            exactly(1).of(blackListFilter3).allow(result2, null);
            will(returnValue(false));

            
        }});
        
        assertTrue(compoundFilter.allow(result1, null));
        assertFalse(compoundFilter.allow(result2, null));
        
        context.assertIsSatisfied();
    }
}
