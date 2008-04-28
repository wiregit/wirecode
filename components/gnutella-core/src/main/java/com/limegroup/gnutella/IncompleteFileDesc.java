package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.http.HTTPHeaderValue;

/**
 * This class extends FileDesc and wraps an incomplete File, 
 * so it can be used for partial file sharing.
 */
public class IncompleteFileDesc extends FileDesc implements HTTPHeaderValue {
    /**
     * Ranges smalles than this will never be offered to other servents
     */
    private final static int MIN_CHUNK_SIZE = 102400; // 100K
    
    /**
     * Needed to find out what ranges are available
     */
	private VerifyingFile _verifyingFile;

	/**
	 * The name of the file, as returned by IncompleteFileManager
     *     .getCompletedName(FILE).
	 */
    private final String _name;

	/**
	 * The size of the file, casted to an <tt>int</tt>.
	 */
    private final long _size;

    /**
     * Constructor for the IncompleteFileDesc object.
     */
    public IncompleteFileDesc(File file, Set<? extends URN> urns, int index, 
                              String completedName, long completedSize,
                              VerifyingFile vf) {
        super(file, urns, index);
        _name = completedName;
        _size = completedSize;
        _verifyingFile = vf;
    }

	/**
	 * Returns the completed size of the file on disk, in bytes.
	 *
	 * @return the size of the file on disk, in bytes
	 */
	@Override
    public long getFileSize() {
		return _size;
	}

	/**
	 * Returns the completed name of this file.
	 * 
	 * @return the name of this file
	 */
	@Override
    public String getFileName() {
		return _name;
	}
    
	public IntervalSet.ByteIntervals getRangesAsByte() {
    	return _verifyingFile.toBytes();
    }
    /**
     * Returns the available ranges as an HTTP string value.
     */
    public String getAvailableRanges() {
        StringBuilder ret = new StringBuilder("bytes");
        boolean added = false;
        // This must be synchronized so that downloaders writing
        // to the verifying file do not cause concurrent mod
        // exceptions.
        synchronized(_verifyingFile) {
            for(Range interval : _verifyingFile.getVerifiedBlocks()) {
    	        // don't offer ranges that are smaller than MIN_CHUNK_SIZE
    	        // ( we add one because HTTP values are exclusive )
    	        if (interval.getHigh() - interval.getLow() + 1 < MIN_CHUNK_SIZE)
    		        continue;
    
                added = true;
                // ( we subtract one because HTTP values are exclusive )
                ret.append(" ").append(interval.getLow()).append("-").append(interval.getHigh() -1).append(",");
            }
        }
        // truncate off the last ',' if atleast one was added.
        // it is necessary to do this (instead of checking hasNext when
        // adding the comma) because it's possible that the last range
        // is smaller than MIN_CHUNK_SIZE, leaving an extra comma at the end.
        if(added)
		    ret.setLength(ret.length()-1);

        return ret.toString();
    }
    
    /**
     * @param dest where to load the ranges
     * @return true if the loaded ranges were verified
     */
    public boolean loadResponseRanges(IntervalSet dest) {
        synchronized(_verifyingFile) {
            if (!hasUrnsAndPartialData()) {
                assert getUrns().size() > 1 &&
                _verifyingFile.getBlockSize() + _verifyingFile.getAmountLost() >= MIN_CHUNK_SIZE :
                    "urns : "+getUrns().size()+" size "+_verifyingFile.getBlockSize()+" lost "+_verifyingFile.getAmountLost();
            }
            if (_verifyingFile.getVerifiedBlockSize() > 0) {
                dest.add(_verifyingFile.getVerifiedIntervalSet());
                return true;
            }
            dest.add(_verifyingFile.getPartialIntervalSet());
            return false;
        }
    }
    
    /**
     * @return true if responses should be returned for this IFD.
     */
    public boolean hasUrnsAndPartialData() {
        return getUrns().size() > 1 && // must have both ttroot & sha1 
            _verifyingFile.getBlockSize() >= MIN_CHUNK_SIZE;
    }
    
    /**
     * Determines whether or not the given range is satisfied by this
     * incomplete file.
     */
    public boolean isRangeSatisfiable(long low, long high) {
        // This must be synchronized so that downloaders writing
        // to the verifying file do not cause concurrent mod
        // exceptions.
        synchronized(_verifyingFile) {
            for(Range interval : _verifyingFile.getVerifiedBlocks()) {
                if (low >= interval.getLow() && high <= interval.getHigh())
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Adjusts the requested range to the available range.
     * @return Interval that has been clipped to match the available range, null
     * if the interval does not overlap any available ranges
     */
     public Range getAvailableSubRange(long low, long high) {
        synchronized(_verifyingFile) {
            for(Range interval : _verifyingFile.getVerifiedBlocks()) {
                if ((interval.getLow() <= high && low <= interval.getHigh()))
                	// overlap found 
                    return Range.createRange(Math.max(interval.getLow(), low), 
                                        Math.min(interval.getHigh(), high));
                else if (interval.getLow() > high) // passed all viable intervals
                    break;
            }
            return null;
        }
     }
    
    /**
     * Determines whether or not the given interval is within the range
     * of our incomplete file.
     */
    public boolean isRangeSatisfiable(Range range) {
        return isRangeSatisfiable(range.getLow(), range.getHigh());
    }
    
    // implements HTTPHeaderValue
    public String httpStringValue() {
        return getAvailableRanges();
    }

	// overrides Object.toString to provide a more useful description
	@Override
    public String toString() {
		return ("IncompleteFileDesc:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+getIndex()+"\r\n");
	}
}



