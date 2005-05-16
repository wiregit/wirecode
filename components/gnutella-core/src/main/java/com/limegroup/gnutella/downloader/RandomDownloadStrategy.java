package com.limegroup.gnutella.downloader;

import java.util.Random;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.IntervalSet;

/** 
 * This SelectionStrategy uses 16 uniformly distributed download points in order to
 * balance the need to minimize the number of Intervals in VerifyingFile (for a 
 * small download.dat file) against the need for a uniform distribution of downloaded
 * chunks over all hosts (to increase the availability of the rarest chunk).
 */
public class RandomDownloadStrategy implements SelectionStrategy {
    
    private static final Log LOG = LogFactory.getLog(RandomDownloadStrategy.class);
    
    /**
     * A global pseudorandom number generator.  At least according to the Java 1.4.2 spec,
     * it will be seeded using System.getTimeMillis().
     * 
     * We don't really care about values duplicated across threads, so don't bother
     * serializing access.
     */
    protected static final Random pseudoRandom = new Random();

    /**
     * A list of the 16 "random" locations in the file from which the random downloader chooses.
     * This is done in order to minimize fragmentation (and therefore size) of the IntervalSets 
     * that must be stored to disk by VerifyingFile.
     * 
     * These are lazily updated to be after the end of the first contiguous range of assigned blocks.
     */
    private final long[] randomLocations = new long[16];
    
    protected long completedSize;
    
    public RandomDownloadStrategy(long completedSize) {
        super();
        this.completedSize = completedSize;
    }
    
    public synchronized Interval pickAssignment(IntervalSet availableIntervals,
            long previewLength,
            long lastNeededByte,
            long blockSize) throws java.util.NoSuchElementException {
        if (blockSize < 1)
            throw new IllegalArgumentException("Block size cannot be "+blockSize);
        
        // Which random range should be extended?
        int randomIndex = pseudoRandom.nextInt() & 0xF; // integer [0 15]
            
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
        
        // The lowest start location of an ideally matched interval.
        long randomPoint = randomLocations[randomIndex];
       
        // The first properly aligned interval, returned in the case that
        // there are no aligned intervals available after lowerBound
        Interval lastSuitableInterval = null;
        
        Iterator intervalIterator = availableIntervals.getAllIntervals();
        // Get the first alligned range after a random place in the file
    
        while (intervalIterator.hasNext()) {
            Interval candidate = (Interval) intervalIterator.next();
            // Calculate the most useful low index for this range,
            // where the primary criterion is bestLow >= randomPoint
            // and lower bestLows are preferable (as a second criterion).
            long bestLow = candidate.low;
            if (bestLow < randomPoint && candidate.high >= randomPoint) {
                bestLow = randomPoint;
            }
                
            // Calculate what the high byte offset should be.
            // This will be at most blockSize-1 bytes greater than bestLow
            // Skip ahead one block
            long bestHigh = bestLow+blockSize;
            // Cut back to the aligned boundary
            bestHigh -= bestHigh % blockSize;
            // Step back one byte from the boundary
            bestHigh -= 1;

            // Is it after our random lower bound ?
            if (bestLow >= randomPoint) {
                // We've found our ideal location.  Log it and return
                
                // Log it
                if (LOG.isDebugEnabled()) { 
                    LOG.debug("Random download, index="+randomIndex+
                            ", random location="+randomLocations[randomIndex]+
                            ", range=["+bestLow+","+bestHigh+
                            "] out of choices "+availableIntervals); 
                }
                
                // return
                if (candidate.high == bestHigh && candidate.low == bestLow) {
                    return candidate;
                }
                if (bestHigh < candidate.high)
                    return new Interval(bestLow, bestHigh);
                return new Interval(bestLow,candidate.high);
            } else {
                // End as high as possible
                bestHigh = randomPoint;
                // Adjust bestHigh to be within the interval
                if (bestHigh > candidate.high)
                    bestHigh = candidate.high;
                
                // step the low point backward to the block boundary
                bestLow = bestHigh-(bestHigh % blockSize);
                // Adjust bestLow to be within the interval
                // We already know bestLow <= bestHigh <= candidate.high
                if (bestLow < candidate.low)
                    bestLow = candidate.low;
                
                if (bestLow == candidate.low && bestHigh == candidate.high) {
                    lastSuitableInterval = candidate;
                } else {
                    lastSuitableInterval = new Interval(bestLow, bestHigh);
                }
            } // close of lowerBound check if-else code block
        } // close of Iterator loop
        
        // If we had found the ideal block, we would have returned from within one
        // of the Iterator loops.
        
        if (lastSuitableInterval == null)
            throw new NoSuchElementException();
        
        // The only way to get here is if we have selected a random lowerBound,
        // and there are no suitible Intervals at or after lowerBound.
        // Note that this may only be the case for this particular server,
        // so it's not necessarily the case that lowerBound > lastNeededByte.
        // Therefore, do not lazily update the random location here.
        
        // Log it
        LOG.debug("Picking last block before random download point, index="+
                randomIndex+", random location="+
                randomLocations[randomIndex]+
                ", range="+ lastSuitableInterval +
                " out of choices "+availableIntervals);
        // return
        return lastSuitableInterval;
    }

    
    ///////////////////// Private Helper Methods /////////////////////////////////
    
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
        // Block number maxIndex/blockSize will always result in the random
        // selection wrapping, so maxBlock is one less than this value.
        long maxBlock = (maxIndex / blockSize) - 1;
        
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
