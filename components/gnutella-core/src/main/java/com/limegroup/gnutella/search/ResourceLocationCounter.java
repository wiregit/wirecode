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
        _partialCount = calculateLocationCount( _isets );
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
     * 
     * @param isets
     * @return
     */
    private int calculateLocationCount (Vector<IntervalSet> isets) {
        
        if (isets == null)
            return 0;
        
        if (isets.size() == 0)
            return 0;
        
        IntervalSet iset = new IntervalSet();
        
        for (IntervalSet is : isets)
            iset.add( is );
        
        // compare iset with _fileSize
        //   what percent of _fileSize is represented by iset?
        //   
        
        
        
        
        return 1;
        
        /*
        for (IntervalSet is : isets) {
            List<Range> ranges = is.getAllIntervalsAsList();
            
            
            
        }
        */
        
        // TODO
        //
        // don't need to adjust _percentAvailable if it is already
        // at 100.
    }
    
}
