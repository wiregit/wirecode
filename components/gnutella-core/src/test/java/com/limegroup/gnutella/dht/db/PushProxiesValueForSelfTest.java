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
            = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        
        ApplicationServices applicationServices = injector.getInstance(
                ApplicationServices.class);
        
        final PushEndpointFactory pushEndpointFactory 
            = context.mock(PushEndpointFactory.class);
        
        final PushEndpoint selfEndpoint 
            = context.mock(PushEndpoint.class);
        
        IpPort result = new ConnectableImpl(
                NetworkUtils.ip2string(
                    networkManagerStub.getAddress()), 
                    networkManagerStub.getPort(), 
                    networkManagerStub.isIncomingTLSEnabled());
        
        final IpPortSet expected 
            = new IpPortSet(result);
        
        context.checking(new Expectations() {{
            allowing(pushEndpointFactory).createForSelf();
            will(returnValue(selfEndpoint));
            
            allowing(selfEndpoint).getFeatures();
            will(returnValue((byte)0));
            
            allowing(selfEndpoint).getFWTVersion();
            will(returnValue(0));
            
            allowing(selfEndpoint).getPort();
            will(returnValue(0));
            
            allowing(selfEndpoint).getProxies();
            will(returnValue(expected));
        }});
        
        // *NOT* Firewalled !!!
        networkManagerStub.setAcceptedIncomingConnection(true);
        PushProxiesValue value2 = new DefaultPushProxiesValue(
                networkManagerStub, applicationServices, pushEndpointFactory);
        
        Set<? extends IpPort> proxies = value2.getPushProxies();
        assertEquals(1, proxies.size());
        assertContains(proxies, result);
        
        context.assertIsSatisfied();
        
        // Firewalled !!!
        networkManagerStub.setAcceptedIncomingConnection(false);
        
        // Replace the expected IpPorts
        expected.clear();
        expected.addAll(new IpPortSet(
                new ConnectableImpl("199.49.49.3", 45454, false), 
                new ConnectableImpl("202.2.23.3", 1000, true)));
        
        PushProxiesValue value3 = new DefaultPushProxiesValue(
                networkManagerStub, applicationServices, pushEndpointFactory);
        
        assertEquals(expected, value3.getPushProxies());
        
        context.assertIsSatisfied();
    }

}
