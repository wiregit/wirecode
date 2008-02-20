package com.limegroup.gnutella.dht.db;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;

public class PushEndpointManagerTest extends BaseTestCase {

    private Mockery context;

    public PushEndpointManagerTest(String name) {
        super(name);
    }

    
    public static Test suite() {
        return buildTestSuite(PushEndpointManagerTest.class);
    }
    
    protected void setUp() throws Exception {
        context = new Mockery();
    }
  
    /**
     * Asserts that {@link PushEndpointManager#startSearch(com.limegroup.gnutella.GUID, SearchListener)}
     * starts a search and notifies the cache of results.
     */
    @SuppressWarnings({ "cast", "unchecked" })
    public void testSearchIsStartedAndCacheIsNotified() {
        PushEndpointCache pushEndpointCache = context.mock(PushEndpointCache.class);
        final PushEndpointService pushEndpointFinder = context.mock(PushEndpointService.class);
        final SearchListener<PushEndpoint> listener = (SearchListener<PushEndpoint>)context.mock(SearchListener.class);
        final PushEndpoint result = context.mock(PushEndpoint.class);
        
        final PushEndpointManager pushEndpointManager = new PushEndpointManager(pushEndpointCache, pushEndpointFinder);
        
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
