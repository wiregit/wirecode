package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.util.BaseTestCase;

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
import com.limegroup.gnutella.messages.vendor.InspectionResponseFactory;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;

public class InspectionRequestHandlerTest extends BaseTestCase {

    public InspectionRequestHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(InspectionRequestHandlerTest.class);
    }
    
    private Mockery mockery;
    private Injector injector;
    private MessageRouter messageRouter;
    private SecureMessageVerifier verifier;
    private InspectionResponseFactory factory;
    
    public void setUp() throws Exception {
        mockery = new Mockery();
        messageRouter = mockery.mock(MessageRouter.class);
        verifier = mockery.mock(SecureMessageVerifier.class);
        factory = mockery.mock(InspectionResponseFactory.class);
        Module m = new AbstractModule() {
            public void configure() {
                bind(MessageRouter.class).toInstance(messageRouter);
                bind(InspectionResponseFactory.class).toInstance(factory);
                bind(SecureMessageVerifier.class).annotatedWith(Names.named("inspection")).toInstance(verifier);
            }
        };
        injector = LimeTestUtils.createInjector(m);
    }

    public void testForwardsToLeaves() throws Exception {
        FilterSettings.INSPECTOR_IP_ADDRESSES.setValue(new String[]{"1.2.3.4"});
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        final ReplyHandler handler = mockery.mock(ReplyHandler.class);
        final InspectionResponse resp = new InspectionResponse(1, new byte[16], new byte[0]);
        mockery.checking(new Expectations() {{
            one(handler).getAddress();
            will(returnValue("1.2.3.4"));
            one(messageRouter).forwardInspectionRequestToLeaves(request);
            one(factory).createResponses(request);
            will(returnValue(new InspectionResponse[]{resp}));
        }});
        InspectionRequestHandler irh = injector.getInstance(InspectionRequestHandler.class);
        irh.handleMessage(request, new InetSocketAddress("1.2.3.4",1), handler);
        mockery.assertIsSatisfied();
    }
    
    public void testSendsDelayed() throws Exception {
        FilterSettings.INSPECTOR_IP_ADDRESSES.setValue(new String[]{"1.2.3.4"});
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        final TestReplyHandler replyHandler = new TestReplyHandler();
        final InspectionResponse [] responses = new InspectionResponse[5];
        for (int i = 0; i < responses.length; i++)
            responses[i] = new TestInspectionResponse();
        
        mockery.checking(new Expectations() {{
            one(messageRouter).forwardInspectionRequestToLeaves(request);
            one(factory).createResponses(request);
            will(returnValue(responses));
        }});
        InspectionRequestHandler irh = injector.getInstance(InspectionRequestHandler.class);
        irh.handleMessage(request, new InetSocketAddress("1.2.3.4",1), replyHandler);
        mockery.assertIsSatisfied();
        
        Thread.sleep(responses.length * 600);
        synchronized(replyHandler) {
            assertEquals(responses.length, replyHandler.timestamps.size());
            assertEquals(responses.length, replyHandler.messages.size());
            
            for (int i = 0 ; i < responses.length ; i++)
                assertSame(responses[i],replyHandler.messages.get(i));
            
            long last = replyHandler.timestamps.get(0);
            for(int i = 1; i < responses.length; i++) {
                long current = replyHandler.timestamps.get(i);
                assertLessThanOrEquals(550, current - last);
                assertGreaterThanOrEquals(450, current - last);
                last = current;
            }
        }
    }
    
    private static class TestReplyHandler extends ReplyHandlerStub {
        final List<Long> timestamps = new ArrayList<Long>();
        final List<Message> messages = new ArrayList<Message>();
        @Override
        public String getAddress() {
            return "1.2.3.4";
        }
        
        @Override
        public synchronized void reply(Message m) {
            timestamps.add(System.currentTimeMillis());
            messages.add(m);
        }
    }
    
    private static class TestInspectionResponse extends InspectionResponse {
        TestInspectionResponse() {
            super(1, new byte[16], new byte[0]);
        }
        
        @Override
        public boolean shouldBeSent() {
            return true;
        }
    }
}
