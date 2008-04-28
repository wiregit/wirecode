package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;

@Singleton
public class OutOfBandThroughputMeasurer {
    
    private static final Log LOG = LogFactory.getLog(OutOfBandThroughputMeasurer.class);
    
    private final ScheduledExecutorService backgroundExecutor;
    private final OutOfBandStatistics outOfBandStatistics;

    @Inject
    public OutOfBandThroughputMeasurer(@Named("backgroundExecutor")
    ScheduledExecutorService backgroundExecutor, OutOfBandStatistics outOfBandStatistics) {
        this.backgroundExecutor = backgroundExecutor;
        this.outOfBandStatistics = outOfBandStatistics;
    }

    void initialize() {
        Runnable adjuster = new Runnable() {
            public void run() {
                if (LOG.isDebugEnabled())
                    LOG.debug("current success rate " + outOfBandStatistics.getSuccessRate()
                            + " based on " + outOfBandStatistics.getRequestedResponses()
                            + " measurements with a min sample size "
                            + outOfBandStatistics.getSampleSize());
                if (!outOfBandStatistics.isSuccessRateGreat()
                        && !outOfBandStatistics.isSuccessRateTerrible()) {
                    LOG.debug("boosting sample size");
                    outOfBandStatistics.increaseSampleSize();
                }
            }
        };
        
        int thirtyMins = 30 * 60 * 1000;
        backgroundExecutor.scheduleWithFixedDelay(adjuster, thirtyMins, thirtyMins, TimeUnit.MILLISECONDS);
    }
    
}
