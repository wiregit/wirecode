package org.limewire.mojito.statistics;

import org.limewire.mojito.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito.routing.RouteTable.RouteTableListener;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent.EventType;

public class RouteTableGroup extends StatisticsGroup implements RouteTableListener {

    private final Statistic<Long> liveCount = new Statistic<Long>();
    
    private final Statistic<Long> unknownCount = new Statistic<Long>();
    
    private final Statistic<Long> cachedCount = new Statistic<Long>();
    
    private final Statistic<Long> removedDead = new Statistic<Long>();
    
    private final Statistic<Long> bucketCount = new Statistic<Long>();
    
    public void handleRouteTableEvent(RouteTableEvent event) {
        EventType type = event.getEventType();
        
        if (type == EventType.ADD_ACTIVE_CONTACT) {
            if (event.getContact().isAlive()) {
                liveCount.incrementByOne();
            } else {
                unknownCount.incrementByOne();
            }
            
        } else if (type == EventType.ADD_CACHED_CONTACT) {
            cachedCount.incrementByOne();
            
        } else if (type == EventType.REMOVE_CONTACT) {
            if (event.getContact().isDead()) {
                removedDead.incrementByOne();
            }
            
        } else if (type == EventType.REPLACE_CONTACT) {
            liveCount.incrementByOne();
            
            if (event.getContact().isDead()) {
                removedDead.incrementByOne();
            }
            
        } else if (type == EventType.SPLIT_BUCKET) {
            // Increment only by one 'cause splitting a Bucket
            // creates only one new Bucket
            bucketCount.incrementByOne();
        }
    }
}
