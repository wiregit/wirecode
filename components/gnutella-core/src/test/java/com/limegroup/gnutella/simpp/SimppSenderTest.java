package com.limegroup.gnutella.simpp;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inject.Providers;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.SimppVM;

public class SimppSenderTest extends LimeTestCase {

    private Mockery context;
    private SimppRequestVM simppRequest;
    private SimppManager simppManager;
    private SimppSender simppSender;
    private ScheduledExecutorService scheduledExecutorService;
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        simppRequest = new SimppRequestVM();
        simppManager = context.mock(SimppManager.class);
        scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        
        simppSender = new SimppSender(Providers.of(simppManager), scheduledExecutorService);
        
        context.checking(new Expectations() {{
            allowing(simppManager).getSimppBytes();
            will(returnValue(new byte[] { 1 }));
            allowing(simppManager).getVersion();
            will(returnValue(5));
            atLeast(1).of(scheduledExecutorService).schedule(with(any(Runnable.class)), with(any(Integer.class)), with(equal(TimeUnit.MILLISECONDS)));
        }});
    }
    
    public void testEnqueueSimppRequest() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
    }
    
    public void testSendSimppToNextInQueue() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            one(handler2).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.sendToNextInQueue();
        assertFalse(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
    }
    
    public void testSendSimppToNextInQueueIfCurrent() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            one(handler2).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.sendToNextInQueueIfCurrent(handler1);
        assertFalse(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
    }
    
    public void testSendSimppToNextInQueueIfCurrentNotCurrent() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        final ReplyHandler handler3 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
        
        simppSender.sendToNextInQueueIfCurrent(handler3);
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            one(handler2).handleSimppVM(with(any(SimppVM.class)));
        }});

        // calling it with current one
        simppSender.sendToNextInQueueIfCurrent(handler1);
        
        assertFalse(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
    }
    
    public void testReplyRemoveHandlerFromQueue() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
        
        simppSender.removeReplyHandlerFromQueue(handler2);

        assertFalse(simppSender.queueContains(handler2));
        
        // should not do anything
        simppSender.sendToNextInQueue();
    }
    
    public void testReplyHandlerRemoveHandlerFromQueueDoesNotAffectCurrentOne() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();

        // has no effect
        simppSender.removeReplyHandlerFromQueue(handler1);
        
        context.checking(new Expectations() {{
            one(handler2).handleSimppVM(with(any(SimppVM.class)));
        }});
        // should should trigger send to handler two
        simppSender.removeStaleReplyHandler(handler1);
        
        context.assertIsSatisfied();
    }
    
    public void testRemoveCurrentStaleReplyHandler() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            one(handler2).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        // removing the current one should cause simpp to be sent to the next one
        simppSender.removeStaleReplyHandler(handler1);
        
        assertFalse(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
    }
    
    public void removeInQueueStaleReplyHandler() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
        
        // removing the current one should cause simpp to be sent to the next one
        simppSender.removeStaleReplyHandler(handler2);
        simppSender.sendToNextInQueue();
        
        assertFalse(simppSender.queueContains(handler2));
        context.assertIsSatisfied();
    }
    
    public void testDuplicateSimppRequestIsNotEnqueued() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
            one(handler2).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        
        // this should have no effect
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        
        simppSender.sendToNextInQueue();
        
        assertFalse(simppSender.queueContains(handler2));
        // this should do nothing
        simppSender.sendToNextInQueue();

        // checks that simpp was only sent once to handler2
        context.assertIsSatisfied();
    }
    
    public void testQueueIsEmptiedIfSimppMessageIsEmpty() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        final ReplyHandler handler3 = context.mock(ReplyHandler.class);
        final SimppManager simppManager = context.mock(SimppManager.class);
        
        context.checking(new Expectations() {{
            // stubbing
            one(simppManager).getSimppBytes();
            will(returnValue(new byte[] { 1 }));
            allowing(simppManager).getSimppBytes();
            will(returnValue(new byte[0]));
            allowing(simppManager).getVersion();
            will(returnValue(5));
            
            // expectations, no messages are sent
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
            never(handler2).handleSimppVM(with(any(SimppVM.class)));
            never(handler3).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender = new SimppSender(Providers.of(simppManager), scheduledExecutorService);
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        simppSender.enqueueSimppRequest(simppRequest, handler3);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        assertTrue(simppSender.queueContains(handler3));
        
        simppSender.sendToNextInQueue();
        assertFalse(simppSender.queueContains(handler2));
        assertFalse(simppSender.queueContains(handler3));
        
        context.assertIsSatisfied();
    }
    
    public void testSendToNextIfCurrentDoesNotRemoveFromQueue() throws Exception {
        final ReplyHandler handler1 = context.mock(ReplyHandler.class);
        final ReplyHandler handler2 = context.mock(ReplyHandler.class);
        final ReplyHandler handler3 = context.mock(ReplyHandler.class);
        
        context.checking(new Expectations() {{
            one(handler1).handleSimppVM(with(any(SimppVM.class)));
            one(handler2).handleSimppVM(with(any(SimppVM.class)));
            never(handler3).handleSimppVM(with(any(SimppVM.class)));
        }});
        
        simppSender.enqueueSimppRequest(simppRequest, handler1);
        simppSender.enqueueSimppRequest(simppRequest, handler2);
        simppSender.enqueueSimppRequest(simppRequest, handler3);
        
        assertFalse(simppSender.queueContains(handler1));
        assertTrue(simppSender.queueContains(handler2));
        assertTrue(simppSender.queueContains(handler3));
        // send to handler2
        simppSender.sendToNextInQueue();
        assertFalse(simppSender.queueContains(handler2));
        
        // signal message sent or timeout
        simppSender.sendToNextInQueueIfCurrent(handler3);
        // handler 3 should still be in queue
        assertTrue(simppSender.queueContains(handler3));
        
        context.assertIsSatisfied();
    }
    
    
}
