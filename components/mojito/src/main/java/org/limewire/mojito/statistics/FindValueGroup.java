package org.limewire.mojito.statistics;

public class FindValueGroup extends LookupGroup {
    
    private final Statistic<Long> notFound = new Statistic<Long>();
    
    public Statistic<Long> getNotFound() {
        return notFound;
    }
}
