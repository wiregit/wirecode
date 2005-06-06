package com.limegroup.gnutella.downloader;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.SystemUtils;

/** 
 * This SelectionStrategy uses user idle time, the number of previewable bytes
 * already downloaded, and a random factor to determine if it should hand the
 * assignment decission off to its super class, or simply extend the previewable
 * portion of the file by assigning the first suitible block.
 */
public class BiasedRandomDownloadStrategy extends RandomDownloadStrategy {
    
    private static final Log LOG = LogFactory.getLog(BiasedRandomDownloadStrategy.class);
    
    /**
     *  The minimum number of bytes for a reasonable preview.
     *  This is used as a goal for the random downloader code.
     */
    private static final int MIN_PREVIEW_BYTES = 1024 * 1024; //1 MB
    
    /**
     *  The minimum fraction of bytes for a reasonable preview.
     *  This is used as a goal for the random downloader code.
     */
    private static final float MIN_PREVIEW_FRACTION = 0.1f; // 10 percent of the file
    
    /**
     *  Once this fraction of the file is previewable, we switch to a fully
     *  random download strategy.
     */
    private static final float MAX_PREVIEW_FRACTION = 0.5f; // 50 percent of the file
    
    /**
     * Number of milliseconds the user has to be idle before being considered idle.
     * This factors into the download order strategy.
     */
    private static final int MIN_IDLE_MILLISECONDS = 5 * 60 * 1000; // 5 minutes
    
    public BiasedRandomDownloadStrategy(long fileSize) {
        super(fileSize);
    }
    
    public synchronized Interval pickAssignment(IntervalSet availableIntervals,
            long previewLength,
            long lastNeededByte,
            long blockSize) throws java.util.NoSuchElementException {
        // Input validation
        if (blockSize < 1)
            throw new IllegalArgumentException("Block size cannot be "+blockSize);
        if (previewLength < 0)
            throw new IllegalArgumentException("Preview length must be >= 0, "+previewLength+"<0");
        if (previewLength > lastNeededByte)
            throw new IllegalArgumentException("Preview length greater than last needed byte "+
                    previewLength+">"+lastNeededByte);
        
        // Determine if we should return a uniformly distributed Interval
        // or the first interval.
        // Note that the specification for java.util.Random.nextFloat() explicitly states that
        // the returned value will be strictly less than 1.0.  Therefore, if getBiasProbability()
        // returns 1.0, nextFloat() < getBiasProbability() with probability 100%.  Other behavior
        // indicates a bug in java.util.Random.
        if (SystemUtils.getIdleTime() >= MIN_IDLE_MILLISECONDS // If the user is idle, always use random strategy
                || pseudoRandom.nextFloat() >= getBiasProbability(previewLength, completedSize)) {
            return super.pickAssignment(availableIntervals, previewLength, lastNeededByte, blockSize);
        }
        
        Interval candidate = availableIntervals.getFirst();

        // Calculate what the high byte offset should be.
        // This will be at most blockSize-1 bytes greater than the low.
        // Skip ahead one block.
        long alignedHigh = candidate.low + blockSize;
        // Cut back to the aligned boundary.
        alignedHigh -= alignedHigh % blockSize;
        // Step back one byte from the boundary.
        alignedHigh -= 1;

        // n % blockSize < blockSize
        // Therefore (n % blockSize) + 1 <= blockSize
        // Therefore n-blockSize-(n % blockSize)-1 >= n
        // Therefore alignedHigh >= candidate.low, and we
        // only have to check if alignedHigh > candidate.high.
        if (alignedHigh > candidate.high) {
            alignedHigh = candidate.high;
        }

        // Our ideal interval is [candidate.low, alignedHigh]
        
        // Optimize away creation of new objects, if possible
        Interval ret = candidate;
        if (ret.high != alignedHigh)
            ret = new Interval(candidate.low, alignedHigh);

        // Log it
        if (LOG.isDebugEnabled()) {
            LOG.debug("Non-random download, range=" + ret + " out of choices "
                    + availableIntervals);
        }

        // return
        return ret;
    }

    
    /////////////////// Private Helper Methods ////////////////////////////
    
    /**
     * Calculates the probability that the next block assigned should be
     * guaranteed to be from the beginning of the file. This is calculated as a
     * step function that is at 100% until max(MIN_PREVIEW_BYTES,
     * MIN_PREVIEW_FRACTION * completedSize), then drops down to 50% until
     * MAX_PREVIEW_FRACTION of the file is downloaded. Above
     * MAX_PREVIEW_FRACTION, the function returns 0%, indicating that a fully
     * random downloading strategy should be used.
     * 
     * @return the probability that the next chunk should be forced to be
     *         downloaded from the beginning of the file.
     */
    private float getBiasProbability(long previewBytesDownloaded, long fileSize) {
        long goal = Math.max((long)MIN_PREVIEW_BYTES, (long)(MIN_PREVIEW_FRACTION * fileSize));
        // If we don't have our goal yet, devote 100% of resources to extending 
        // the previewable length
        if (previewBytesDownloaded < goal) 
            return 1.0f;
        
        // If we have less than the cutoff (currently 50% of the file) 
        if (previewBytesDownloaded < MAX_PREVIEW_FRACTION * fileSize)
            return 0.5f;
        
        return 0.0f;
    }
}
