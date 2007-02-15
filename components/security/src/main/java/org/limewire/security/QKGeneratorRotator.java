package org.limewire.security;

import org.limewire.concurrent.SchedulingThreadPool;

/**
 * Class that hides the rotation of private keys.
 */
class QKGeneratorRotator implements QKGeneratorKeychain {
    private final SettingsProvider provider;
    private final QKGeneratorFactory factory;
    private final SchedulingThreadPool scheduler;
    private QueryKeyGenerator current, old;
    private final Runnable rotator, expirer;

    /**
     * @param scheduler a <tt>SchedulingThreadPool</tt> that will execute the rotation
     * @param factory something that creates the QKGenerators
     * @param provider a <tt>SettingsProvider</tt>.  The change period must be bigger
     * than the grace period. 
     */
    QKGeneratorRotator(SchedulingThreadPool scheduler, 
            QKGeneratorFactory factory, 
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
    
    public synchronized QueryKeyGenerator[] getValidQueryKeyGenerators() {
        if (old == null)
            return new QueryKeyGenerator[]{current};
        else
            return new QueryKeyGenerator[]{current, old};
    }
    
    public synchronized QueryKeyGenerator getSecretKey() {
        return current;
    }
    
    private void rotate() {
        QueryKeyGenerator newKQ = factory.createQueryKeyGenerator();
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
