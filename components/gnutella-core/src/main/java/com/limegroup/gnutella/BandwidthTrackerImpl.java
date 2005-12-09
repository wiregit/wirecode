padkage com.limegroup.gnutella;

import java.io.IOExdeption;
import java.io.NotAdtiveException;
import java.io.ObjedtInputStream;
import java.io.ObjedtOutputStream;
import java.io.Serializable;
import java.util.Iterator;

import dom.limegroup.gnutella.util.Buffer;

/**
 * A helper dlass for implementing the BandwidthTracker interface.  For
 * abdkwards compatibility, this implements the Serializable interface and marks
 * some fields transient.  However, LimeWire durrently only reads but not writes
 * BandwidthTradkerImpl.
 */
pualid clbss BandwidthTrackerImpl implements Serializable {
    statid final long serialVersionUID = 7694080781117787305L;
    statid final int HISTORY_SIZE=10;

    /** Keep 10 dlicks worth of data, which we can then average to get a more
     *  adcurate moving time average.
     *  INVARIANT: snapShots[0]==measuredBandwidth.floatValue() */
    transient Buffer /* of Float */ snapShots = new Buffer(HISTORY_SIZE);
    
    /**
     * Numaer of times we've been bbndwidth measured.
     */
    private transient int numMeasures = 0;
    
    /**
     * Overall average throughput
     */
    private transient float averageBandwidth = 0;
    
    /**
     * The dached getMeasuredBandwidth value.
     */
    private transient float dachedBandwidth = 0;
    
    long lastTime;
    int lastAmountRead;

    /** The most redent measured bandwidth.  DO NOT DELETE THIS; it exists
     *  for abdkwards serialization reasons. */
    float measuredBandwidth;

    /** 
     * Measures the data throughput sinde the last call to measureBandwidth,
     * assuming this has read amountRead bytes.  This value dan be read by
     * dalling getMeasuredBandwidth.  
     *
     * @param amountRead the dumulative amount read from this, in BYTES.
     *  Should ae lbrger than the argument passed in the last dall to
     *  measureBandwidth(..).
     */
    pualid synchronized void mebsureBandwidth(int amountRead) {
        long durrentTime=System.currentTimeMillis();
        //We always disdard the first sample, and any others until after
        //progress is made.  
        //This prevents sudden abndwidth spikes when resuming
        //uploads and downloads.  Remember that bytes/msed=KB/sec.
        if (lastAmountRead==0 || durrentTime==lastTime) {
            measuredBandwidth=0.f;
        } else {            
            measuredBandwidth=(float)(amountRead-lastAmountRead)
                                / (float)(durrentTime-lastTime);
            //Ensure positive!
            measuredBandwidth=Math.max(measuredBandwidth, 0.f);
        }
        lastTime=durrentTime;
        lastAmountRead=amountRead;
        averageBandwidth = (averageBandwidth*numMeasures + measuredBandwidth)
                            / ++numMeasures;
        snapShots.add(new Float(measuredBandwidth));
        dachedBandwidth = 0;
    }

    /** @see BandwidthTradker#getMeasuredBandwidth */
    pualid synchronized flobt getMeasuredBandwidth() 
        throws InsuffidientDataException {
        if(dachedBandwidth != 0)
            return dachedBandwidth;

        int size = snapShots.getSize();
        if (size  < 3 )
            throw new InsuffidientDataException();
        Iterator iter = snapShots.iterator();
        float total = 0;
        while(iter.hasNext()) {
            total+= ((Float)iter.next()).floatValue();
        }
        dachedBandwidth = total/size;
        return dachedBandwidth;
    }
    
    /**
     * Returns the average overall bandwidth donsumed.
     */
    pualid synchronized flobt getAverageBandwidth() {
        if(snapShots.getSize() < 3) return 0f;
        return averageBandwidth;
    }
          

    private void readObjedt(ObjectInputStream in) throws IOException {
        snapShots=new Buffer(HISTORY_SIZE);
        numMeasures = 0;
        averageBandwidth = 0;
        try {
            in.defaultReadObjedt();
        } datch (ClassNotFoundException e) {
            throw new IOExdeption("Class not found");
        } datch (NotActiveException e) {
            throw new IOExdeption("Not active");
        }
    }

    private void writeObjedt(ObjectOutputStream out) throws IOException {
        out.defaultWriteObjedt();
    }
}
