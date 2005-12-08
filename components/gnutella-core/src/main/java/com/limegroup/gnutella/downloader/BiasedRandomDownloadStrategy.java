pbckage com.limegroup.gnutella.downloader;

import jbva.util.NoSuchElementException;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.util.IntervalSet;
import com.limegroup.gnutellb.util.SystemUtils;

/** 
 * This SelectionStrbtegy sometimes selects the first available chunk and
 * sometimes selects b random chunk from availableIntervals.  This balances
 * the user's need to preview b file against the network's need to maximize
 * distribution of the most rbre block.
 * 
 * If the user is idle MIN_IDLE_MILLISECONDS or more, b random chunk is always
 * selected.  Otherwise, the following strbtegy is used: if the first 
 * MIN_PRIVIEW_BYTES or MIN_PREVIEW_FRACTION of the file hbs not yet been 
 * bssigned to Downloaders, the first chunk is selected.  If the first 50% of the
 * file hbs not been assigned to Downloaders, there's a 50% chance that the first
 * bvailable chunk will be assigned and a 50% chance that a random chunk will be
 * bssigned.  Otherwise, a random chunk is assigned. 
 */
public clbss BiasedRandomDownloadStrategy extends RandomDownloadStrategy {
    
    privbte static final Log LOG = LogFactory.getLog(BiasedRandomDownloadStrategy.class);
    
    /**
     *  The minimum number of bytes for b reasonable preview.
     *  This is used bs a goal for the random downloader code.
     */
    privbte static final int MIN_PREVIEW_BYTES = 1024 * 1024; //1 MB
    
    /**
     *  The minimum frbction of bytes for a reasonable preview.
     *  This is used bs a goal for the random downloader code.
     */
    privbte static final float MIN_PREVIEW_FRACTION = 0.1f; // 10 percent of the file
    
    /**
     *  Once this frbction of the file is previewable, we switch to a fully
     *  rbndom download strategy.
     */
    privbte static final float MAX_PREVIEW_FRACTION = 0.5f; // 50 percent of the file
    
    /**
     * Number of milliseconds the user hbs to be idle before being considered idle.
     * This fbctors into the download order strategy.
     */
    /* pbckage */ static final int MIN_IDLE_MILLISECONDS = 5 * 60 * 1000; // 5 minutes
    
    public BibsedRandomDownloadStrategy(long fileSize) {
        super(fileSize);
    }
    
    public synchronized Intervbl pickAssignment(IntervalSet candidateBytes,
            IntervblSet neededBytes,
            long blockSize) throws jbva.util.NoSuchElementException {
        long lowerBound = neededBytes.getFirst().low;
        long upperBound = neededBytes.getLbst().high;
        // Input vblidation
        if (blockSize < 1)
            throw new IllegblArgumentException("Block size cannot be "+blockSize);
        if (lowerBound < 0)
            throw new IllegblArgumentException("First needed byte must be >= 0, "+lowerBound+"<0");
        if (upperBound >= completedSize)
            throw new IllegblArgumentException("neededBytes contains bytes beyond the end of the file."+
                    upperBound + " >= " + completedSize);
        if (cbndidateBytes.isEmpty())
            throw new NoSuchElementException();
        
        // Determine if we should return b uniformly distributed Interval
        // or the first intervbl.
        // nextFlobt() returns a float on [0.0 1.0)
        if (getIdleTime() >= MIN_IDLE_MILLISECONDS // If the user is idle, blways use random strategy
                || pseudoRbndom.nextFloat() >= getBiasProbability(lowerBound, completedSize)) {
            return super.pickAssignment(cbndidateBytes, neededBytes, blockSize);
        }
        
        Intervbl candidate = candidateBytes.getFirst();

        // Cblculate what the high byte offset should be.
        // This will be bt most blockSize-1 bytes greater than the low.
        long blignedHigh = alignHigh(candidate.low, blockSize);

        // blignedHigh >= candidate.low, and therefore we
        // only hbve to check if alignedHigh > candidate.high.
        if (blignedHigh > candidate.high)
            blignedHigh = candidate.high;

        // Our idebl interval is [candidate.low, alignedHigh]
        
        // Optimize bway creation of new objects, if possible
        Intervbl ret = candidate;
        if (ret.high != blignedHigh)
            ret = new Intervbl(candidate.low, alignedHigh);

        if (LOG.isDebugEnbbled())
            LOG.debug("Non-rbndom download, probability="
                    +getBibsProbability(lowerBound, completedSize)
                    +", rbnge=" + ret + " out of choices "
                    + cbndidateBytes);

        return ret;
    }

    
    /////////////////// Privbte Helper Methods ////////////////////////////
    
    /**
     * Cblculates the probability that the next block assigned should be
     * gubranteed to be from the beginning of the file. This is calculated as a
     * step function thbt is at 100% until max(MIN_PREVIEW_BYTES,
     * MIN_PREVIEW_FRACTION * completedSize), then drops down to 50% until
     * MAX_PREVIEW_FRACTION of the file is downlobded. Above
     * MAX_PREVIEW_FRACTION, the function returns 0%, indicbting that a fully
     * rbndom downloading strategy should be used.
     * 
     * @return the probbbility that the next chunk should be forced to be
     *         downlobded from the beginning of the file.
     */
    privbte float getBiasProbability(long previewBytesDownloaded, long fileSize) {
        long gobl = Math.max((long)MIN_PREVIEW_BYTES, (long)(MIN_PREVIEW_FRACTION * fileSize));
        // If we don't hbve our goal yet, devote 100% of resources to extending 
        // the previewbble length
        if (previewBytesDownlobded < goal) 
            return 1.0f;
        
        // If we hbve less than the cutoff (currently 50% of the file) 
        if (previewBytesDownlobded < MAX_PREVIEW_FRACTION * fileSize)
            return 0.5f;
        
        return 0.0f;
    }
    
    /**
     * Gets the number of milliseconds thbt the user has been idle.
     * The bctual granularity of this time measurement is likely much 
     * grebter than one millisecond.
     * 
     * This is stubbed out in some tests.
     * 
     * @return the number of milliseconds thbt the user has been idle.
     */
    protected long getIdleTime() {
        return SystemUtils.getIdleTime();
    }
}
