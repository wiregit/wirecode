package org.limewire.mojito.statistics;

public class StatsGroup extends BasicGroup {
    
    private final Statistic<Long> badSignature = new Statistic<Long>();
    
    public Statistic<Long> getBadSignature() {
        return badSignature;
    }
}
