package com.limegroup.gnutella;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.bittorrent.dht.DHTPeerLocator;
import com.limegroup.bittorrent.dht.DHTPeerPublisher;

public class LifecycleManagerImplTest extends BaseTestCase {

    private LifecycleManager lifecycleManager;
    private Mockery mockery;
    private Module module;
    
    private DHTPeerLocator locator;
    private DHTPeerPublisher publisher;

    public LifecycleManagerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LifecycleManagerImplTest.class);
    }

    @Override
    public void setUp() throws Exception {
        mockery = new Mockery();
        locator = mockery.mock(DHTPeerLocator.class);
        publisher = mockery.mock(DHTPeerPublisher.class);
        module = new AbstractModule() {
            @Override
            public void configure() {
                bind(DHTPeerLocator.class).toInstance(locator);
                bind(DHTPeerPublisher.class).toInstance(publisher);                
            }
        };

        Injector inj = LimeTestUtils.createInjector(module);
        lifecycleManager = inj.getInstance(LifecycleManager.class);
    }

    //Tests to ensure the DHTPeerPublisher and DHTPeerLocator are getting initialized.
    public void testDoStartShouldInitializeDHTPeerPublisherAndDHTPeerLocator() {
        mockery.checking(new Expectations () {
            {
                one(locator).init();
                one(publisher).init();
            }
        });
       ((LifecycleManagerImpl)lifecycleManager).doStart();
       mockery.assertIsSatisfied();
    }
}
