package org.limewire.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.util.BaseTestCase;

public class AsyncFutureTest extends BaseTestCase {

    private static final Runnable DO_NOTHING = new Runnable() {
        @Override
        public void run() {
            // Do nothing!
        }
    };
    
    private static final Runnable THROW_EXCEPTION = new Runnable() {
        @Override
        public void run() {
            throw new IllegalStateException("Expected!");
        }
    };
    
    public AsyncFutureTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AsyncFutureTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Check the pre- and post-state of {@link AsyncFutureTask}s if the 
     * underlying {@link Runnable} is throwing *NO* {@link Exception}.
     */
    public void testPreAndPostState() throws InterruptedException, ExecutionException {
        AsyncFutureTask<String> future 
            = new AsyncFutureTask<String>(DO_NOTHING, "Hello World!");
        
        assertFalse("Done", future.isDone());
        assertFalse("Cancelled", future.isCancelled());
        assertFalse("Completed Abnormally", future.isCompletedAbnormally());
        
        future.run();
        
        assertTrue("Done", future.isDone());
        assertFalse("Cancelled", future.isCancelled());
        assertFalse("Completed Abnormally", future.isCompletedAbnormally());
        
        assertEquals("Unexpected Result", "Hello World!", future.get());
    }
    
    /**
     * Check the pre- and post-state of {@link AsyncFutureTask}s if the 
     * underlying {@link Runnable} *IS* throwing an {@link Exception}.
     */
    public void testPreAndPostStateWithException() throws InterruptedException {
        AsyncFutureTask<String> future 
            = new AsyncFutureTask<String>(THROW_EXCEPTION, "Hello World!");
        
        assertFalse("Done", future.isDone());
        assertFalse("Cancelled", future.isCancelled());
        assertFalse("Completed Abnormally", future.isCompletedAbnormally());
        
        future.run();
        
        assertTrue("Done", future.isDone());
        assertFalse("Cancelled", future.isCancelled());
        assertTrue("Completed Abnormally", future.isCompletedAbnormally());
        
        try {
            future.get();
            fail("Should have failed!");
        } catch (ExecutionException expected) {
            assertTrue(expected.getMessage().contains("Expected!"));
        }
    }
    
    /**
     * Check if the {@link EventListener}s are being notified regardless if
     * an {@link Exception} is thrown or not.
     */
    public void testNotifyListeners() throws InterruptedException {
        checkListenerNotification(DO_NOTHING);
        checkListenerNotification(THROW_EXCEPTION);
    }
    
    private static AsyncFuture<String> checkListenerNotification(
            Runnable task) throws InterruptedException {
        AsyncFutureTask<String> future 
            = new AsyncFutureTask<String>(task, "Hello World!");
        
        final CountDownLatch latch = new CountDownLatch(1);
        future.addFutureListener(new DHTFutureAdapter<String>() {
            @Override
            protected void operationComplete(FutureEvent<String> event) {
                latch.countDown();
            }
        });
        
        future.run();
        
        boolean success = latch.await(1L, TimeUnit.SECONDS);
        assertTrue("Event was not fired", success);
        
        return future;
    }
    
    /**
     * Check if {@link AsyncFuture#setValue(Object)} is working
     */
    public void testSetValue() throws InterruptedException {
        AsyncFuture<String> future 
            = new AsyncValueFuture<String>();
        
        final CountDownLatch latch = new CountDownLatch(1);
        future.addFutureListener(new DHTFutureAdapter<String>() {
            @Override
            protected void operationComplete(FutureEvent<String> event) {
                latch.countDown();
            }
        });
        
        future.setValue("Hello World!");
        
        boolean success = latch.await(1L, TimeUnit.SECONDS);
        assertTrue("Event was not fired", success);
    }
    
    /**
     * Check if {@link AsyncFuture#setException(Throwable)} is working
     */
    public void testSetException() throws InterruptedException {
        AsyncFuture<String> future 
            = new AsyncValueFuture<String>();
        
        final CountDownLatch latch = new CountDownLatch(1);
        future.addFutureListener(new DHTFutureAdapter<String>() {
            @Override
            protected void operationComplete(FutureEvent<String> event) {
                latch.countDown();
            }
        });
        
        future.setException(new IllegalStateException("Expected!"));
        
        boolean success = latch.await(1L, TimeUnit.SECONDS);
        assertTrue("Event was not fired", success);
    }
}
