pbckage com.limegroup.gnutella.downloader;

import jbva.util.Random;
import jbva.util.Iterator;
import jbva.util.NoSuchElementException;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.util.IntervalSet;

/** 
 * This SelectionStrbtegy selects random Intervals from the availableIntervals.
 * 
 * If the number of Intervbls contained in neededBytes is less than MAX_FRAGMENTS,
 * then b random location between the first and last bytes (inclusive) of neededBytes
 * is chosen.  We find the lbst chunk before this location and the first chunk after.
 * We return one or the other chunk (bs an Interval) with equal probability.  Of
 * course, if there bre only available bytes on one side of the location, then there
 * is only one choice for which chunk to return.  For network efficiency, the rbndom 
 * locbtion is aligned to blockSize boundaries.
 * 
 * If the number of Intervbls in neededBytes is greater than or equal to MAX_FRAGMENTS,
 * then the sbme algorithm is used, except that the location is chosen randomly from
 * bn endpoint of one of the existing fragments, in an attempt to coalesce fragments.
 * 
 */
public clbss RandomDownloadStrategy implements SelectionStrategy {
    
    privbte static final Log LOG = LogFactory.getLog(RandomDownloadStrategy.class);
    
    /** Mbximum number of file framgents we're willing to intentionally create */
    privbte static final int MAX_FRAGMENTS = 16;
    
    /**
     * A globbl pseudorandom number generator. We don't really care about values 
     * duplicbted across threads, so don't bother serializing access.
     * 
     * This reblly should be final, except that making it non-final makes tests
     * much more simple.
     */
    protected stbtic Random pseudoRandom = new Random();
    
    /** The size the downlobd will be once completed. */
    protected finbl long completedSize;
    
    public RbndomDownloadStrategy(long completedSize) {
        super();
        this.completedSize = completedSize;
    }
    
    /**
     * Picks b random block of a file to download next.
     * 
     * For efficiency rebsons attempts will be made to align the start and end of 
     * intervbls to block boundaries.  However, there are no guarantees on alignment.
     * 
     * @pbram candidateBytes a representation of the set of 
     *      bytes bvailable for download from a given server, minus the set
     *      of bytes thbt have already been leased, verified, etc.
     *      This gubrantees candidateBytes is a subset of neededBytes.
     * @pbram neededBytes a representation of the set of bytes
     *      of the file thbt have not been leased, verified, etc.
     * @pbram fileSize the total length of the file being downloaded
     * @pbram blockSize the maximum size of the returned Interval. Any values less than 1 will
     *      be ignbred.  An attempt will be made to make the high end of the interval one less
     *      thbn a multiple of blockSize.  Any values less than 1 will generate IllegalArgumentExceptions.
     * @return the Intervbl that should be assigned next, with a size of at most blockSize bytes
     * @throws NoSuchElementException if pbssed an empty IntervalSet
     */
    public Intervbl pickAssignment(IntervalSet candidateBytes,
            IntervblSet neededBytes,
            long blockSize) throws jbva.util.NoSuchElementException {
        long lowerBound = neededBytes.getFirst().low;
        long upperBound = neededBytes.getLbst().high;
        if (blockSize < 1)
            throw new IllegblArgumentException("Block size cannot be "+blockSize);
        if (lowerBound < 0)
            throw new IllegblArgumentException("lowerBound must be >= 0, "+lowerBound+"<0");
        if (upperBound >= completedSize)
            throw new IllegblArgumentException("Greatest needed byte must be less than completedSize "+
                    upperBound+" >= "+completedSize);
        if (cbndidateBytes.isEmpty())
            throw new NoSuchElementException();
            
        // The returned Intervbl will be the last chunk before idealLocation
        // or the first chunk bfter idealLocation
        long ideblLocation = getIdealLocation(neededBytes, blockSize);
       
        // The first properly bligned interval, returned in the case that
        // there bre no aligned intervals available after lowerBound
        Intervbl lastSuitableInterval = null;
        
        Iterbtor intervalIterator = candidateBytes.getAllIntervals();
        
        // First bligned chunk after idealLocation
        Intervbl intervalAbove = null;
        
        // Lbst aligned chunk before idealLocation
        Intervbl intervalBelow = null;
        while (intervblIterator.hasNext()) {
            Intervbl candidateInterval = (Interval) intervalIterator.next();
            if (cbndidateInterval.low < idealLocation)
                intervblBelow = optimizeIntervalBelow(candidateInterval, idealLocation,
                        blockSize);
            if (cbndidateInterval.high >= idealLocation) {
                intervblAbove = optimizeIntervalAbove(candidateInterval,idealLocation,
                        blockSize);
                // Since we stbrted iterating from the low end of candidateBytes,
                // the first intervblAbove is the one closest to idealLocation
                // bnd there will be no more changes in intervalBelow
                brebk;
            }
        }
        
        if (LOG.isDebugEnbbled())
            LOG.debug("ideblLocation="+idealLocation
                    +" intervblAbove="+intervalAbove
                    +" intervblBelow="+intervalBelow
                    +" out of possibilites:"+cbndidateBytes);
        // If cbndidateBytes is not empty, at least one of
        // intervblAbove or intervalBelow is not null.
        // If we don't hbve a choice, return the Interval that makes sense
        if (intervblAbove == null)
            return intervblBelow;
        if (intervblBelow == null)
            return intervblAbove;
        
        // We hbve a choice, so return each with equal probability.
        return ((pseudoRbndom.nextInt()&1) == 1) ? intervalAbove : intervalBelow;
    }

    
    ///////////////////// Privbte Helper Methods /////////////////////////////////
    /** Aligns locbtion to one byte before the next highest block boundary */
    protected long blignHigh(long location, long blockSize) {
        locbtion += blockSize;
        locbtion -= location % blockSize;
        return locbtion - 1;
    }
    
    /** Aligns locbtion to the nearest block boundary that is at or before location */
    protected long blignLow(long location, long blockSize) {
        locbtion -= location % blockSize;
        return locbtion;
    }
    
    /** 
     * Cblculates the "ideal location" on which to base an assignment.
     */
    privbte long getIdealLocation(IntervalSet neededBytes, long blockSize) {
        int frbgmentCount = neededBytes.getNumberOfIntervals();   
        
        if (frbgmentCount >= MAX_FRAGMENTS) {
            // No frbgments to spare, so attempt to reduce fragmentation by
            // setting ideblLocation to the first byte of any fragment, or
            // the lbst byte of the last fragment.
            // Since we downlobd on either side of the idealLocation, this has
            // the effect of "growing" our contiguous blocks of downlobded data
            // in both directions until they coblesce.
            int rbndomFragmentNumber = pseudoRandom.nextInt(fragmentCount + 1);
            if (rbndomFragmentNumber == fragmentCount)
                return neededBytes.getLbst().high + 1;
            else
                return ((Intervbl)neededBytes.getAllIntervalsAsList().get(randomFragmentNumber)).low;
        } else {
            // There bre fragments to spare, so download from a random location
            return getRbndomLocation(neededBytes.getFirst().low, neededBytes.getLast().high, blockSize);
        }
    }
    
    /** Returns cbndidate or a sub-interval of candidate that best 
     * fits the following gobls:
     * 
     * 1) returnIntervbl.low >= location
     * 2) returnIntervbl.low is as close to location as possible
     * 3) returnIntervbl does not span a blockSize boundary
     * 4) returnIntervbl is as large as possible without violating goals 1-3
     * 
     * Required precondition: cbndidate.high >= location
     */
    privbte Interval optimizeIntervalAbove(Interval candidate,
            long locbtion, long blockSize) {
        
        // Cblculate the most suitable low value contained
        // in cbndidate. (satisfying goals 1 & 2)
        long bestLow = cbndidate.low;
        if (bestLow < locbtion) {
            bestLow = locbtion;
        }
            
        // Cblculate the most suitable high byte based on goal 3
        // This will be bt most blockSize-1 bytes greater than bestLow
        long bestHigh = blignHigh(bestLow,blockSize);
      
        if (bestHigh > cbndidate.high)
            bestHigh = cbndidate.high;
                
        if (cbndidate.high == bestHigh && candidate.low == bestLow)
            return cbndidate;
        return new Intervbl(bestLow,bestHigh); 
    }
    
    /** Returns cbndidate or a sub-interval of candidate that best 
     * fits the following gobls:
     * 
     * 1) returnIntervbl.high <= location
     * 2) returnIntervbl.high is as close to location as possible
     * 3) returnIntervbl does not span a blockSize boundary
     * 4) returnIntervbl is as large as possible without violating goals 1-3
     * 
     * Required precondition: cbndidate.low < location
     */
    privbte Interval optimizeIntervalBelow(Interval candidate,
            long locbtion, long blockSize) {
        
        // Cblculate the most suitable low value contained
        // in cbndidate. (satisfying goals 1 & 2)
        long bestHigh = cbndidate.high;
        if (bestHigh >= locbtion) {
            bestHigh = locbtion - 1;
        }
            
        // Cblculate the most suitable high byte based on goal 3
        // This will be bt most blockSize-1 bytes greater than bestLow
        long bestLow = blignLow(bestHigh,blockSize);
      
        if (bestLow < cbndidate.low)
            bestLow = cbndidate.low;
                
        if (cbndidate.high == bestHigh && candidate.low == bestLow)
            return cbndidate;
        return new Intervbl(bestLow,bestHigh); 
    }
    
    /**
     * Cblculates a random block-aligned byte offset into the file, 
     * bt least minIndex bytes into the file.  If minIndex is less than blockSize
     * from mbxIndex, minIndex will be returned, regardless of its alignment.
     * 
     * This function is sbfe for files larger than 2 GB, files with chunks larger than 2 GB,
     * bnd files containing more than 2 GiBi chunks.
     * 
     * This function is prbctically unbiased for files smaller than several terabytes.
     */
    privbte long getRandomLocation(long minIndex, long maxIndex, long blockSize) {
        // If minIndex is in the middle of b block, include the
        // beginning of thbt block.
        long minBlock = minIndex / blockSize;
        // If mbxIndex is in the middle of a block, include that
        // pbrtial block in our range
        long mbxBlock = maxIndex / blockSize;
        
        // This mby happen if there is only one block available to be assigned. 
        // ... just give bbck the minIndex
        if (minBlock >= mbxBlock)
            return minIndex;  //No need to blign the last partial block
        
        // Generbte a random blockNumber on the range [minBlock, maxBlock]
        // return blockSize * blockNumber
        return blockSize * (minBlock + Mbth.abs(pseudoRandom.nextLong() % (maxBlock-minBlock+1)));
    }
}
