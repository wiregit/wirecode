package org.limewire.mojito.manager;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.settings.BootstrapSettings;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.settings.NetworkSettings;

/**
 * 
 */
public class BootstrapConfig {

    private static final int ALPHA = 4;
    
    private volatile int alpha = ALPHA;
    
    private volatile long pingTimeout 
        = NetworkSettings.DEFAULT_TIMEOUT.getValue();
    
    private volatile long lookupTimeout 
        = LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue();
    
    private volatile long refreshTimeout 
        = BootstrapSettings.BOOTSTRAP_TIMEOUT.getValue();
    
    public BootstrapConfig() {
        
    }
    
    /**
     * 
     */
    public int getAlpha() {
        return alpha;
    }
    
    /**
     * 
     */
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }
    
    /**
     * 
     */
    public long getPingTimeout(TimeUnit unit) {
        return unit.convert(pingTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public long getPingTimeoutInMillis() {
        return getPingTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public void setPingTimeout(long timeout, TimeUnit unit) {
        this.pingTimeout = unit.toMillis(timeout);
    }
    
    /**
     * 
     */
    public long getLookupTimeout(TimeUnit unit) {
        return unit.convert(lookupTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public long getLookupTimeoutInMillis() {
        return getLookupTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public void setLookupTimeout(long timeout, TimeUnit unit) {
        this.lookupTimeout = unit.toMillis(timeout);
    }
    
    /**
     * 
     */
    public long getRefreshTimeout(TimeUnit unit) {
        return unit.convert(refreshTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public long getRefreshTimeoutInMillis() {
        return getRefreshTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public void setRefreshTimeout(long timeout, TimeUnit unit) {
        this.refreshTimeout = unit.toMillis(timeout);
    }
    
    /**
     * 
     */
    public long getTime(TimeUnit unit) {
        return getPingTimeout(unit) 
                + getLookupTimeout(unit) 
                + getRefreshTimeout(unit);
    }
    
    /**
     * 
     */
    public long getTimeInMillis() {
        return getTime(TimeUnit.MILLISECONDS);
    }
}
