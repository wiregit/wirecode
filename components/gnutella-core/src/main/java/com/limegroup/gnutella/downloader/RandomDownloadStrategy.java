package com.limegroup.gnutella.downloader;

import java.util.Random;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.SystemUtils;

public class RandomDownloadStrategy implements SelectionStrategy {
    
    private static final Log LOG = LogFactory.getLog(RandomDownloadStrategy.class);
    
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
    
    /**
     * A global pseudorandom number generator.  At least according to the Java 1.4.2 spec,
     * it will be seeded using System.getTimeMillis().
     * 
     * We don't really care about values duplicated across threads, so don't bother
     * serializing access.
     */
    private static final Random pseudoRandom = new Random();

    /**
     * A list of the 16 "random" locations in the file from which the random downloader chooses.
     * This is done in order to minimize fragmentation (and therefore size) of the IntervalSets 
     * that must be stored to disk by VerifyingFile.
     * 
     * These are lazily updated to be after the end of the first contiguous range of assigned blocks.
     */
    private final long[] randomLocations = new long[16];
    
    private RandomDownloadStrategy() {
        super();
    }
    
    public static SelectionStrategy create() {
        return new RandomDownloadStrategy();
    }
    
    public synchronized Interval pickAssignment(IntervalSet availableIntervals,
            long previewLength, long lastNeededByte, long fileSize, long blockSize) throws java.util.NoSuchElementException {
      
        // The lowest start location of an ideally matched interval.
        // We could re-use previewLength, but the code is easier to read this way.
        long lowerBound = previewLength;
        
        // Perhaps the lower range should be random
        int randomIndex = -1;
        
        if (SystemUtils.getIdleTime() > MIN_IDLE_MILLISECONDS // If the user is idle, always use random strategy
                || pseudoRandom.nextFloat() > getBiasProbability(previewLength, fileSize)) {
            randomIndex = pseudoRandom.nextInt() & 0xF; // integer [0 15]
            // Lazy update of the random location
            if (randomLocations[randomIndex] <= previewLength || randomLocations[randomIndex] > lastNeededByte) {
                // If the "random" location is zero, it has never been initialized,
                // and we should try to align it to with the Nth available fragment
                if (randomLocations[randomIndex] == 0 && availableIntervals.getAllIntervalsAsList().size() > randomIndex) {
                    randomLocations[randomIndex] = ((Interval)availableIntervals.getAllIntervalsAsList().get(randomIndex)).low; 
                } else {
                    // Make the random location somewhere between the first and last bytes we still need to assign
                    randomLocations[randomIndex] = getRandomLocation(previewLength, lastNeededByte, blockSize);
                }
            }
            lowerBound = randomLocations[randomIndex];
        }
       
        // The first properly aligned interval, returned in the case that
        // there are no aligned intervals available after lowerBound
        Interval backupInterval = null;
        
        Iterator intervalIterator = availableIntervals.getAllIntervals();
        // Get all of the ranges after a random place in the file
        if (blockSize <= 0) {
            // We're not picky about the block size or alignment.  This is the easy case
            while (intervalIterator.hasNext()) {
                Interval candidate = (Interval) intervalIterator.next();
                if (candidate.low <= lowerBound)
                    return candidate;
                if (candidate.high <= lowerBound)
                    return new Interval((int)lowerBound, candidate.high);
                if (backupInterval == null)
                    backupInterval = candidate;
            }
        } else {
            // We care about alignment and block size... this is a bit more complicated
            while (intervalIterator.hasNext()) {
                Interval candidate = (Interval) intervalIterator.next();
                // Calculate the most useful low index for this range,
                // where the primary criterion is bestLow >= lowerBound
                // and lower bestLows are preferable.
                long bestLow = candidate.low;
                if (bestLow > lowerBound && candidate.high <= lowerBound) {
                    // bestLow can be moved up
                    bestLow = lowerBound;
                }
                
                // Calculate what the high byte offset should be.
                // This will be at most blockSize-1 bytes greater than bestLow
                // Skip ahead one block
                long alignedHigh = bestLow+blockSize;
                // Cut back to the aligned boundary
                alignedHigh -= alignedHigh % blockSize;
                // Step back one byte from the boundary
                alignedHigh -= 1;

                // Is it after lowerBound ?
                if (bestLow >= lowerBound) {
                    // We've found our ideal location.  Log it and return
                        
                    // Log it
                    if (LOG.isDebugEnabled()) {
                        if (lowerBound > previewLength) {
                            LOG.debug("Random download, index="+randomIndex+
                                    ", random location="+randomLocations[randomIndex]+
                                    ", range=["+bestLow+","+alignedHigh+
                                    "] out of choices "+availableIntervals);
                        } else {
                            LOG.debug("Non-random download, range=["+bestLow+","+
                                    alignedHigh+"] out of choices "+availableIntervals);
                        }
                    }
                
                    // return
                    if (candidate.high == alignedHigh && candidate.low == bestLow)
                        return candidate;
                    return new Interval((int)bestLow, (int)(alignedHigh));
                } else if (backupInterval == null){
                    // Store the first backup 
                    if (candidate.high == alignedHigh && candidate.low == bestLow) {
                        backupInterval = candidate;
                    } else {
                            backupInterval = new Interval((int)bestLow, (int)alignedHigh);
                    }
                } // close of lowerBound check if-else code block
            } // close of Iterator loop
        }  // close of blockSize <= 0 if-else code block
        
        // If we had found the ideal block, we would have returned from within one
        // of the Iterator loops.
        
        if (backupInterval == null)
            throw new NoSuchElementException();
        
        // The only way to get here is if we have selected a random lowerBound,
        // and there are no suitible Intervals at or after lowerBound.
        // Note that this may only be the case for this particular server,
        // so it's not necessarily the case that lowerBound > lastNeededByte.
        // Therefore, do not lazily update the random location here.
        
        // Log it
        if (LOG.isDebugEnabled()) {
                LOG.debug("Wrapping random download to beginning, index="+randomIndex+
                        ", random location="+randomLocations[randomIndex]+
                        ", range="+ backupInterval +
                        " out of choices "+availableIntervals);
        }
        
        return backupInterval;
    }

    
    /**
     * Calculates the probability that the next block assigned should be guaranteed to be from the beginning of the file.
     * 
     * This is calculated as a step function that is at 100% until max(MIN_PREVIEW_BYTES, MIN_PREVIEW_FRACTION * completedSize), 
     * then drops down to 50% until MAX_PREVIEW_FRACTION of the file is downloaded.  
     * Above MAX_PREVIEW_FRACTION, the function returns 0%, indicating
     * that a fully random downloading strategy should be used.
     * 
     * @return the probability that the next chunk should be forced to be downloaded from the beginning of the file.
     */
    private float getBiasProbability(long previewBytesDownloaded, long fileSize) {
        long goal = Math.max((long)MIN_PREVIEW_BYTES, (long)(MIN_PREVIEW_FRACTION * fileSize));
        // If we don't have our goal yet, devote 100% of resources to extending the previewable length
        if (previewBytesDownloaded < goal) 
            return 1.0f;
        
        // If we have less than the cutoff (currently 50% of the file) 
        if (previewBytesDownloaded < MAX_PREVIEW_FRACTION * fileSize)
            return 0.5f;
        
        return 0.0f;
    }
    
    /**
     * Calculates a random block-aligned byte offset into the file, 
     * at least minIndex bytes into the file.  If minIndex is less than blockSize
     * from maxIndex, minIndex will be returned, regardless of its alignment.
     * 
     * This function is safe for files larger than 2 GB, files with chunks larger than 2 GB,
     * and files containing more than 2 GiBi chunks.
     * 
     * This function is also imperceptibly biased; the bias is approximately
     * one part in (9e+18 / (downloadedSize/chunkSize)), which is vanishlingly small, 
     * even for several terabyte files with chunk sizes of 1024 bytes.
     */
    private long getRandomLocation(long minIndex, long maxIndex, long blockSize) {
        if (minIndex > maxIndex)
            throw new IndexOutOfBoundsException();
        if (minIndex < 0)
            throw new IndexOutOfBoundsException();
        long minBlock = (minIndex+blockSize-1) / blockSize;
        long maxBlock = (maxIndex) / blockSize;
        
        // This will happen if the last block to assign is the 
        // last block of the file and the file is not an exact multiple of
        // the block size ... just give back the minIndex
        if (minBlock >= maxBlock)
            return minIndex;
        
        // Generate a random blockNumber on the range [minBlock, maxBlock]
        // return blockSize * blockNumber
        // (nextLong() >>> 1) is a 63-bit pseudorandom positive long
        return blockSize * (minBlock + ((pseudoRandom.nextLong() >>> 1) % (maxBlock-minBlock+1)));
    }
    
}
