package com.limegroup.gnutella.downloader;

import java.util.Random;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.IntervalSet;

/** 
 * This SelectionStrategy uses 16 pseudorandom locations (offsets into the file), 
 * pseudorandomly selects one of the 16 offsets, and downloads the first available 
 * aligned chunk after the offset.  This balances the need to minimize the number 
 * of Intervals in VerifyingFile (for a small download.dat file) against the need
 * for a uniform distribution of downloaded chunks over all hosts (to increase the 
 * availability of the rarest chunk).
 * 
 * For efficiency, the random offsets are aligned to chunk boundaries.  Changing
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
     * A global pseudorandom number generator.  At least according to the Java 1.4.2 spec,
     * it will be seeded using System.getTimeMillis().
     * 
     * We don't really care about values duplicated across threads, so don't bother
     * serializing access.
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
    protected long completedSize;
    
    public RandomDownloadStrategy(long completedSize) {
        super();
        this.completedSize = completedSize;
    }
    
    /**
     * Encapsulates a pseudorandom uniformly distributed algorithm for deciding which block of 
     * a file to download next.
     * 
     * For efficiency reasons attempts will be made to align the start and end of intervals
     * to block boundaries.  However, there are no guarantees on alignment.
     * 
     * 
     * @param availableIntervals a representation of the set of 
     *      bytes available for download from a given server
     * @param previewLength the number of contiguous bytes from the 
     *      beginning of the file that have already been assigned 
     *      (and will presumably soon be available for preview)
     * @param lastNeededByte all bytes after lastNeededByte have been assigned for
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
            long previewLength,
            long lastNeededByte,
            long blockSize) throws java.util.NoSuchElementException {
        if (blockSize < 1)
            throw new IllegalArgumentException("Block size cannot be "+blockSize);
        if (previewLength < 0)
            throw new IllegalArgumentException("Preview length must be >= 0, "+previewLength+"<0");
        if (previewLength > lastNeededByte)
            throw new IllegalArgumentException("Preview length greater than last needed byte "+
                    previewLength+">"+lastNeededByte);
            
        // Which random range should be extended?
        int randomIndex = pseudoRandom.nextInt() & 0xF; // integer [0 15]
            
        // Lazy update of the random location
        if (randomLocations[randomIndex] == 0) {
            // If the "random" location is zero, it has never been initialized
            // or will point to the first block and then be randomly updated
            // next time around, after the first block has been assigned.
            // We should try to align it to with the Nth available fragment
            if (availableIntervals.getAllIntervalsAsList().size() > randomIndex) {
                randomLocations[randomIndex] = ((Interval) availableIntervals
                        .getAllIntervalsAsList().get(randomIndex)).low;

                // Undo the heavy bias this creates towards the first block of
                // the file in freshly started downloads.
                // Note that if completedSize <= blockSize, it's a moot point as
                // there's only one block to pick.
                if (randomLocations[randomIndex] == 0
                        && pseudoRandom.nextFloat() > ((float) blockSize)/completedSize)
                    randomLocations[randomIndex] = getRandomLocation(previewLength,
                            lastNeededByte, blockSize);
            } else {
                // Definitely uninitialized, so initialize
                randomLocations[randomIndex] = getRandomLocation(previewLength,
                        lastNeededByte, blockSize);
            }
        } else if (randomLocations[randomIndex] < previewLength || 
                randomLocations[randomIndex] > lastNeededByte) {
            // Make the random location somewhere between the first and last bytes we still need to assign
            randomLocations[randomIndex] = getRandomLocation(previewLength, lastNeededByte, blockSize);
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
                // If randomPoint < candidate.high, then earlier code would
                // have stepped bestLow >= randomPoint, and we would have
                // returned.  Therefore, candidate.high is now our best
                // high point.
                
                // step the low point backward to the block boundary
                bestLow = candidate.high-(candidate.high % blockSize);
                // Adjust bestLow to be within the interval
                // We already know bestLow <= bestHigh <= candidate.high
                if (bestLow < candidate.low)
                    bestLow = candidate.low;
                
                if (bestLow == candidate.low) {
                    lastSuitableInterval = candidate;
                } else {
                    lastSuitableInterval = new Interval(bestLow, candidate.high);
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
        //
        // Note that pseudoRandom.nextLong() & (-1L >>> 1) is a uniform distribution
        // over all non-negative longs.
        return blockSize * (minBlock + ((pseudoRandom.nextLong() & (-1L >>> 1)) % (maxBlock-minBlock+1)));
    }
}
