package org.limewire.net;

import junit.framework.Test;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class WhoIsRequestTest extends BaseTestCase {

    public WhoIsRequestFactory factory;
    
    public WhoIsRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(WhoIsRequestTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
        
    @Override
    protected void setUp() throws Exception {
        Injector injector = Guice.createInjector(new LimeWireNetModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(ProxySettings.class).to(EmptyProxySettings.class);
                bind(SocketBindingSettings.class).to(EmptySocketBindingSettings.class);
                bind(NetworkInstanceUtils.class).to(SimpleNetworkInstanceUtils.class);
            }
        });        
        
        factory = injector.getInstance(WhoIsRequestFactory.class); 
    }
    
    public void testName() throws Exception {
        factory.createWhoIsRequest("google.com").doRequest();
    }
    
    public void testAddr() throws Exception {
        WhoIsRequest request = factory.createWhoIsRequest("64.233.187.99");
        
        request.doRequest();
        
        assertNotNull(request.getNetRange());
    }
    
}
