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
    //private final ArrayList<IntervalSet> availableIntervals = new ArrayList<IntervalSet>();
    
    /**
     * Temporary solution for our simplistic implementation of counting
     * partial sources. We "compact" all of the partial search results into
     * this interval set and when it is matches the size of the file, then
     * no more computation is necessary. 
     */
    private final IntervalSet _psets = new IntervalSet();
    
    /**
     * The number of locations for which the entire file can
     * be found.
     */
    private int _wholeCount = 0;
    
    /**
     * 
     */
    private int _displayCount = 0;
    
    /**
     * The number of complete locations available based on
     * all of the partial results that we know of.
     */
    private int _partialCount = 0;
    
    /**
     * This value represent the percentage of the data that
     * is available, up to 100%.
     */
    private float _percentAvailable = 0.0f;
    
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
            throw new IllegalArgumentException("fileSize may not be negative: " + fileSize);
        
       _urn = urn;
       _fileSize = fileSize;
    }
    
    /**
     * When a search result is received the interval set data can be added to
     * the results via addIntervalSet.
     * 
     * @param The ranges this source has.
     */
    public void addPartialSource (IntervalSet is) {
        synchronized (this) {
        //  availableIntervals.add( is );
            _psets.add( is );
        }
        
        calculateLocationCount();
    }
    
    /**
     * Increments the count of complete (ie, non-partial) search
     * results for the given URN.
     */
    public void incrementWholeSources () {
        synchronized (this) {
            _wholeCount++;
            _percentAvailable = 100;
        }
    }
    
    /**
     * 
     * @param num
     */
    public void updateDisplayLocationCount (int num) {
       _displayCount += num; 
    }
    
    /**
     * Combines the whole and partial result counts and returns the total
     * number of locations from which this URN can be accessed.
     * 
     * @return Number of locations from which this URN is available
     */
    public int getLocationCount () {
        synchronized (this) {
            return _wholeCount + _partialCount;
        }
    }
    
    /**
     * 
     * @return 
     */
    public int getDisplayLocationCount () {
        return _displayCount;
//      return getLocationCount();
    }
    
    /**
     * Returns the percentage of the data for the URN that is available. If 
     * whole count is >= 1 or the partial count is >= 1, then the return value
     * is 100. The return value should not exceed 100.
     * 
     * @return The percentage of the file that is accessible on the network
     */
    public float getPercentAvailable () {
        synchronized (this) {
            return _percentAvailable;
        }
    }
    
    /**
     * Determine the percentage of the file that is accessible via the partial 
     * search results.
     * 
     * If the entire file is available, then _partialCount will be set to 1; 
     * otherwise 0.
     */
    private void calculateLocationCount () {
        synchronized (this) {
            long sum = 0;
            
            if (_psets.getNumberOfIntervals() == 0) {
                _partialCount = 0;
                _percentAvailable = _wholeCount > 0 ? 100.0f : 0.0f;
                return;
            }
            
            /*
            IntervalSet iset = new IntervalSet();
            
            // take all of the interval sets that we have for
            // the various URNs and "flatten" them into a single
            // interval set.
            //
            synchronized (availableIntervals) {
                for (IntervalSet is : availableIntervals)
                    iset.add( is );
            }
            */
            
            // if the flattened interval set contains the entire
            // range for the file, then we have enough sources
            // to obtain the entire file.
            //
            if (_psets.contains(Range.createRange(0, _fileSize-1))) {
                _partialCount = 1;
                _percentAvailable = 100.0f;
                return;
            }
            
            // sum the amount of the file represented by this
            // flattened interval set. we must add the +1 
            // because the range is inclusive and not strictly
            // the difference between the two points of the
            // range.
            //
            for (Range range : _psets.getAllIntervalsAsList()) {
                sum += (range.getHigh() - range.getLow()) + 1;
            }
            
            _partialCount = 0;
            
            // if the total number of bytes available in the
            // partial search result is greater than zero, then
            // determine what percentage of the file is available,
            // rounding appropriately and if it rounds down to 
            // zero, set it to 1%.
            //
            if (sum > 0)
                _percentAvailable = sum * 100f / _fileSize;
            else
                _percentAvailable = 0.0f;
        }
    }
    
}
