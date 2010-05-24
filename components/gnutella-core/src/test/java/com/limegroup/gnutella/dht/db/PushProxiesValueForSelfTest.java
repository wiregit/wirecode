package com.limegroup.gnutella.dht.db;

import java.util.Set;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;

public class PushProxiesValueForSelfTest extends BaseTestCase {

    private Mockery context;

    private Injector injector;
    
    public PushProxiesValueForSelfTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushProxiesValueForSelfTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        
        final PushEndpointFactory pushEndpointFactory 
            = context.mock(PushEndpointFactory.class);
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).to(NetworkManagerStub.class);
                bind(PushEndpointFactory.class).toInstance(pushEndpointFactory);
            }            
        });
    }
    
    public void testGetPushProxiesReturnsSelf() throws Exception {
        NetworkManagerStub networkManagerStub 
            = (NetworkManagerStub)injector.getInstance(NetworkManager.class)
        ;
        final PushEndpointFactory pushEndpointFactory 
            = context.mock(PushEndpointFactory.class);
        final PushEndpoint selfEndpoint = context.mock(PushEndpoint.class);
        
        context.checking(new Expectations() {{
            allowing(pushEndpointFactory).createForSelf();
            will(returnValue(selfEndpoint));
        }});
        
        ApplicationServices applicationServices = injector.getInstance(
                ApplicationServices.class);
        
        PushProxiesValue value2 = new DefaultPushProxiesValue(
                networkManagerStub, applicationServices, pushEndpointFactory);
        
        networkManagerStub.setAcceptedIncomingConnection(true);
        
        Set<? extends IpPort> proxies = value2.getPushProxies();
        assertEquals(1, proxies.size());

        assertContains(proxies, new ConnectableImpl(
                NetworkUtils.ip2string(
                        networkManagerStub.getAddress()), 
                        networkManagerStub.getPort(), 
                        networkManagerStub.isIncomingTLSEnabled()));
        
        
        // let's go firewalled
        networkManagerStub.setAcceptedIncomingConnection(false);
        final IpPortSet pushProxies = new IpPortSet(
                new ConnectableImpl("199.49.49.3", 45454, false), 
                new ConnectableImpl("202.2.23.3", 1000, true));
        
        context.checking(new Expectations() {{
            one(selfEndpoint).getProxies();
            will(returnValue(pushProxies));
        }});
        
        assertEquals(pushProxies, value2.getPushProxies());
        context.assertIsSatisfied();
    }

}
