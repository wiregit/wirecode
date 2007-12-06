package com.limegroup.gnutella.gui.search;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.GuiCoreMediator;

public class SearchMediatorTest extends GUIBaseTestCase {

    private PushEndpointFactory pushEndpointFactory;

    public SearchMediatorTest(String name) {
        super(name);
     }

    public static Test suite() { 
        return buildTestSuite(SearchMediatorTest.class); 
    } 
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                requestStaticInjection(GuiCoreMediator.class);
            }
        });
        pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
    }
    
    public void testDoBrowseHostPushEndpointNoAddress() throws Exception {
        // instantiate because the constructor sets some static fields in other classes
        new SearchMediator();
        String httpValue = "FFB3EC3B9D93A8F9CE42AED28F674900;222.222.222.222:2222";
        PushEndpoint pushEndpoint = pushEndpointFactory.createPushEndpoint(httpValue);
        SearchMediator.doBrowseHost(pushEndpoint);
    }

}
