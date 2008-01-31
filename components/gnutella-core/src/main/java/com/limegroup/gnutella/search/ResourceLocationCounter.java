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
     * 
     * @param urn
     */
    public ResourceLocationCounter (URN urn) {
       _urn = urn;
    }
    
    /**
     * 
     * @param is
     */
    public void addIntervalSet (IntervalSet is) {
        _isets.add( is );
        calculateLocationCount(_isets);
    }
    
    /**
     * Increments the count of complete (ie, non-partial) search
     * results for the given URN.
     * 
     */
    public void incrementCount () {
        _wholeCount++;
    }
    
    /**
     * 
     * @return
     */
    public int getLocationCount () {
        return _wholeCount + _partialCount;
    }
    
    /**
     * 
     * @param isets
     * @return
     */
    private int calculateLocationCount (Vector<IntervalSet> isets) {
        for (IntervalSet is : isets) {
            List<Range> ranges = is.getAllIntervalsAsList();
            
            
            
        }
        
        
        return 0;
    }
    
}
