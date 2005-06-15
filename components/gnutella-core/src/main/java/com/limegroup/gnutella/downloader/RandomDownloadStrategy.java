package com.limegroup.gnutella.downloader;

import java.util.Random;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.IntervalSet;

/** 
 * This SelectionStrategy selects random Intervals from the availableIntervals.
 * 
 * This SelectionStrategy does so by using 16 pseudorandom locations 
 * (offsets into the file), pseudorandomly selects one of the 16 offsets, 
 * and downloads the first available aligned chunk after the offset.  This balances
 * the need to minimize the number of Intervals in VerifyingFile 
 * (for a small download.dat file) against the need for a uniform distribution of 
 * downloaded chunks over all hosts (to increase the availability of the rarest chunk).
 * 
 * For network efficiency, the random offsets are aligned to chunk boundaries.  Changing
 * the chunk size does not, however, cause any random locations to be re-calculated.
 * 
 * Random locations are replaced with new random locations only when they cease to 
 * fall between previewLength and lastNeededByte.
 * 
 * If a particular source cannot provide bytes after the selected random location,
 * the block closest before the random location is selected instead.
 */
public class RandomDownloadStrategy implements SelectionStrategy {
    
    private static final Log LOG = LogFactory.getLog(RandomDownloadStrategy.class);
    
    /**
     * A global pseudorandom number generator. We don't really care about values 
     * duplicated across threads, so don't bother serializing access.
     * 
     * This really should be final, except that making it non-final makes tests
     * much more simple.
     */
    protected static Random pseudoRandom = new Random();

    /**
     * A list of the 16 "random" locations in the file from which the random downloader chooses.
     * This is done in order to minimize fragmentation (and therefore size) of the IntervalSets 
     * that must be stored to disk by VerifyingFile.
     * 
     * These are lazily updated to be after the end of the first contiguous range of assigned blocks.
     */
    private final long[] randomLocations = new long[16];
    
    /** The size the download will be once completed. */
    protected final long completedSize;
    
    public RandomDownloadStrategy(long completedSize) {
        super();
        this.completedSize = completedSize;
        // Set all randomLocations to be less than the minimum lowerBound,
        // so that they will be lazily updated. 
        for(int i=randomLocations.length - 1; i >= 0; i--)
            randomLocations[i] = -1L;
    }
    
    /**
     * Picks a random block of a file to download next.
     * 
     * For efficiency reasons attempts will be made to align the start and end of 
     * intervals to block boundaries.  However, there are no guarantees on alignment.
     * 
     * @param availableIntervals a representation of the set of 
     *      bytes available for download from a given server, minus
     *      the set of bytes that we already have (or have assigned)
     * @param lowerBound the number of contiguous bytes from the 
     *      beginning of the file that have already been assigned 
     *      (and will presumably soon be available for preview)
     * @param upperBound all bytes after lastNeededByte have been assigned for
     *      download.  Note that this information may not be availbable
     *      from availableIntervals, since availableIntervals may contain server-specific
     *      information.
     * @param fileSize the total length of the file being downloaded
     * @param blockSize the maximum size of the returned Interval. Any values less than 1 will
     *      be ignared.  An attempt will be made to make the high end of the interval one less
     *      than a multiple of blockSize.  Any values less than 1 will generate IllegalArgumentExceptions.
     * @return the Interval that should be assigned next, with a size of at most blockSize bytes
     * @throws NoSuchElementException if passed an empty IntervalSet
     */
    public synchronized Interval pickAssignment(IntervalSet availableIntervals,
            long lowerBound,
            long upperBound,
            long blockSize) throws java.util.NoSuchElementException {
        if (blockSize < 1)
            throw new IllegalArgumentException("Block size cannot be "+blockSize);
        if (lowerBound < 0)
            throw new IllegalArgumentException("lowerBound must be >= 0, "+lowerBound+"<0");
        if (upperBound >= completedSize)
            throw new IllegalArgumentException("upperBound must be less than completedSize "+
                    upperBound+" >= "+completedSize);
        if (lowerBound > upperBound)
            throw new IllegalArgumentException("lowerBound greater than upperBound "+
                    lowerBound+" > "+upperBound);
        if (availableIntervals.isEmpty())
            throw new NoSuchElementException();
            
        // Which random range should be extended?
        int randomIndex = pseudoRandom.nextInt() & 0xF; // integer [0 15]
            
        if (randomLocations[randomIndex] < lowerBound || 
                randomLocations[randomIndex] > upperBound)
            // Make the random location somewhere between the first and last bytes we still need to assign
            randomLocations[randomIndex] = getRandomLocation(lowerBound, upperBound, blockSize);
        
        // The lowest start location of an ideally matched interval.
        long randomPoint = randomLocations[randomIndex];
       
        // The first properly aligned interval, returned in the case that
        // there are no aligned intervals available after lowerBound
        Interval lastSuitableInterval = null;
        
        Iterator intervalIterator = availableIntervals.getAllIntervals();
        // Get the first alligned range after a random place in the file
    
        Interval optimalInterval = null;
        while (intervalIterator.hasNext()) {
            optimalInterval = optimizeInterval((Interval) intervalIterator.next(),
                    randomPoint, blockSize);
            // Check if it really is optimal
            if (optimalInterval.low >= randomPoint) {
                // Lazily update any randomLocations that have coalesced 
                // with this randomLocation.  randomPoint <= optimalInterval.high
                clearRandomLocationsBetween(randomPoint, optimalInterval.high, randomIndex);
                return optimalInterval; 
            }
        }
        // optimalInterval really isn't optimal.  We've settled for the last interval
        // and it is still before randomPoint
       
        // Lazily update any randomLocations that have coalesced with this randomLocation
        // randomPoint > optimalInterval.low
        clearRandomLocationsBetween(optimalInterval.low, randomPoint, randomIndex);
       return optimalInterval;
    }

    
    ///////////////////// Private Helper Methods /////////////////////////////////
    /** Aligns location to one byte before the next highest block boundary */
    protected long alignHigh(long location, long blockSize) {
        location += blockSize;
        location -= location % blockSize;
        return location - 1;
    }
    
    /** Aligns location to the nearest block boundary that is at or before location */
    protected long alignLow(long location, long blockSize) {
        location -= location % blockSize;
        return location;
    }
    
    /**
     * Sets randomLocations between lowBound and highBound (inclusive) to
     * -1, except for the randomLocation at excludeIndex.
     * 
     * This will cause the affected randomLocations to be lazily updated
     * next time they are used, since -1 is before all legal lower bounds
     * passed to pickAssignment.
     * 
     * @param lowBound
     * @param highBound
     * @param excludeIndex
     */
    private void clearRandomLocationsBetween(long lowBound, long highBound, 
            int excludeIndex) {
        for(int index=randomLocations.length-1; index >= 0; index--) {
            if(randomLocations[index] >= lowBound &&
                    randomLocations[index] <= highBound &&
                    index != excludeIndex)
                randomLocations[index] = -1L;
        }
    }
    
    /** Returns candidate or a sub-interval of candidate that best 
     * fits the following goals:
     * 
     * 1.a) returnInterval.low >= location
     * 1.b) returnInterval.low is as close to location as possible
     * 1.c) returnInterval does not span a blockSize boundary
     * 1.d) returnInterval is as large as possible without violating 1.a-1.c
     * 
     * failing 1.a, the secondary goals become most important:
     * 2.a) returnInterval.high <= location
     * 2.b) returnInterval.high is as close to location as possible
     * 2.c) returnInterval does not span a blockSize boundary
     * 2.d) returnInterval is as large as possible without violating 2.a-2.c
     */
    private Interval optimizeInterval(Interval candidate,
            long location, long blockSize) {
        
            // Calculate the most suitable low value contained
            // in candidate.
            long bestLow = candidate.low;
            if (bestLow < location && candidate.high >= location) {
                bestLow = location;
            }
            
            // See if goal 1.a is violated
            if (bestLow >= location) {
                // Calculate the most suitable high byte based on goal 1.c 
                // This will be at most blockSize-1 bytes greater than bestLow
                long bestHigh = alignHigh(bestLow,blockSize);
      
                if (bestHigh > candidate.high)
                    bestHigh = candidate.high;
                
                if (candidate.high == bestHigh && candidate.low == bestLow)
                    return candidate;
                return new Interval(bestLow,bestHigh);
            } else {
                // candidate.high < location
                // Goal 1.a cannot be met, so move on to secondary goals
                bestLow = alignLow(candidate.high, blockSize);
                // Adjust low to be within candidate
                if (bestLow < candidate.low)
                    bestLow = candidate.low;
                
                if (bestLow == candidate.low) {
                    return candidate;
                } else {
                    return new Interval(bestLow, candidate.high);
                }
            }   
    }
    
    /**
     * Calculates a random block-aligned byte offset into the file, 
     * at least minIndex bytes into the file.  If minIndex is less than blockSize
     * from maxIndex, minIndex will be returned, regardless of its alignment.
     * 
     * This function is safe for files larger than 2 GB, files with chunks larger than 2 GB,
     * and files containing more than 2 GiBi chunks.
     * 
     * This function is practically unbiased for files smaller than several terabytes.
     */
    private long getRandomLocation(long minIndex, long maxIndex, long blockSize) {
        // If minIndex is in the middle of a block, include the
        // beginning of that block.
        long minBlock = minIndex / blockSize;
        // If maxIndex is in the middle of a block, include that
        // partial block in our range
        long maxBlock = maxIndex / blockSize;
        
        // This may happen if there is only one block available to be assigned. 
        // ... just give back the minIndex
        if (minBlock >= maxBlock)
            return minIndex;  //No need to align the last partial block
        
        // Generate a random blockNumber on the range [minBlock, maxBlock]
        // return blockSize * blockNumber
        return blockSize * (minBlock + Math.abs(pseudoRandom.nextLong() % (maxBlock-minBlock+1)));
    }
}
