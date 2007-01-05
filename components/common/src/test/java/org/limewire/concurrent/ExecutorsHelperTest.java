package org.limewire.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import junit.framework.Test;

import org.limewire.service.ErrorCallback;
import org.limewire.service.ErrorCallbackStub;
import org.limewire.service.ErrorService;
import org.limewire.util.BaseTestCase;

public class ExecutorsHelperTest extends BaseTestCase {

    public ExecutorsHelperTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ExecutorsHelperTest.class);
    }
    
    public void testProcessingQueueStarts() throws Exception {
        Runner r1 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("Sam");
        service.execute(r1);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertEquals("Sam", r1.getName());
    }
    
    public void testProcessingQueueReuseThread() throws Exception {
        Runner r1 = new Runner();
        Runner r2 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("Sammy");
        service.execute(r1);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertEquals("Sammy", r1.getName());
        assertEquals("Sammy", r1.getThread().getName());
        service.execute(r2);
        Thread.sleep(100);
        assertTrue(r2.isRan());
        assertSame(r1.getThread(), r2.getThread());
        assertEquals("Sammy", r2.getName());
    }
    
    public void testProcessingQueueThreadLingersThenDies() throws Exception {
        Runner r1 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("SammyB");
        service.execute(r1);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        Thread thread = r1.getThread();
        assertEquals("SammyB", r1.getThread().getName());
        assertTrue(thread.isAlive());
        Thread.sleep(3500);
        assertTrue(thread.isAlive());
        Thread.sleep(2000);
        assertFalse(thread.isAlive());
        assertEquals("SammyB", r1.getThread().getName());
    }
    
    public void testProcessingQueueGoesSequentially() throws Exception {
        Runner r1 = new Runner(2000);
        Runner r2 = new Runner(2000);
        ExecutorService service = ExecutorsHelper.newProcessingQueue("SammyB");
        service.execute(r1);
        service.execute(r2);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertFalse(r2.isRan());
        r1.waitForDone();
        Thread.sleep(100);
        assertTrue(r2.isRan());
        assertSame(r1.getThread(), r2.getThread());
        assertEquals("SammyB", r1.getName());        
        assertEquals("SammyB", r2.getName());
    }
    
    public void testProcessingQueueUsesDaemonThread() throws Exception {
        Runner r1 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("B");
        service.execute(r1);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertTrue(r1.getThread().isDaemon());
    }
    
    public void testProcessingQueueExceptionContinues() throws Exception {
        ErrorCallback oldCallback = ErrorService.getErrorCallback();
        try {
            ErrorCallbackStub testCallback = new ErrorCallbackStub();
            ErrorService.setErrorCallback(testCallback);
            assertEquals(0, testCallback.getExceptionCount());
            Runner r1 = new Runner(1000, true);
            Runner r2 = new Runner(1000);
            ExecutorService service = ExecutorsHelper.newProcessingQueue("Exceptionary");
            service.execute(r1);
            service.execute(r2);
            Thread.sleep(100);
            assertTrue(r1.isRan());
            assertFalse(r2.isRan());
            r1.waitForDone();
            Thread.sleep(100);
            assertEquals(1, testCallback.getExceptionCount());
            Thread.sleep(100);
            assertTrue(r2.isRan());
            r2.waitForDone();
            assertEquals(1, testCallback.getExceptionCount());
        } finally {
            ErrorService.setErrorCallback(oldCallback);
        }
    }
    

    private static class Runner implements Runnable {
        private volatile boolean ran;
        private volatile Thread thread;
        private volatile String name;
        private volatile long waitLength;
        private final boolean throwException;
        private final CountDownLatch latch;
    
        public Runner() {
            this(-1);
        }
        
        public Runner(long waitLength) {
            this(waitLength, false);
        }
        
        public Runner(boolean throwException) {
            this(-1, throwException);
        }
        
        public Runner(long waitLength, boolean throwException) {
            this.throwException = throwException;
            this.waitLength = waitLength;
            latch = new CountDownLatch(1);
        }
        
        public void run() {
            ran = true;
            thread = Thread.currentThread();
            name = thread.getName();
            if(waitLength != -1) {
                try {
                    Thread.sleep(waitLength);
                } catch(InterruptedException ix) {}
            }
           latch.countDown();
           if(throwException)
               throw new RuntimeException("Abandon Hope.");
        }

        public boolean isRan() {
            return ran;
        }

        public Thread getThread() {
            return thread;
        }
        
        public String getName() {
            return name;
        }
        
        public void waitForDone() throws Exception {
            latch.await();
        }
    }    
    
}
    