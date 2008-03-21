package com.limegroup.gnutella.search;

import java.util.ArrayList;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

import com.limegroup.gnutella.URN;

/**
 * Used to track the search results of a given URN. A single search will result
 * in multiple URNs, and this class will track the total number of locations
 * from which that URN is available including both whole and partial results.
 * 
 */
public class ResourceLocationCounter {

    /**
     * The URN we are tracking.
     */
    private final URN _urn;
    
    /**
     * Size of the file.
     */
    private final long _fileSize;
    
    /**
     * The list of ranges of available data for the URN. Synchronized 
     * internally (on itself) in this class as necessary.
     */
    private final ArrayList<IntervalSet> _isets = new ArrayList<IntervalSet>();
    
    /**
     * The number of locations for which the entire file can
     * be found.
     */
    private int _wholeCount = 0;
    
    /**
     * The number of complete locations available based on
     * all of the partial results that we know of.
     */
    private int _partialCount = 0;
    
    /**
     * This value represent the percentage of the data that
     * is available, up to 100%.
     */
    private int _percentAvailable = 0;
    
    /**
     * Creates a new instance for the given URN.
     * 
     * @param urn The URN we are tracking
     * @param fileSize The size of the file represented by the URN
     */
    public ResourceLocationCounter (URN urn, long fileSize) {
        if (urn == null)
            throw new IllegalArgumentException("URN may not be null.");
        
        if (fileSize < 0)
            throw new IllegalArgumentException("fileSize may not be negative.");
        
       _urn = urn;
       _fileSize = fileSize;
    }
    
    /**
     * When a search result is received the interval set data can be added to
     * the results via addIntervalSet.
     * 
     * @param is The interval set from the search result
     */
    public void addIntervalSet (IntervalSet is) {
        synchronized (_isets) {
            _isets.add( is );
        }
        
        calculateLocationCount();
    }
    
    /**
     * Increments the count of complete (ie, non-partial) search
     * results for the given URN.
     */
    public void incrementCount () {
        _wholeCount++;
        _percentAvailable = 100;
    }
    
    /**
     * Combines the whole and partial result counts and returns the total
     * number of locations from which this URN can be accessed.
     * 
     * @return Number of locations from which this URN is available
     */
    public int getLocationCount () {
        return _wholeCount + _partialCount;
    }
    
    /**
     * Returns the percentage of the data for the URN that is available. If 
     * whole count is >= 1 or the partial count is >= 1, then the return value
     * is 100. The return value should not exceed 100.
     * 
     * @return The percentage of the file that is accessible on the network
     */
    public int getPercentAvailable () {
        return _percentAvailable;
    }
    
    /**
     * Determine the percentage of the file that is accessible via the partial 
     * search results.
     * 
     * If the entire file is available, then _partialCount will be set to 1; 
     * otherwise 0.
     */
    private synchronized void calculateLocationCount () {
        long sum = 0;
        
        // if there are no partial result interval sets,
        // then the partial count is zero and the percent
        // available is also zero, unless there is at least
        // one whole result - then the percent available is
        // 100.
        //
        if (_isets.size() == 0) {
            _partialCount = 0;
            _percentAvailable = _wholeCount > 0 ? 100 : 0;
            return;
        }
        
        IntervalSet iset = new IntervalSet();
        
        // take all of the interval sets that we have for
        // the various URNs and "flatten" them into a single
        // interval set.
        //
        synchronized (_isets) {
            for (IntervalSet is : _isets)
                iset.add( is );
        }
        
        // if the flattened interval set contains the entire
        // range for the file, then we have enough sources
        // to obtain the entire file.
        //
        if (iset.contains(Range.createRange(0, _fileSize-1))) {
            _partialCount = 1;
            _percentAvailable = 100;
            return;
        }
        
        // sum the amount of the file represented by this
        // flattened interval set. we must add the +1 
        // because the range is inclusive and not strictly
        // the difference between the two points of the
        // range.
        //
        for (Range range : iset.getAllIntervalsAsList()) {
            sum += (range.getHigh() - range.getLow()) + 1;
        }
        
        _partialCount = 0;
        
        // if the total number of bytes available in the
        // partial search result is greater than zero, then
        // determine what percentage of the file is available,
        // rounding appropriately and if it rounds down to 
        // zero, set it to 1%.
        //
        if (sum > 0) {
            _percentAvailable = (int)Math.floor(100.0 / ((float)_fileSize / (float)sum));
            
            if (_percentAvailable == 0)
                _percentAvailable = 1;
        }
        else
            _percentAvailable = 0;
    }
    
}
