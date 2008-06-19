package org.limewire.swarm.http.gnutella;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.ProtocolException;
import org.apache.http.nio.IOControl;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

public class QueueControllerImplTest extends BaseTestCase {
    
    private Mockery mockery;
    private ScheduledExecutorService executorService;
    
    private QueueControllerImpl queueController;

    public QueueControllerImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(QueueControllerImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        mockery = new Mockery();
        executorService = mockery.mock(ScheduledExecutorService.class);
        
        queueController = new QueueControllerImpl(executorService);
    }
    
    @Override
    protected void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }
    
    public void testQueues() throws Exception {
        final IOControl ioctrl = mockery.mock(IOControl.class);
        mockery.checking(new Expectations() {{
            one(executorService).schedule(with(any(Runnable.class)),
                    with(equal(2L)), with(equal((TimeUnit.SECONDS))));
            one(ioctrl).suspendOutput();
        }});
        
        queueController.addToQueue("position=0,pollMin=1,pollMax=2", ioctrl);
    }
    
    public void testAssertionsAreOn() throws Exception {
        assertTrue("Assertions must be on for tests to mean anything!",
                    QueueControllerImpl.class.desiredAssertionStatus());
    }
    
    public void testBumpingAndReplacingAndRemoving() throws Exception {
        final IOControl[] ioctrls = new IOControl[5];
        final ScheduledFuture<?>[] scheduledFutures = new ScheduledFuture<?>[5];
        
        queueController.setMaxQueueCapacity(5);
        
        // Add the first 5. [named 0 through 4]
        for(int i = 0; i < 5; i++) {
            final IOControl ioctrl = mockery.mock(IOControl.class, "IOControl" + i);
            final ScheduledFuture<?> scheduledFuture = mockery.mock(ScheduledFuture.class, "ScheduledFuture" + i);
            ioctrls[i] = ioctrl;
            scheduledFutures[i] = scheduledFuture;
            mockery.checking(new Expectations() {{
                one(executorService).schedule(with(any(Runnable.class)),
                        with(equal(2L)), with(equal((TimeUnit.SECONDS))));
                    will(returnValue(scheduledFuture));
                one(ioctrl).suspendOutput();
            }});
            
            queueController.addToQueue("position=" + i + ",pollMin=1,pollMax=2", ioctrl);
        }
        
        // The next one should not be accepted.
        {
            final IOControl ioctrl5 = mockery.mock(IOControl.class, "IOControl5");
            mockery.checking(new Expectations() {{
                one(ioctrl5).shutdown();
            }});
            queueController.addToQueue("position=6,pollMin=1,pollMax=2", ioctrl5);
        }
        
        // And the next one should bump the fifth queued dude.
        final IOControl ioctrl6 = mockery.mock(IOControl.class, "IOControl6");
        final ScheduledFuture<?> scheduledFuture6 = mockery.mock(ScheduledFuture.class, "ScheduledFuture6");
        mockery.checking(new Expectations() {{
            one(scheduledFutures[4]).cancel(false);
            one(ioctrls[4]).shutdown();
            
            one(executorService).schedule(with(any(Runnable.class)),
                    with(equal(2L)), with(equal((TimeUnit.SECONDS))));
                will(returnValue(scheduledFuture6));
            one(ioctrl6).suspendOutput();
        }});
        queueController.addToQueue("position=0,pollMin=1,pollMax=2", ioctrl6);
        
        // Re-add the 3rd one, now in a lower position.
        final ScheduledFuture<?> scheduledFuture3 = mockery.mock(ScheduledFuture.class, "ScheduledFuture3Replacement");
        mockery.checking(new Expectations() {{
            one(scheduledFutures[3]).isDone();
                will(returnValue(true));
            one(executorService).schedule(with(any(Runnable.class)),
                    with(equal(2L)), with(equal((TimeUnit.SECONDS))));
                will(returnValue(scheduledFuture3));
            one(ioctrls[3]).suspendOutput();
        }});
        scheduledFutures[3] = scheduledFuture3;
        queueController.addToQueue("position=0,pollMin=1,pollMax=2", ioctrls[3]);
        
        // Now should bump the 3rd...
        final IOControl ioctrl7 = mockery.mock(IOControl.class, "IOControl7");
        final ScheduledFuture<?> scheduledFuture7 = mockery.mock(ScheduledFuture.class, "ScheduledFuture7");
        mockery.checking(new Expectations() {{
            one(scheduledFutures[2]).cancel(false);
            one(ioctrls[2]).shutdown();
            
            one(executorService).schedule(with(any(Runnable.class)),
                    with(equal(2L)), with(equal((TimeUnit.SECONDS))));
                will(returnValue(scheduledFuture7));
            one(ioctrl7).suspendOutput();
        }});
        QueueInfo q8 = queueController.addToQueue("position=0,pollMin=1,pollMax=2", ioctrl7);
        
        // Remove a dude...
        mockery.checking(new Expectations() {{
            one(scheduledFuture7).cancel(false);
        }});
        queueController.removeFromQueue(q8);
        
        // Add it back, good as new -- and w/o bumping anything.
        mockery.checking(new Expectations() {{
            one(executorService).schedule(with(any(Runnable.class)),
                    with(equal(2L)), with(equal((TimeUnit.SECONDS))));
                will(returnValue(scheduledFuture7));
            one(ioctrl7).suspendOutput();
        }});
        queueController.addToQueue("position=0,pollMin=1,pollMax=2", ioctrl7);        
    }
    
    public void testEnqueueSuspendsAndDequeueRequests() throws Exception {
        final IOControl ioctrl = mockery.mock(IOControl.class);
        mockery.checking(new Expectations() {{
            one(executorService).schedule(with(any(Runnable.class)),
                    with(equal(2L)), with(equal((TimeUnit.SECONDS))));
            one(ioctrl).suspendOutput();
        }});        
        QueueInfo q = queueController.addToQueue("position=0,pollMin=1,pollMax=2", ioctrl);        
        assertTrue(q.isQueued());
        
        mockery.checking(new Expectations() {{
            one(ioctrl).suspendOutput();
        }});
        q.enqueue();
        assertTrue(q.isQueued());
        
        mockery.checking(new Expectations() {{
            one(ioctrl).requestOutput();
        }});
        q.dequeue();
        assertFalse(q.isQueued());
        
        // Does nothing!
        q.enqueue();
        assertFalse(q.isQueued());
    }
    
    public void testRunnableDequeues() throws Exception {
        final IOControl ioctrl = mockery.mock(IOControl.class);
        final RunnerCatcher runnerCatcher = new RunnerCatcher();
        mockery.checking(new Expectations() {{
            one(executorService).schedule(with(runnerCatcher),
                    with(equal(2L)), with(equal((TimeUnit.SECONDS))));
            one(ioctrl).suspendOutput();
        }});        
        QueueInfo q = queueController.addToQueue("position=0,pollMin=1,pollMax=2", ioctrl);        
        assertTrue(q.isQueued());
        
        mockery.checking(new Expectations() {{
            one(ioctrl).requestOutput();
        }});
        runnerCatcher.runnable.run();
        assertFalse(q.isQueued());
        
        // Does nothing!
        q.enqueue();
    }
    
    public void testSubmitsWithCorrectTime() throws Exception {
        {
            final IOControl ioctrl = mockery.mock(IOControl.class);
            mockery.checking(new Expectations() {{
                one(executorService).schedule(with(any(Runnable.class)),
                        with(equal(1L)), with(equal((TimeUnit.SECONDS))));
                one(ioctrl).suspendOutput();
            }});        
            queueController.addToQueue("position=0,pollMin=0,pollMax=5", ioctrl);
        }
        
        {
            final IOControl ioctrl = mockery.mock(IOControl.class);
            mockery.checking(new Expectations() {{
                one(executorService).schedule(with(any(Runnable.class)),
                        with(equal(5L)), with(equal((TimeUnit.SECONDS))));
                one(ioctrl).suspendOutput();
            }});        
            queueController.addToQueue("position=0,pollMin=6,pollMax=5", ioctrl);
        }
        
        {
            final IOControl ioctrl = mockery.mock(IOControl.class);
            mockery.checking(new Expectations() {{
                one(executorService).schedule(with(any(Runnable.class)),
                        with(equal(4L)), with(equal((TimeUnit.SECONDS))));
                one(ioctrl).suspendOutput();
            }});        
            queueController.addToQueue("position=0,pollMin=3,pollMax=5", ioctrl);
        }
    }
    
    public void testProtocolExceptions() throws Exception {
        try {
            queueController.addToQueue("position=0,pollMin=3", null);
            fail();
        } catch(ProtocolException expected) {}
        
        try {
            queueController.addToQueue("position=0,pollMax=3", null);
            fail();
        } catch(ProtocolException expected) {}
        
        try {
            queueController.addToQueue("pollMax=0,pollMin=3", null);
            fail();
        } catch(ProtocolException expected) {}
        
        try {
            queueController.addToQueue("position=0,pollMin=3,pollMax=", null);
            fail();
        } catch(ProtocolException expected) {}
        
        try {
            queueController.addToQueue("position=0,pollMin=3,pollMax", null);
            fail();
        } catch(ProtocolException expected) {}
        
        try {
            queueController.addToQueue("position=0,pollMin=3,pollMax=3.5", null);
            fail();
        } catch(ProtocolException expected) {}
        
        try {
            queueController.addToQueue("position=0,pollMin=3,pollMax=A", null);
            fail();
        } catch(ProtocolException expected) {}
    }
    
    public void testOtherParametersIgnored() throws Exception {
        final IOControl ioctrl = mockery.mock(IOControl.class);
        mockery.checking(new Expectations() {{
            one(executorService).schedule(with(any(Runnable.class)),
                    with(equal(1L)), with(equal((TimeUnit.SECONDS))));
            one(ioctrl).suspendOutput();
        }});        
        queueController.addToQueue("garabeIn=A,position=0,pollMin=0,pollMax=5,gargabeOut=ZZZ", ioctrl);
    }
    
    private static class RunnerCatcher extends TypeSafeMatcher<Runnable> {
        private Runnable runnable;
        
        public void describeTo(Description description) {
            description.appendText("RunnerCatcher");
        }
        
        @Override
        public boolean matchesSafely(Runnable item) {
            this.runnable = item;
            return true;
        }
    }

}
