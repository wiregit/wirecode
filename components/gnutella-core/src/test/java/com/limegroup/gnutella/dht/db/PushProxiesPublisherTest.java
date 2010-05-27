package com.limegroup.gnutella.dht.db;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.GUID;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.LimeWireIOTestModule;
import org.limewire.mojito.KUID;
import org.limewire.mojito.storage.DHTValue;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;

public class PushProxiesPublisherTest extends BaseTestCase {
    
    private Injector injector;
    
    public PushProxiesPublisherTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushProxiesPublisherTest.class);
    }

    @Override
    public void setUp() {
        DHTSettings.PUBLISH_PUSH_PROXIES.setValue(true);
        
        injector = LimeTestUtils.createInjectorNonEagerly(
                new LimeWireIOTestModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).to(NetworkManagerStub.class);
            }
        });
    }
    
    public void testSimplePublish() throws InterruptedException {
        PublisherQueue queue = new PublisherQueueStub();
        NetworkManager networkManager 
            = injector.getInstance(NetworkManager.class);
        ApplicationServices applicationServices
            = injector.getInstance(ApplicationServices.class);
        PushEndpointFactory pushEndpointFactory 
            = injector.getInstance(PushEndpointFactory.class);
        
        final CountDownLatch latch = new CountDownLatch(1);
        PushProxiesPublisher publisher 
            = new PushProxiesPublisher(queue, 
                    networkManager, applicationServices, 
                    pushEndpointFactory, 
                    5L, TimeUnit.MILLISECONDS) {
            @Override
            protected void publish(KUID key, DHTValue value) {
                latch.countDown();
            }
        };
        
        try {
            publisher.setStableProxiesTime(0, TimeUnit.MILLISECONDS);
            publisher.setPublishProxiesTime(0, TimeUnit.MILLISECONDS);
            
            publisher.start();
            
            if (!latch.await(5L, TimeUnit.SECONDS)) {
                fail("Shouldn't have failed!");
            }
        } finally {
            publisher.close();
        }
    }
    
    public void testHasChangedTooMuch() throws UnknownHostException {
        byte[] guid = GUID.makeGuid();
        byte features = 0;
        int fwtVersion = 0;
        int port = 6666;
        IpPortSet proxies = new IpPortSet(
            new IpPortImpl("www.limewire.com", 5555),
            new IpPortImpl("www.limewire.org", 6666),
            new IpPortImpl("www.google.com", 7777)
        );
        
        PushProxiesValue value1 = new DefaultPushProxiesValue(
                PushProxiesValue.VERSION, guid, features, 
                fwtVersion, port, proxies);
        
        // Compare to null -> true
        assertTrue(PushProxiesPublisher.hasChangedTooMuch(value1, null));
        
        // Compare to same configuration -> false
        PushProxiesValue value2 = new DefaultPushProxiesValue(
                PushProxiesValue.VERSION, guid, features, 
                fwtVersion, port, proxies);
        assertFalse(PushProxiesPublisher.hasChangedTooMuch(value1, value2));
        
        // Compare to a different fwtVersion
        PushProxiesValue value3 = new DefaultPushProxiesValue(
                PushProxiesValue.VERSION, guid, features, 
                1, port, proxies);
        assertTrue(PushProxiesPublisher.hasChangedTooMuch(value1, value3));
        
        // There is a threshold that says if less than X-number of differ
        // from the previous value then replace it. In this case we're changing
        // just one address and two remain the same. -> false
        IpPortSet proxies2 = new IpPortSet(
            new IpPortImpl("www.limewire.com", 5555),
            new IpPortImpl("www.limewire.org", 6666),
            new IpPortImpl("www.apple.com", 8888)
        );
        
        PushProxiesValue value4 = new DefaultPushProxiesValue(
                PushProxiesValue.VERSION, guid, features, 
                fwtVersion, port, proxies2);
        assertFalse(PushProxiesPublisher.hasChangedTooMuch(value1, value4));
        
        // Same as above but two addresses have changed. The value has
        // changed too much and it should therefore return -> true.
        IpPortSet proxies3 = new IpPortSet(
                new IpPortImpl("www.dell.com", 5555),
                new IpPortImpl("www.limewire.org", 6666),
                new IpPortImpl("www.apple.com", 8888)
            );
            
        PushProxiesValue value5 = new DefaultPushProxiesValue(
                PushProxiesValue.VERSION, guid, features, 
                fwtVersion, port, proxies3);
        assertTrue(PushProxiesPublisher.hasChangedTooMuch(value1, value5));
    }
}
