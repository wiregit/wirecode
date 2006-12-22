package org.limewire.concurrent;


import org.limewire.concurrent.ThreadExecutor;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;


public class ThreadExecutorTest extends BaseTestCase {

    public ThreadExecutorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ThreadExecutorTest.class);
    }
    
    public void testThreadStarts() throws Exception {
        Runner r1 = new Runner();
        ThreadExecutor.startThread(r1, "Name");
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertEquals("Name", r1.getName());
    }
    
    public void testReuseThreads() throws Exception {
        Runner r1 = new Runner();
        Runner r2 = new Runner();
        ThreadExecutor.startThread(r1, "Name1");
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertEquals("Name1", r1.getName());
        assertEquals("IdleThread", r1.getThread().getName());
        ThreadExecutor.startThread(r2, "Name2");
        Thread.sleep(100);
        assertTrue(r2.isRan());
        assertSame(r1.getThread(), r2.getThread());
        assertEquals("Name2", r2.getName());
    }
    
    public void testThreadLingers() throws Exception {
        Runner r1 = new Runner();
        ThreadExecutor.startThread(r1, "Name");
        Thread.sleep(100);
        assertTrue(r1.isRan());
        Thread thread = r1.getThread();
        assertEquals("IdleThread", r1.getThread().getName());
        assertTrue(thread.isAlive());
        Thread.sleep(3500);
        assertTrue(thread.isAlive());
        Thread.sleep(2000);
        assertFalse(thread.isAlive());
        assertEquals("IdleThread", r1.getThread().getName());
    }
    
    public void testManyThreadsAtOnce() throws Exception {
        Runner r1 = new Runner(5000);
        Runner r2 = new Runner(5000);
        ThreadExecutor.startThread(r1, "Name1");
        ThreadExecutor.startThread(r2, "Name2");
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertTrue(r2.isRan());
        assertNotSame(r1.getThread(), r2.getThread());
        assertEquals("Name1", r1.getName());        
        assertEquals("Name2", r2.getName());
    }
    
    public void testDaemonThread() throws Exception {
        Runner r1 = new Runner();
        ThreadExecutor.startThread(r1, "Name");
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertTrue(r1.getThread().isDaemon());
    }    
    
    private static class Runner implements Runnable {
        private boolean ran;
        private Thread thread;
        private String name;
        private long waitLength = -1;
    
        public Runner() {            
        }
        
        public Runner(long waitLength) {
            this.waitLength = waitLength;
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
    }    
}
