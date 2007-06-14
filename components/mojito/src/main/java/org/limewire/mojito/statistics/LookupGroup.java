package org.limewire.mojito.statistics;

public class LookupGroup extends BasicGroup {
    
    private final Statistic<Integer> hops = new Statistic<Integer>();
    
    public Statistic<Integer> getHops() {
        return hops;
    }
}
