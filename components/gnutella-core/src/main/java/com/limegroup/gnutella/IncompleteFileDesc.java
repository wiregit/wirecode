package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.Interval;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.http.*;

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
	private IncompleteFileManager _incompleteFileManager;

	/**
	 * The name of the file, as returned by IncompleteFileManager
     *     .getCompletedName(FILE).
	 */
    private final String _name;

	/**
	 * The size of the file, casted to an <tt>int</tt>.
	 */
    private final int _size;

    /**
     * Constructor for the IncompleteFileDesc object.
     */
    public IncompleteFileDesc( IncompleteFileManager ifm, int index, 
                               Set urns, URN sha1 ) {
        super(new File(ifm.getFileForUrn(sha1).getAbsolutePath()), 
              urns, index);
        _incompleteFileManager = ifm;
        _name = ifm.getCompletedName(getFile());
        _size = (int)ifm.getCompletedSize(getFile());
    }

	/**
	 * Returns the completed size of the file on disk, in bytes.
	 *
	 * @return the size of the file on disk, in bytes
	 */
	public long getSize() {
		return _size;
	}

	/**
	 * Returns the completed name of this file.
	 * 
	 * @return the name of this file
	 */
	public String getName() {
		return _name;
	}

    /**
     * Hits won't ever happen for incomplete files
     * @return 0     
     */    
    public int incrementHitCount() {
        return 0;
    }
    
    /** 
     * Hits won't ever happen for incomplete files
     * @return 0
     */
    public int getHitCount() {
        return 0;
    }
    
    /**
     * Opens an input stream to the <tt>File</tt> instance for this
	 * <tt>FileDesc</tt>.
	 *
	 * @return an <tt>InputStream</tt> to the <tt>File</tt> instance
	 * @throws <tt>FileNotFoundException</tt> if the file represented
	 *  by the <tt>File</tt> instance could not be found
     */
    public InputStream createInputStream() throws FileNotFoundException {
        // if we don't have any available ranges, we should never
        // have entered the download mesh in the first place!!!
        if (getFile().length() == 0) {
            throw new FileNotFoundException();
        }
        return new FileInputStream(getFile());
    }
    
    /**
     * Returns the available ranges as an HTTP string value.
     */
    public String getAvailableRanges() {
        StringBuffer ret = new StringBuffer("bytes");
        VerifyingFile vf = _incompleteFileManager.getEntry(getFile());
        for (Iterator iter = vf.getBlocks(); iter.hasNext(); ) {
            Interval interval = (Interval) iter.next();
	    // don't offer ranges that are smaller than MIN_CHUNK_SIZE
	    if (interval.high - interval.low +1 < MIN_CHUNK_SIZE)
		continue;
            ret.append(" " + interval.low + "-" + (interval.high -1));
            if (iter.hasNext())
                ret.append(",");
        }
        return ret.toString();
    }
    
    /**
     * Determines whether or not the given range is satisfied by this
     * incomplete file.
     */
    public boolean isRangeSatisfiable(int low, int high) {
        return isRangeSatisfiable(new Interval(low, high));
    }
    
    /**
     * Determines whether or not the given interval is within the range
     * of our incomplete file.
     */
    public boolean isRangeSatisfiable(Interval range) {
        VerifyingFile vf = _incompleteFileManager.getEntry(getFile());
        for (Iterator iter = vf.getBlocks(); iter.hasNext(); ) {
            Interval interval = (Interval) iter.next();
            if (range.low >= interval.low && range.high <= interval.high)
                return true;
        }
        return false;
    }
    
    // implements HTTPHeaderValue
    public String httpStringValue() {
        return getAvailableRanges();
    }
    
	// overrides Object.toString to provide a more useful description
	public String toString() {
		return ("IncompleteFileDesc:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+getIndex()+"\r\n"+
				"path:     "+getPath()+"\r\n"+
				"size:     "+_size+"\r\n"+
				"modTime:  "+lastModified()+"\r\n"+
				"File:     "+getFile()+"\r\n"+
				"urns:     "+getUrns()+"\r\n"+
				"alt locs: "+getAlternateLocationCollection()+"\r\n");
	}
}



