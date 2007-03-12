package org.limewire.security;

import org.limewire.concurrent.SchedulingThreadPool;

/**
 * Class that hides the rotation of private keys.
 */
class MACCalculatorRotator implements MACCalculatorRepository {
    private final SettingsProvider provider;
    private final MACCalculatorFactory factory;
    private final SchedulingThreadPool scheduler;
    private MACCalculator current, old;
    private final Runnable rotator, expirer;

    /**
     * @param scheduler a <tt>SchedulingThreadPool</tt> that will execute the rotation
     * @param factory something that creates the QKGenerators
     * @param provider a <tt>SettingsProvider</tt>.  The change period must be bigger
     * than the grace period. 
     */
    MACCalculatorRotator(SchedulingThreadPool scheduler, 
            MACCalculatorFactory factory, 
            SettingsProvider provider) {
        this.provider = provider;
        this.factory = factory;
        this.scheduler = scheduler;
        
        if (provider.getGracePeriod() >= provider.getChangePeriod())
            throw new IllegalArgumentException("settings not supported");
        
        rotator = new Runnable() {
            public void run() {
                rotate();
            }
        };
        expirer = new Runnable() {
            public void run() {
                expireOld();
            }
        };
        
        rotate();
    }
    
    public synchronized MACCalculator[] getValidMACCalculators() {
        if (old == null)
            return new MACCalculator[]{current};
        else
            return new MACCalculator[]{current, old};
    }
    
    public synchronized MACCalculator getCurrentMACCalculator() {
        return current;
    }
    
    private void rotate() {
        MACCalculator newKQ = factory.createMACCalculator();
        synchronized(this) {
            old = current;
            current = newKQ;
        }
        scheduler.invokeLater(rotator, provider.getChangePeriod());
        scheduler.invokeLater(expirer, provider.getGracePeriod());
    }
    
    private synchronized void expireOld() {
        old = null;
    }
}
