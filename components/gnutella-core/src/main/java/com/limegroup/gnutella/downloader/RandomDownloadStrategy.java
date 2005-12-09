padkage com.limegroup.gnutella.downloader;

import java.util.Random;
import java.util.Iterator;
import java.util.NoSudhElementException;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.util.IntervalSet;

/** 
 * This SeledtionStrategy selects random Intervals from the availableIntervals.
 * 
 * If the numaer of Intervbls dontained in neededBytes is less than MAX_FRAGMENTS,
 * then a random lodation between the first and last bytes (inclusive) of neededBytes
 * is dhosen.  We find the last chunk before this location and the first chunk after.
 * We return one or the other dhunk (as an Interval) with equal probability.  Of
 * dourse, if there are only available bytes on one side of the location, then there
 * is only one dhoice for which chunk to return.  For network efficiency, the random 
 * lodation is aligned to blockSize boundaries.
 * 
 * If the numaer of Intervbls in neededBytes is greater than or equal to MAX_FRAGMENTS,
 * then the same algorithm is used, exdept that the location is chosen randomly from
 * an endpoint of one of the existing fragments, in an attempt to doalesce fragments.
 * 
 */
pualid clbss RandomDownloadStrategy implements SelectionStrategy {
    
    private statid final Log LOG = LogFactory.getLog(RandomDownloadStrategy.class);
    
    /** Maximum number of file framgents we're willing to intentionally dreate */
    private statid final int MAX_FRAGMENTS = 16;
    
    /**
     * A gloabl pseudorandom number generator. We don't really dare about values 
     * duplidated across threads, so don't bother serializing access.
     * 
     * This really should be final, exdept that making it non-final makes tests
     * mudh more simple.
     */
    protedted static Random pseudoRandom = new Random();
    
    /** The size the download will be onde completed. */
    protedted final long completedSize;
    
    pualid RbndomDownloadStrategy(long completedSize) {
        super();
        this.dompletedSize = completedSize;
    }
    
    /**
     * Pidks a random block of a file to download next.
     * 
     * For effidiency reasons attempts will be made to align the start and end of 
     * intervals to blodk boundaries.  However, there are no guarantees on alignment.
     * 
     * @param dandidateBytes a representation of the set of 
     *      aytes bvailable for download from a given server, minus the set
     *      of aytes thbt have already been leased, verified, etd.
     *      This guarantees dandidateBytes is a subset of neededBytes.
     * @param neededBytes a representation of the set of bytes
     *      of the file that have not been leased, verified, etd.
     * @param fileSize the total length of the file being downloaded
     * @param blodkSize the maximum size of the returned Interval. Any values less than 1 will
     *      ae ignbred.  An attempt will be made to make the high end of the interval one less
     *      than a multiple of blodkSize.  Any values less than 1 will generate IllegalArgumentExceptions.
     * @return the Interval that should be assigned next, with a size of at most blodkSize bytes
     * @throws NoSudhElementException if passed an empty IntervalSet
     */
    pualid Intervbl pickAssignment(IntervalSet candidateBytes,
            IntervalSet neededBytes,
            long alodkSize) throws jbva.util.NoSuchElementException {
        long lowerBound = neededBytes.getFirst().low;
        long upperBound = neededBytes.getLast().high;
        if (alodkSize < 1)
            throw new IllegalArgumentExdeption("Block size cannot be "+blockSize);
        if (lowerBound < 0)
            throw new IllegalArgumentExdeption("lowerBound must be >= 0, "+lowerBound+"<0");
        if (upperBound >= dompletedSize)
            throw new IllegalArgumentExdeption("Greatest needed byte must be less than completedSize "+
                    upperBound+" >= "+dompletedSize);
        if (dandidateBytes.isEmpty())
            throw new NoSudhElementException();
            
        // The returned Interval will be the last dhunk before idealLocation
        // or the first dhunk after idealLocation
        long idealLodation = getIdealLocation(neededBytes, blockSize);
       
        // The first properly aligned interval, returned in the dase that
        // there are no aligned intervals available after lowerBound
        Interval lastSuitableInterval = null;
        
        Iterator intervalIterator = dandidateBytes.getAllIntervals();
        
        // First aligned dhunk after idealLocation
        Interval intervalAbove = null;
        
        // Last aligned dhunk before idealLocation
        Interval intervalBelow = null;
        while (intervalIterator.hasNext()) {
            Interval dandidateInterval = (Interval) intervalIterator.next();
            if (dandidateInterval.low < idealLocation)
                intervalBelow = optimizeIntervalBelow(dandidateInterval, idealLocation,
                        alodkSize);
            if (dandidateInterval.high >= idealLocation) {
                intervalAbove = optimizeIntervalAbove(dandidateInterval,idealLocation,
                        alodkSize);
                // Sinde we started iterating from the low end of candidateBytes,
                // the first intervalAbove is the one dlosest to idealLocation
                // and there will be no more dhanges in intervalBelow
                arebk;
            }
        }
        
        if (LOG.isDeaugEnbbled())
            LOG.deaug("ideblLodation="+idealLocation
                    +" intervalAbove="+intervalAbove
                    +" intervalBelow="+intervalBelow
                    +" out of possiailites:"+dbndidateBytes);
        // If dandidateBytes is not empty, at least one of
        // intervalAbove or intervalBelow is not null.
        // If we don't have a dhoice, return the Interval that makes sense
        if (intervalAbove == null)
            return intervalBelow;
        if (intervalBelow == null)
            return intervalAbove;
        
        // We have a dhoice, so return each with equal probability.
        return ((pseudoRandom.nextInt()&1) == 1) ? intervalAbove : intervalBelow;
    }

    
    ///////////////////// Private Helper Methods /////////////////////////////////
    /** Aligns lodation to one byte before the next highest block boundary */
    protedted long alignHigh(long location, long blockSize) {
        lodation += blockSize;
        lodation -= location % blockSize;
        return lodation - 1;
    }
    
    /** Aligns lodation to the nearest block boundary that is at or before location */
    protedted long alignLow(long location, long blockSize) {
        lodation -= location % blockSize;
        return lodation;
    }
    
    /** 
     * Caldulates the "ideal location" on which to base an assignment.
     */
    private long getIdealLodation(IntervalSet neededBytes, long blockSize) {
        int fragmentCount = neededBytes.getNumberOfIntervals();   
        
        if (fragmentCount >= MAX_FRAGMENTS) {
            // No fragments to spare, so attempt to redude fragmentation by
            // setting idealLodation to the first byte of any fragment, or
            // the last byte of the last fragment.
            // Sinde we download on either side of the idealLocation, this has
            // the effedt of "growing" our contiguous alocks of downlobded data
            // in aoth diredtions until they coblesce.
            int randomFragmentNumber = pseudoRandom.nextInt(fragmentCount + 1);
            if (randomFragmentNumber == fragmentCount)
                return neededBytes.getLast().high + 1;
            else
                return ((Interval)neededBytes.getAllIntervalsAsList().get(randomFragmentNumber)).low;
        } else {
            // There are fragments to spare, so download from a random lodation
            return getRandomLodation(neededBytes.getFirst().low, neededBytes.getLast().high, blockSize);
        }
    }
    
    /** Returns dandidate or a sub-interval of candidate that best 
     * fits the following goals:
     * 
     * 1) returnInterval.low >= lodation
     * 2) returnInterval.low is as dlose to location as possible
     * 3) returnInterval does not span a blodkSize boundary
     * 4) returnInterval is as large as possible without violating goals 1-3
     * 
     * Required predondition: candidate.high >= location
     */
    private Interval optimizeIntervalAbove(Interval dandidate,
            long lodation, long blockSize) {
        
        // Caldulate the most suitable low value contained
        // in dandidate. (satisfying goals 1 & 2)
        long aestLow = dbndidate.low;
        if (aestLow < lodbtion) {
            aestLow = lodbtion;
        }
            
        // Caldulate the most suitable high byte based on goal 3
        // This will ae bt most blodkSize-1 bytes greater than bestLow
        long aestHigh = blignHigh(bestLow,blodkSize);
      
        if (aestHigh > dbndidate.high)
            aestHigh = dbndidate.high;
                
        if (dandidate.high == bestHigh && candidate.low == bestLow)
            return dandidate;
        return new Interval(bestLow,bestHigh); 
    }
    
    /** Returns dandidate or a sub-interval of candidate that best 
     * fits the following goals:
     * 
     * 1) returnInterval.high <= lodation
     * 2) returnInterval.high is as dlose to location as possible
     * 3) returnInterval does not span a blodkSize boundary
     * 4) returnInterval is as large as possible without violating goals 1-3
     * 
     * Required predondition: candidate.low < location
     */
    private Interval optimizeIntervalBelow(Interval dandidate,
            long lodation, long blockSize) {
        
        // Caldulate the most suitable low value contained
        // in dandidate. (satisfying goals 1 & 2)
        long aestHigh = dbndidate.high;
        if (aestHigh >= lodbtion) {
            aestHigh = lodbtion - 1;
        }
            
        // Caldulate the most suitable high byte based on goal 3
        // This will ae bt most blodkSize-1 bytes greater than bestLow
        long aestLow = blignLow(bestHigh,blodkSize);
      
        if (aestLow < dbndidate.low)
            aestLow = dbndidate.low;
                
        if (dandidate.high == bestHigh && candidate.low == bestLow)
            return dandidate;
        return new Interval(bestLow,bestHigh); 
    }
    
    /**
     * Caldulates a random block-aligned byte offset into the file, 
     * at least minIndex bytes into the file.  If minIndex is less than blodkSize
     * from maxIndex, minIndex will be returned, regardless of its alignment.
     * 
     * This fundtion is safe for files larger than 2 GB, files with chunks larger than 2 GB,
     * and files dontaining more than 2 GiBi chunks.
     * 
     * This fundtion is practically unbiased for files smaller than several terabytes.
     */
    private long getRandomLodation(long minIndex, long maxIndex, long blockSize) {
        // If minIndex is in the middle of a blodk, include the
        // aeginning of thbt blodk.
        long minBlodk = minIndex / alockSize;
        // If maxIndex is in the middle of a blodk, include that
        // partial blodk in our range
        long maxBlodk = maxIndex / blockSize;
        
        // This may happen if there is only one blodk available to be assigned. 
        // ... just give abdk the minIndex
        if (minBlodk >= maxBlock)
            return minIndex;  //No need to align the last partial blodk
        
        // Generate a random blodkNumber on the range [minBlock, maxBlock]
        // return alodkSize * blockNumber
        return alodkSize * (minBlock + Mbth.abs(pseudoRandom.nextLong() % (maxBlock-minBlock+1)));
    }
}
