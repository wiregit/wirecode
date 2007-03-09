package org.limewire.security;

import java.util.Arrays;
import java.util.concurrent.Future;

import junit.framework.Test;

import org.limewire.concurrent.SchedulingThreadPool;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.util.BaseTestCase;

public class MACCalculatorRotatorTest extends BaseTestCase {

    public MACCalculatorRotatorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(MACCalculatorRotatorTest.class);
    }
    
    public void testKeyGeneratorsAreRotatedAndExpired() {
        
        SchedulingTestThreadPool pool = new SchedulingTestThreadPool();
        MACCalculatorRotator rotator = new MACCalculatorRotator(pool, new MACCalculatorRepositoryManager.TEAMACCalculatorFactory(),
                new SettingsProvider() {
                    public long getChangePeriod() {
                        return 1;
                    }
                    public long getGracePeriod() {
                        return 0;
                    }
        });
        
        MACCalculator generator = rotator.getCurrentMACCalculator();
        assertEquals(1, rotator.getValidMACCalculators().length);
        assertContains(Arrays.asList(rotator.getValidMACCalculators()), generator);
        
        // run rotate
        Runnable r = pool.r;
        pool.r = null;
        r.run();
        
        assertEquals(2, rotator.getValidMACCalculators().length);
        MACCalculator generatorNew = rotator.getCurrentMACCalculator();
        assertNotSame(generator, generatorNew);
        assertContains(Arrays.asList(rotator.getValidMACCalculators()), generator);
        assertContains(Arrays.asList(rotator.getValidMACCalculators()), generatorNew);
        
        // expire old key
        r = pool.r2;
        pool.r2 = null;
        r.run();
        
        assertEquals(1, rotator.getValidMACCalculators().length);
        MACCalculator generator3 = rotator.getCurrentMACCalculator();
        assertSame(generatorNew, generator3);
        assertContains(Arrays.asList(rotator.getValidMACCalculators()), generator3);
        assertNotContains(Arrays.asList(rotator.getValidMACCalculators()), generator);
    }
    
    public void testGracePeriodIsHonored() throws Exception {
        
        WrappingSchedulingTestThreadPool pool = new WrappingSchedulingTestThreadPool();
        
        MACCalculatorRotator rotator = new MACCalculatorRotator(pool, new MACCalculatorRepositoryManager.TEAMACCalculatorFactory(),
                new SettingsProvider() {
                    public long getChangePeriod() {
                        return 500;
                    }
                    public long getGracePeriod() {
                        return 250;
                    }
        });
        
        assertEquals(1, rotator.getValidMACCalculators().length);
        
        pool.r.waitForRunnable();
        
        assertEquals(2, rotator.getValidMACCalculators().length);
        
        pool.r2.waitForRunnable();
        
        assertEquals(1, rotator.getValidMACCalculators().length);
        
        pool.r.waitForRunnable();
        
        assertEquals(2, rotator.getValidMACCalculators().length);
    }
    
    public void testInvalidSettings() throws Exception {
        try {
            new MACCalculatorRotator(new SchedulingTestThreadPool(), 
                    new MACCalculatorRepositoryManager.TEAMACCalculatorFactory(),
                    new SettingsProvider() {
                public long getChangePeriod() {
                    return 500;
                }
                public long getGracePeriod() {
                    return 2500;
                }
            });
            fail("constructed rotator with grace > expiry");
        } catch (IllegalArgumentException expected ){}
       
    }

    private static class WrappingSchedulingTestThreadPool implements SchedulingThreadPool {
        
        private SchedulingThreadPool pool = SimpleTimer.sharedTimer();
        
        private NotifyinRunnable r;
        
        private NotifyinRunnable r2;
        
        private int modCounter = 0;

        public void invokeLater(Runnable r) {
            throw new UnsupportedOperationException("Not used in old tests, implement if needed");
        }

        public Future invokeLater(Runnable r, long delay) {
            if ((modCounter++ % 2) == 0) {
                this.r = new NotifyinRunnable(r);
                return pool.invokeLater(this.r, delay);
            }
            else {
                this.r2 = new NotifyinRunnable(r);
                return pool.invokeLater(this.r2, delay);
            }
        }
                
    }
    
    private static class NotifyinRunnable implements Runnable {

        private Runnable r;
        
        public NotifyinRunnable(Runnable r) {
            this.r = r;
        }
        
        public synchronized void run() {
            r.run();
            r = null;
            notify();
        }
        
        public synchronized void waitForRunnable() throws InterruptedException {
            while (r != null) {
                wait();
            }
        }
        
    }
    
    private static class SchedulingTestThreadPool implements SchedulingThreadPool {

        Runnable r;
        Runnable r2;
        
        public void invokeLater(Runnable r) {

        }

        public Future invokeLater(Runnable r, long delay) {
            if (this.r == null) {
                this.r = r;
            }
            else {
                r2 = r;
            }
            return null;
        }
        
    }

}
