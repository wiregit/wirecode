package com.limegroup.gnutella.search;

import java.util.List;
import java.util.Vector;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

import com.limegroup.gnutella.URN;

public class ResourceLocationCounter {

    /**
     * 
     */
    private URN _urn;
    
    /**
     * 
     */
    private Vector<IntervalSet> _isets = new Vector<IntervalSet>();
    
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
     * Size of the file.
     */
    private long _fileSize = 0;
    
    /**
     * 
     * @param urn
     */
    public ResourceLocationCounter (URN urn, long fileSize) {
       _urn = urn;
       _fileSize = fileSize;
    }
    
    /**
     * 
     * @param is
     */
    public void addIntervalSet (IntervalSet is) {
        _isets.add( is );
        calculateLocationCount();
    }
    
    /**
     * Increments the count of complete (ie, non-partial) search
     * results for the given URN.
     * 
     */
    public void incrementCount () {
        _wholeCount++;
        _percentAvailable = 100;
    }
    
    /**
     * 
     * @return
     */
    public int getLocationCount () {
        return _wholeCount + _partialCount;
    }
    
    /**
     * Returns the percentage of the data for the URN _urn that
     * is available. If _wholeCount is >= 1 or _partialCount is
     * >= 1, then the return value is 100. The return value should
     * not exceed 100.
     * 
     * @return
     */
    public int getPercentAvailable () {
        return _percentAvailable;
    }
    
    /**
     * Determine the percentage of the file that is accessible
     * via the partial search results.
     * 
     * If the entire file is available, then _partialCount will
     * be set to 1; otherwise 0.
     * 
     * @param isets
     * @return
     */
    private void calculateLocationCount () {
        long sum = 0;
        
        // if there are no partial result interval sets,
        // then the partial count is zero and the percent
        // available is also zero, unless there is at least
        // one whole result - then the percent available is
        // 100.
        //
        if (_isets == null || _isets.size() == 0) {
            _partialCount = 0;
            _percentAvailable = _wholeCount > 0 ? 100 : 0;
            System.out.println(" *** ResourceLocationCounter::calculateLocationCount().. [POINT-A] pc=" + _partialCount + "; pa=" + _percentAvailable + ";");
            return;
        }
        
        IntervalSet iset = new IntervalSet();
        
        // take all of the interval sets that we have for
        // the various URNs and "flatten" them into a single
        // interval set.
        //
        for (IntervalSet is : _isets)
            iset.add( is );
        
        // if the flattened interval set contains the entire
        // range for the file, then we have enough sources
        // to obtain the entire file.
        //
        if (iset.contains(Range.createRange(0, _fileSize-1))) {
            _partialCount = 1;
            _percentAvailable = 100;
            System.out.println(" *** ResourceLocationCounter::calculateLocationCount().. [POINT-B] pc=" + _partialCount + "; pa=" + _percentAvailable + ";");
            return;
        }
        
        // sum the amount of the file represented by this
        // flattened interval set. we must add the +1 
        // because the range is inclusive and not strictly
        // the difference between the two points of the
        // range.
        //
        for (Range range : iset.getAllIntervalsAsList())
            sum += (range.getHigh() - range.getLow()) + 1;
        
        _partialCount = 0;
        _percentAvailable = (int)(100.0 / ((float)_fileSize / (float)sum));
        
        System.out.println(" *** ResourceLocationCounter::calculateLocationCount().. [POINT-C] pc=" + _partialCount + "; pa=" + _percentAvailable + ";");
    }
    
}
