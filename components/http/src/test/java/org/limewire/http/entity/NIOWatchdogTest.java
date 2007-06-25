package org.limewire.http.entity;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.NIOTestUtils;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.BaseTestCase;

public class NIOWatchdogTest extends BaseTestCase {

    public NIOWatchdogTest(String name) {
        super(name);
    }

    public void testActivate() throws Exception {
        MyShutdownable shutdownable = new MyShutdownable();
        NIOWatchdog watchdog = new NIOWatchdog(NIODispatcher.instance(), shutdownable , 50);
        assertFalse(shutdownable.shutdown);
        
        Thread.sleep(50);
        NIOTestUtils.waitForNIO();
        assertFalse(shutdownable.shutdown);
        
        watchdog.activate();
        Thread.sleep(50);
        NIOTestUtils.waitForNIO();
        assertTrue(shutdownable.shutdown);
        
        // reactivate
        shutdownable.shutdown = false;
        watchdog.activate();
        Thread.sleep(50);
        NIOTestUtils.waitForNIO();
        assertTrue(shutdownable.shutdown);
    }
    
    public void testDeactivate() throws Exception {
        MyShutdownable shutdownable = new MyShutdownable();
        NIOWatchdog watchdog = new NIOWatchdog(NIODispatcher.instance(), shutdownable, 50);

        watchdog.activate();
        watchdog.deactivate();
        Thread.sleep(50);
        NIOTestUtils.waitForNIO();
        assertFalse(shutdownable.shutdown);
        
        watchdog.deactivate();
        assertFalse(shutdownable.shutdown);
    }

    public void testActivateOnNIOThread() throws Exception {
        final MyShutdownable shutdownable = new MyShutdownable();
        final NIOWatchdog watchdog = new NIOWatchdog(NIODispatcher.instance(), shutdownable, 1000);

        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                watchdog.activate();
            }            
        });
        NIOTestUtils.waitForNIO();
        Thread.sleep(50);
        assertFalse(shutdownable.shutdown);
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                watchdog.deactivate();
            }            
        });
        NIOTestUtils.waitForNIO();
        assertFalse(shutdownable.shutdown);
    }
    private class MyShutdownable implements Shutdownable {
        private volatile boolean shutdown;

        public void shutdown() {
            this.shutdown = true;
        }

    }

}
