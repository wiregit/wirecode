package org.limewire.mojito.statistics;

public class StoreGroup extends BasicGroup {
    
    private final Statistic<Long> noSecurityToken = new Statistic<Long>();
    
    private final Statistic<Long> badSecurityToken = new Statistic<Long>();
    
    private final Statistic<Long> requestRejected = new Statistic<Long>();
    
    private final Statistic<Long> forwardToNearest = new Statistic<Long>();
    
    private final Statistic<Long> removeFromFurthest = new Statistic<Long>();
    
    private final Statistic<Long> publishedValues = new Statistic<Long>();
    
    private final Statistic<Long> expiredValues = new Statistic<Long>();
    
    public Statistic<Long> getNoSecurityToken() {
        return noSecurityToken;
    }
    
    public Statistic<Long> getBadSecurityToken() {
        return badSecurityToken;
    }
    
    public Statistic<Long> getRequestRejected() {
        return requestRejected;
    }
    
    public Statistic<Long> getForwardToNearest() {
        return forwardToNearest;
    }
    
    public Statistic<Long> getRemoveFromFurthest() {
        return removeFromFurthest;
    }
    
    public Statistic<Long> getPublishedValues() {
        return publishedValues;
    }
    
    public Statistic<Long> getExpiredValues() {
        return expiredValues;
    }
}
