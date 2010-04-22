package org.limewire.mojito.routing;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.settings.NetworkSettings;

public class BucketRefresherConfig {

    private volatile long pingTimeout 
        = NetworkSettings.DEFAULT_TIMEOUT.getValue();
    
    private volatile long lookupTimeout 
        = LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue();
    
    public BucketRefresherConfig() {
        
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
}
