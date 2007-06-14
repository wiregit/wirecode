package org.limewire.mojito.statistics;

public class BasicGroup extends StatisticsGroup {
    
    private final Statistic<Long> requestsSent = new Statistic<Long>();
    
    private final Statistic<Long> responsesReceived = new Statistic<Long>();
    
    private final Statistic<Long> requestsReceived = new Statistic<Long>();
    
    private final Statistic<Long> responsesSent = new Statistic<Long>();
    
    public Statistic<Long> getRequestsSent() {
        return requestsSent;
    }
    
    public Statistic<Long> getResponsesReceived() {
        return responsesReceived;
    }
    
    public Statistic<Long> getRequestsReceived() {
        return requestsReceived;
    }
    
    public Statistic<Long> getResponsesSent() {
        return responsesSent;
    }
}
