package com.limegroup.gnutella.messagehandlers;

import java.io.File;
import java.net.InetSocketAddress;

import junit.framework.Test;

import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.inspection.Inspector;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.util.BaseTestCase;
import org.limewire.util.CommonUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.InspectionResponse;
import com.limegroup.gnutella.settings.FilterSettings;

public class InspectionRequestHandlerTest extends BaseTestCase {

    public InspectionRequestHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(InspectionRequestHandlerTest.class);
    }
    
    private Mockery mockery;
    private Inspector inspector;
    private Injector injector;
    private MessageRouter messageRouter;
    private SecureMessageVerifier verifier;
    
    public void setUp() throws Exception {
        mockery = new Mockery();
        inspector = mockery.mock(Inspector.class);
        messageRouter = mockery.mock(MessageRouter.class);
        verifier = mockery.mock(SecureMessageVerifier.class);
        Module m = new AbstractModule() {
            public void configure() {
                bind(Inspector.class).toInstance(inspector);
                bind(MessageRouter.class).toInstance(messageRouter);
                bind(SecureMessageVerifier.class).annotatedWith(Names.named("inspection")).toInstance(verifier);
            }
        };
        injector = LimeTestUtils.createInjector(m);
    }

    @SuppressWarnings("unused")
    public void testLoadsProperties() throws Exception {
        final File props = new File(CommonUtils.getCurrentDirectory(),"inspection.props");
        mockery.checking(new Expectations() {{
            one(inspector).load(props);
        }});
        InspectionRequestHandler irh = injector.getInstance(InspectionRequestHandler.class);
        mockery.assertIsSatisfied();
    }
    
    public void testQueriesInspector() throws Exception {
        FilterSettings.INSPECTOR_IP_ADDRESSES.setValue(new String[]{"1.2.3.4"});
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        final ReplyHandler handler = mockery.mock(ReplyHandler.class);
        mockery.checking(new Expectations() {{
            allowing(inspector).load((File)with(Matchers.instanceOf(File.class)));
            one(handler).getAddress();
            will(returnValue("1.2.3.4"));
            one(request).getRequestedFields();
            will(returnValue(new String[]{"asdf"}));
            one(request).requestsTimeStamp();
            will(returnValue(Boolean.TRUE));
            allowing(request).getGUID();
            one(inspector).inspect("asdf");
            one(handler).reply((Message)with(Matchers.instanceOf(InspectionResponse.class)));
            one(messageRouter).forwardInspectionRequestToLeaves(request);
        }});
        InspectionRequestHandler irh = injector.getInstance(InspectionRequestHandler.class);
        irh.handleMessage(request, new InetSocketAddress("1.2.3.4",1), handler);
        mockery.assertIsSatisfied();
    }
}
