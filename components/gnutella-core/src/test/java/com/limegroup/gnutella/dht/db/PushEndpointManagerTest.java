package com.limegroup.gnutella.dht.db;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;

public class PushEndpointManagerTest extends BaseTestCase {

    private Mockery context;
    private PushEndpointCache pushEndpointCache;
    private PushEndpointService pushEndpointFinder;
    private SearchListener<PushEndpoint> listener;
    private PushEndpoint result;
    private PushEndpointManager pushEndpointManager;

    public PushEndpointManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushEndpointManagerTest.class);
    }
    
    @SuppressWarnings({ "cast", "unchecked" })
    protected void setUp() throws Exception {
        context = new Mockery();
        
        // the players
        pushEndpointCache = context.mock(PushEndpointCache.class);
        pushEndpointFinder = context.mock(PushEndpointService.class);
        listener = (SearchListener<PushEndpoint>)context.mock(SearchListener.class);
        result = context.mock(PushEndpoint.class);
        pushEndpointManager = new PushEndpointManager(pushEndpointCache, pushEndpointFinder);
    }
    
    @SuppressWarnings({ "cast", "unchecked" })
    public void testNoSearchIfPushEndpointInCache() throws Exception {
        final GUID guid = new GUID();
        final IpPortSet proxies = new IpPortSet(new IpPortImpl("192.168.1.1:4545"));
        
        context.checking(new Expectations() {{
            one(pushEndpointCache).getPushEndpoint(guid);
            will(returnValue(result));
            one(result).getProxies();
            will(returnValue(proxies));
            one(listener).handleResult(result);
            one(listener).handleSearchDone(true);
            never(pushEndpointFinder).findPushEndpoint(guid, listener);
        }});
        
        pushEndpointManager.findPushEndpoint(guid, listener);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Cache is empty and two searches are started right away, second one
     * should fail immediately and not ask trigger a search.
     */
    @SuppressWarnings("unchecked")
    public void testTimeoutBetweenSearchesIsHonored() throws Exception {
        final GUID guid = new GUID();
        
        final Sequence sequence = context.sequence("sequence");
        
        context.checking(new Expectations() {{
            one(pushEndpointCache).getPushEndpoint(guid);
            will(returnValue(null));
            inSequence(sequence);
            one(pushEndpointFinder).findPushEndpoint(with(same(guid)), with(any(SearchListener.class)));
            inSequence(sequence);
            one(pushEndpointCache).getPushEndpoint(guid);
            will(returnValue(null));
            inSequence(sequence);
            one(listener).handleSearchDone(false);
            inSequence(sequence);
            // zero calls to the finder here
            
            // then after the timeout:
            one(pushEndpointCache).getPushEndpoint(guid);
            will(returnValue(null));
            inSequence(sequence);
            one(pushEndpointFinder).findPushEndpoint(with(same(guid)), with(any(SearchListener.class)));
            inSequence(sequence);
        }});
        
        pushEndpointManager.setTimeBetweenSearches(1000L);
        
        // does search
        pushEndpointManager.findPushEndpoint(guid, listener);
        // does not search
        pushEndpointManager.findPushEndpoint(guid, listener);
        
        Thread.sleep(1000L);
        
        // does search again
        pushEndpointManager.findPushEndpoint(guid, listener);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Asserts that {@link PushEndpointManager#startSearch(com.limegroup.gnutella.GUID, SearchListener)}
     * starts a search and notifies the cache of results.
     */
    @SuppressWarnings("unchecked")
    public void testSearchIsStartedAndCacheIsNotified() {
        final GUID guid = new GUID();
        
        context.checking(new Expectations() {{
            one(pushEndpointFinder).findPushEndpoint(with(same(guid)), with(any(SearchListener.class)));
            will(new CustomAction("call listener") {
                public Object invoke(Invocation invocation) throws Throwable {
                    ((SearchListener<PushEndpoint>)invocation.getParameter(1)).handleResult(result);
                    ((SearchListener<PushEndpoint>)invocation.getParameter(1)).handleSearchDone(true);
                    return null;
                }
            });
            one(listener).handleResult(result);
            one(listener).handleSearchDone(true);
            one(result).updateProxies(true);
        }});
        
        pushEndpointManager.startSearch(guid, listener);
        
        context.assertIsSatisfied();
    }

}
