package com.limegroup.gnutella;

import java.io.Serializable;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;

/**
 * A helper class for implementing the BandwidthTracker interface.  For
 * backwards compatibility, this implements the Serializable interface and marks
 * some fields transient.  However, LimeWire currently only reads but not writes
 * BandwidthTrackerImpl.
 */
public class BandwidthTrackerImpl implements Serializable {
    static final long serialVersionUID = 7694080781117787305L;

    /** Keep 10 clicks worth of data, which we can then average to get a more
     *  accurate moving time average.
     *  INVARIANT: snapShots[0]==measuredBandwidth.floatValue() */
    transient Buffer /* of Float */ snapShots = new Buffer(10);
    
    long lastTime;
    int lastAmountRead;

    /** The most recent measured bandwidth.  DO NOT DELETE THIS; it exists
     *  for backwards serialization reasons. */
    float measuredBandwidth;

    /** 
     * Measures the data throughput since the last call to measureBandwidth,
     * assuming this has read amountRead bytes.  This value can be read by
     * calling getMeasuredBandwidth.  
     *
     * @param amountRead the cumulative amount read from this, in BYTES.
     *  Should be larger than the argument passed in the last call to
     *  measureBandwidth(..).
     */
    public synchronized void measureBandwidth(int amountRead) {
        long currentTime=System.currentTimeMillis();
        //We always discard the first sample, and any others until after
        //progress is made.  
        //This prevents sudden bandwidth spikes when resuming
        //uploads and downloads.  Remember that bytes/msec=KB/sec.
        if (lastAmountRead==0 || currentTime==lastTime) {
            measuredBandwidth=0.f;
        } else {            
            measuredBandwidth=(float)(amountRead-lastAmountRead)
                                / (float)(currentTime-lastTime);
            //Ensure positive!
            measuredBandwidth=Math.max(measuredBandwidth, 0.f);
        }
        lastTime=currentTime;
        lastAmountRead=amountRead;
        snapShots.add(new Float(measuredBandwidth));
    }

    /** @see BandwidthTracker#getMeasuredBandwidth */
    public synchronized float getMeasuredBandwidth() 
        throws InsufficientDataException {
        //TODO - make it throw and exception if not enough data
        int size = snapShots.getSize();
        if (size  < 3 )
            throw new InsufficientDataException();
        Iterator iter = snapShots.iterator();
        float total = 0;
        while(iter.hasNext()) {
            total+= ((Float)iter.next()).floatValue();
        }
        return total/size;
    }    
}
