package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;

@Singleton
public class OutOfBandThroughputMeasurer {
    
    private static final Log LOG = LogFactory.getLog(OutOfBandThroughputMeasurer.class);
    
    private final ScheduledExecutorService backgroundExecutor;

    @Inject
    public OutOfBandThroughputMeasurer(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }
    
    void initialize() {
        Runnable adjuster = new Runnable() {
            public void run() {
                if (LOG.isDebugEnabled())
                    LOG.debug("current success rate "+ OutOfBandThroughputStat.getSuccessRate()+
                            " based on "+((int)OutOfBandThroughputStat.RESPONSES_REQUESTED.getTotal())+ 
                            " measurements with a min sample size "+OutOfBandThroughputStat.MIN_SAMPLE_SIZE);
                if (!OutOfBandThroughputStat.isSuccessRateGreat() &&
                    !OutOfBandThroughputStat.isSuccessRateTerrible()) {
                    LOG.debug("boosting sample size by 500");
                    OutOfBandThroughputStat.MIN_SAMPLE_SIZE += 500;
                }
            }
        };
        
        int thirtyMins = 30 * 60 * 1000;
        backgroundExecutor.scheduleWithFixedDelay(adjuster, thirtyMins, thirtyMins, TimeUnit.MILLISECONDS);
    }
    
}
