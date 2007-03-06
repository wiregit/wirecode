package org.limewire.security;

import org.limewire.concurrent.SchedulingThreadPool;

/**
 * Class that hides the rotation of private keys.
 */
class SecurityTokenGeneratorRotator implements SecurityTokenGeneratorChain {
    private final SettingsProvider provider;
    private final SecurityTokenGeneratorFactory factory;
    private final SchedulingThreadPool scheduler;
    private SecurityTokenGenerator current, old;
    private final Runnable rotator, expirer;

    /**
     * @param scheduler a <tt>SchedulingThreadPool</tt> that will execute the rotation
     * @param factory something that creates the QKGenerators
     * @param provider a <tt>SettingsProvider</tt>.  The change period must be bigger
     * than the grace period. 
     */
    SecurityTokenGeneratorRotator(SchedulingThreadPool scheduler, 
            SecurityTokenGeneratorFactory factory, 
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
    
    public synchronized SecurityTokenGenerator[] getValidSecurityTokenGenerators() {
        if (old == null)
            return new SecurityTokenGenerator[]{current};
        else
            return new SecurityTokenGenerator[]{current, old};
    }
    
    public synchronized SecurityTokenGenerator getCurrentTokenGenerator() {
        return current;
    }
    
    private void rotate() {
        SecurityTokenGenerator newKQ = factory.createSecurityTokenGenerator();
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
