package com.limegroup.gnutella.mp3;

import com.limegroup.gnutella.gui.mp3.Semaphore;
import com.limegroup.gnutella.util.BaseTestCase;

public class SemaphoreTest extends BaseTestCase {

    public SemaphoreTest(String name) {
        super(name);
    }

    public void testSignalAll() throws Exception {
        final Semaphore s = new Semaphore();
        final int[] threadCompletedCount = {0};
        for(int i = 0 ; i < 10; i++) {
            Thread t = new Thread() {
                    public void run() {
                        s.waitForSignal();
                        threadCompletedCount[0]++;
                    }
                };
            t.start();
        }
        Thread.sleep(200);
        s.signalAll();
        s.signal();
        Thread.sleep(200);
        assertEquals(threadCompletedCount[0],10);
        for(int i = 0; i<1000; i++) {
            s.waitForSignal();
        }       
    }

    public void testWaitForSignalTimesout() throws Exception {
        Semaphore s = new Semaphore();
        long start = System.currentTimeMillis();
        try {
            s.waitForSignal(500);
        } catch (Semaphore.TimedOut t) {
            long now = System.currentTimeMillis();
            assertGreaterThan(499,now-start);
            return;
        }
        fail("TimedOut not thrown");
    }

    public void testWaitForSignalDoesntTimeout() throws Exception {
        final Semaphore s = new Semaphore();
        Thread t = new Thread() {public void run() {
            s.signal();
        }};
        t.start();
        long start = System.currentTimeMillis();
        s.waitForSignal(500);
        long now = System.currentTimeMillis();
        assertLessThan(200,now-start);
        t.join();
    }
    
}
