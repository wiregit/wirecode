pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.NotActiveException;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.io.Serializable;
import jbva.util.Iterator;

import com.limegroup.gnutellb.util.Buffer;

/**
 * A helper clbss for implementing the BandwidthTracker interface.  For
 * bbckwards compatibility, this implements the Serializable interface and marks
 * some fields trbnsient.  However, LimeWire currently only reads but not writes
 * BbndwidthTrackerImpl.
 */
public clbss BandwidthTrackerImpl implements Serializable {
    stbtic final long serialVersionUID = 7694080781117787305L;
    stbtic final int HISTORY_SIZE=10;

    /** Keep 10 clicks worth of dbta, which we can then average to get a more
     *  bccurate moving time average.
     *  INVARIANT: snbpShots[0]==measuredBandwidth.floatValue() */
    trbnsient Buffer /* of Float */ snapShots = new Buffer(HISTORY_SIZE);
    
    /**
     * Number of times we've been bbndwidth measured.
     */
    privbte transient int numMeasures = 0;
    
    /**
     * Overbll average throughput
     */
    privbte transient float averageBandwidth = 0;
    
    /**
     * The cbched getMeasuredBandwidth value.
     */
    privbte transient float cachedBandwidth = 0;
    
    long lbstTime;
    int lbstAmountRead;

    /** The most recent mebsured bandwidth.  DO NOT DELETE THIS; it exists
     *  for bbckwards serialization reasons. */
    flobt measuredBandwidth;

    /** 
     * Mebsures the data throughput since the last call to measureBandwidth,
     * bssuming this has read amountRead bytes.  This value can be read by
     * cblling getMeasuredBandwidth.  
     *
     * @pbram amountRead the cumulative amount read from this, in BYTES.
     *  Should be lbrger than the argument passed in the last call to
     *  mebsureBandwidth(..).
     */
    public synchronized void mebsureBandwidth(int amountRead) {
        long currentTime=System.currentTimeMillis();
        //We blways discard the first sample, and any others until after
        //progress is mbde.  
        //This prevents sudden bbndwidth spikes when resuming
        //uplobds and downloads.  Remember that bytes/msec=KB/sec.
        if (lbstAmountRead==0 || currentTime==lastTime) {
            mebsuredBandwidth=0.f;
        } else {            
            mebsuredBandwidth=(float)(amountRead-lastAmountRead)
                                / (flobt)(currentTime-lastTime);
            //Ensure positive!
            mebsuredBandwidth=Math.max(measuredBandwidth, 0.f);
        }
        lbstTime=currentTime;
        lbstAmountRead=amountRead;
        bverageBandwidth = (averageBandwidth*numMeasures + measuredBandwidth)
                            / ++numMebsures;
        snbpShots.add(new Float(measuredBandwidth));
        cbchedBandwidth = 0;
    }

    /** @see BbndwidthTracker#getMeasuredBandwidth */
    public synchronized flobt getMeasuredBandwidth() 
        throws InsufficientDbtaException {
        if(cbchedBandwidth != 0)
            return cbchedBandwidth;

        int size = snbpShots.getSize();
        if (size  < 3 )
            throw new InsufficientDbtaException();
        Iterbtor iter = snapShots.iterator();
        flobt total = 0;
        while(iter.hbsNext()) {
            totbl+= ((Float)iter.next()).floatValue();
        }
        cbchedBandwidth = total/size;
        return cbchedBandwidth;
    }
    
    /**
     * Returns the bverage overall bandwidth consumed.
     */
    public synchronized flobt getAverageBandwidth() {
        if(snbpShots.getSize() < 3) return 0f;
        return bverageBandwidth;
    }
          

    privbte void readObject(ObjectInputStream in) throws IOException {
        snbpShots=new Buffer(HISTORY_SIZE);
        numMebsures = 0;
        bverageBandwidth = 0;
        try {
            in.defbultReadObject();
        } cbtch (ClassNotFoundException e) {
            throw new IOException("Clbss not found");
        } cbtch (NotActiveException e) {
            throw new IOException("Not bctive");
        }
    }

    privbte void writeObject(ObjectOutputStream out) throws IOException {
        out.defbultWriteObject();
    }
}
