package de.kapsi.net.kademlia.routing;

import java.io.IOException;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;

public class RandomBucketRefresher extends TimerTask implements Runnable{
    
    private static final Log LOG = LogFactory.getLog(RandomBucketRefresher.class);
    
    private Context context;
    
    public RandomBucketRefresher(Context context) {
        this.context = context;
    }
    
    public void run() {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Random bucket refresh");
        }
        
        try {
            context.getRouteTable().refreshBuckets(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
