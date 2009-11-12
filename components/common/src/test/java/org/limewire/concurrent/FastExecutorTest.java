package org.limewire.concurrent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.io.IOException;

import org.limewire.service.ErrorService;
import org.limewire.service.ErrorCallbackStub;
import org.limewire.util.BaseTestCase;

/**
 * Test the LimeScheduledThreadPoolExecutor in house implementation
 * of the ScheduledExecutorService
 */
public class FastExecutorTest extends BaseTestCase {
    
    private ErrorCallbackStub errorCallback;
    
    @Override
    public void setUp() {
        errorCallback = new ErrorCallbackStub();
        ErrorService.setErrorCallback(errorCallback);
    }
    
    /**
     * Make sure that if a task is scheduled in LimeScheduledThreadPoolExecutor,
     * uncaught exceptions are handled by ErrorService. 
     */
    public void testUncheckedExceptionIsReported() throws Exception {
        ScheduledExecutorService t = new LimeScheduledThreadPoolExecutor(1, 
            ExecutorsHelper.daemonThreadFactory("FastExecutor"));
        t.scheduleWithFixedDelay(new Runnable() {
            @Override public void run() {
                throw new RuntimeException();    
            }
        }, 10, 50, TimeUnit.MILLISECONDS);
        Thread.sleep(20);
        t.shutdown();
        assertEquals(1, errorCallback.getExceptionCount());
        assertInstanceof(RuntimeException.class, errorCallback.getException(0));  
    }
    
    /**
     * Make sure that if a task is scheduled in LimeScheduledThreadPoolExecutor,
     * but has been cancelled, that it is NOT reported to ErrorService. 
     */
    public void testCancelledTaskIsNotReported() throws Exception {
        ScheduledExecutorService t = new LimeScheduledThreadPoolExecutor(1, 
            ExecutorsHelper.daemonThreadFactory("FastExecutor"));
        Future future = t.scheduleWithFixedDelay(new Runnable() {
            @Override public void run() {
                throw new RuntimeException();    
            }
        }, 20, 50, TimeUnit.MILLISECONDS);
        future.cancel(false);
        Thread.sleep(30);
        t.shutdown();
        assertEquals(0, errorCallback.getExceptionCount());
    }
    
    /**
     * Make sure that if a task is scheduled in LimeScheduledThreadPoolExecutor,
     * and a checked exception is thrown, it is NOT reported to ErrorService. 
     */
    public void testCallableCheckedExceptionIsNotReported() throws Exception {
        ScheduledExecutorService t = new LimeScheduledThreadPoolExecutor(1, 
            ExecutorsHelper.daemonThreadFactory("FastExecutor"));
        Future result = t.schedule(new Callable<Object>() {
            @Override public Object call() throws IOException {
                throw new IOException();    
            }
        }, 10L, TimeUnit.MILLISECONDS);
        Thread.sleep(20);
        t.shutdown();
        assertEquals(0, errorCallback.getExceptionCount());
        
        try {
            result.get();  
            fail("Expected ExecutionException containing checked exception");
        } catch (ExecutionException e) {
            assertInstanceof(IOException.class, e.getCause());
        }
    }
}
