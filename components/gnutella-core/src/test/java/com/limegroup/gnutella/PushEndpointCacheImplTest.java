package com.limegroup.gnutella;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.util.BaseTestCase;

public class PushEndpointCacheImplTest extends BaseTestCase {

    private PushEndpointCacheImpl pushEndpointCacheImpl;
    private Mockery context;
    private ScheduledExecutorService executorService;
    private Runnable weakCleaner;

    public PushEndpointCacheImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushEndpointCacheImplTest.class);
    }    
   

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        executorService = context.mock(ScheduledExecutorService.class);

        context.checking(new Expectations() {{
            allowing(executorService).scheduleWithFixedDelay(with(any(Runnable.class)), 
                    with(any(Long.class)), with(any(Long.class)), with(any(TimeUnit.class)));
            will(new CustomAction("savefield") {
                public Object invoke(Invocation invocation) throws Throwable {
                    weakCleaner = (Runnable) invocation.getParameter(0);
                    return null;
                }
            });
        }});
        
        pushEndpointCacheImpl = new PushEndpointCacheImpl(executorService);
    }

    public void testCachedEntriesAreExpunged() throws Exception {
        assertNotNull(weakCleaner);
        
        Set<IpPort> proxies = Collections.emptySet();
        GUID guid = new GUID();
        PushEndpoint pushEndpoint = new PushEndpointImpl(guid.bytes(), proxies, (byte)0, 1, new IpPortImpl("127.0.0.1:6666"), pushEndpointCacheImpl);
        
        assertNull(pushEndpointCacheImpl.getCached(guid));
        assertNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
        pushEndpoint.updateProxies(true);
        
        assertNotNull(pushEndpointCacheImpl.getCached(guid));
        assertNotNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
        System.gc();
        
        weakCleaner.run();
        
        assertNotNull(pushEndpointCacheImpl.getCached(guid));
        assertNotNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
        // now lose instance
        pushEndpoint = null;
        
        System.gc();
        
        weakCleaner.run();
        
        assertNull(pushEndpointCacheImpl.getCached(guid));
        assertNull(pushEndpointCacheImpl.getPushEndpoint(guid));
        
    }

}
