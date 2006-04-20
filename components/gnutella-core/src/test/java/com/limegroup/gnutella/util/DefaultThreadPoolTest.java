package com.limegroup.gnutella.util;

import junit.framework.Test;

public class DefaultThreadPoolTest extends BaseTestCase {

    public DefaultThreadPoolTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DefaultThreadPoolTest.class);
    } 

    public void testThreadStarts() throws Exception {
        ThreadPool pool = new DefaultThreadPool("Name");
        Runner r1 = new Runner();
        pool.invokeLater(r1);
        Thread.sleep(100);
        assertTrue(r1.isRan());
    }
    
    public void testReuseThreads() throws Exception {
        ThreadPool pool = new DefaultThreadPool("Name");
        Runner r1 = new Runner();
        Runner r2 = new Runner();
        pool.invokeLater(r1);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        pool.invokeLater(r2);
        Thread.sleep(100);
        assertTrue(r2.isRan());
        assertSame(r1.getThread(), r2.getThread());
        assertEquals("Name", r1.getThread().getName());
    }
    
    public void testThreadLingers() throws Exception {
        ThreadPool pool = new DefaultThreadPool("Name");
        Runner r1 = new Runner();
        pool.invokeLater(r1);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        Thread thread = r1.getThread();
        assertTrue(thread.isAlive());
        Thread.sleep(3500);
        assertTrue(thread.isAlive());
        Thread.sleep(2000);
        assertFalse(thread.isAlive());
        assertEquals("Name", r1.getThread().getName());
    }
    
    public void testManyThreadsAtOnce() throws Exception {
        ThreadPool pool = new DefaultThreadPool("Name");
        Runner r1 = new Runner(5000);
        Runner r2 = new Runner(5000);
        pool.invokeLater(r1);
        pool.invokeLater(r2);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertTrue(r2.isRan());
        assertNotSame(r1.getThread(), r2.getThread());
        assertEquals("Name", r1.getThread().getName());        
        assertEquals("Name", r2.getThread().getName());
    }
    
    public void testMaxThreadLimit() throws Exception {
        ThreadPool pool = new DefaultThreadPool("Name", 1);
        Runner r1 = new Runner(5000);
        Runner r2 = new Runner(5000);
        pool.invokeLater(r1);
        pool.invokeLater(r2);
        Thread.sleep(1000);
        assertTrue(r1.isRan());
        assertFalse(r2.isRan());
        Thread.sleep(5000);
        assertTrue(r2.isRan());
        assertSame(r1.getThread(), r2.getThread());
        assertEquals("Name", r1.getThread().getName());
    }
    
    public void testDaemonThread() throws Exception {
        ThreadPool pool = new DefaultThreadPool("Name");
        Runner r1 = new Runner();
        pool.invokeLater(r1);
        Thread.sleep(100);
        assertTrue(r1.isRan());
        assertTrue(r1.getThread().isDaemon());
    }
    
    static class Runner implements Runnable {
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
