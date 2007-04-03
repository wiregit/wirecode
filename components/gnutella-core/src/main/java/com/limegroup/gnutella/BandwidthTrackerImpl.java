package com.limegroup.gnutella;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.limewire.collection.Buffer;


/**
 * A helper class for implementing the BandwidthTracker interface.  For
 * backwards compatibility, this implements the Serializable interface and marks
 * some fields transient.  However, LimeWire currently only reads but not writes
 * BandwidthTrackerImpl.
 */
public class BandwidthTrackerImpl implements Serializable {
    static final long serialVersionUID = 7694080781117787305L;
    static final int HISTORY_SIZE=10;

    /** Keep 10 clicks worth of data, which we can then average to get a more
     *  accurate moving time average.
     *  INVARIANT: snapShots[0]==measuredBandwidth.floatValue() */
    transient Buffer<Float> snapShots = new Buffer<Float>(HISTORY_SIZE);
    
    /**
     * Number of times we've been bandwidth measured.
     */
    private transient int numMeasures = 0;
    
    /**
     * Overall average throughput
     */
    private transient float averageBandwidth = 0;
    
    /**
     * The cached getMeasuredBandwidth value.
     */
    private transient float cachedBandwidth = 0;
    
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
        averageBandwidth = (averageBandwidth*numMeasures + measuredBandwidth)
                            / ++numMeasures;
        snapShots.add(new Float(measuredBandwidth));
        cachedBandwidth = 0;
    }

    /** @see BandwidthTracker#getMeasuredBandwidth */
    public synchronized float getMeasuredBandwidth() 
        throws InsufficientDataException {
        if(cachedBandwidth != 0)
            return cachedBandwidth;

        int size = snapShots.getSize();
        if (size  < 3 )
            throw new InsufficientDataException();
        float total = 0;
        for(Float f : snapShots)
            total += f.floatValue();
        cachedBandwidth = total/size;
        return cachedBandwidth;
    }
    
    /**
     * Returns the average overall bandwidth consumed.
     */
    public synchronized float getAverageBandwidth() {
        if(snapShots.getSize() < 3) return 0f;
        return averageBandwidth;
    }
          

    private void readObject(ObjectInputStream in) throws IOException {
        snapShots=new Buffer<Float>(HISTORY_SIZE);
        numMeasures = 0;
        averageBandwidth = 0;
        try {
            in.defaultReadObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found");
        } catch (NotActiveException e) {
            throw new IOException("Not active");
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
