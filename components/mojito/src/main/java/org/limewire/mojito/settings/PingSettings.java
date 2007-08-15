package org.limewire.mojito.settings;

import org.limewire.setting.IntSetting;

public class PingSettings extends MojitoProps {

    private PingSettings() {}
    
    /**
     * The number of pings to send in parallel
     */
    public static final IntSetting PARALLEL_PINGS
        = FACTORY.createRemoteIntSetting("PARALLEL_PINGS", 15, 
                "Mojito.ParallelPings", 1, 30);
    /**
     * The maximum number of ping failures before pinging is
     * given up
     */
    public static final IntSetting MAX_PARALLEL_PING_FAILURES
        = FACTORY.createIntSetting("MAX_PARALLEL_PING_FAILURES", 40);
    
    /**
     * Returns the lock timeout for pings
     */
    public static long getWaitOnLock() {
        return ContextSettings.getWaitOnLock(
                NetworkSettings.DEFAULT_TIMEOUT.getValue());
    }
}
