package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.Interval;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.altlocs.*;

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
    private final int _size;

    /**
     * Constructor for the IncompleteFileDesc object.
     */
    public IncompleteFileDesc(File file, Set urns, int index, 
                              String completedName, int completedSize,
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
     * Adds the alternate location to this FileDesc and also notifies
     * the ManagedDownloader of a new location for this.
     */
    public boolean add(AlternateLocation al) {
        boolean ret = super.add(al);
        if (ret) {
            ManagedDownloader md = _verifyingFile.getManagedDownloader();
            if( md != null )
                md.addDownload(al.createRemoteFileDesc((int)getSize()),false);
        }
        return ret;
    }
    
	/**
     * Adds the alternate locations to this FileDesc and also notifies the
     * ManagedDownloader of new locations for this.
     */
	public int addAll(AlternateLocationCollection alc) {
	    ManagedDownloader md = _verifyingFile.getManagedDownloader();
	    
        // if no downloader, just add the collection.
	    if( md == null )
	        return super.addAll(alc);
	    
        // otherwise, iterate through and individually add them, to make
        // sure they get added to the downloader.
        int added = 0;
        for(Iterator iter = alc.iterator(); iter.hasNext(); ) {
            AlternateLocation al = (AlternateLocation)iter.next();
            if( super.add(al) ) {
                md.addDownload(al.createRemoteFileDesc((int)getSize()),false);
                added++;
            }
        }
        return added;
	}    
    
    /**
     * Returns the available ranges as an HTTP string value.
     */
    public String getAvailableRanges() {
        StringBuffer ret = new StringBuffer("bytes");
        boolean added = false;
        for (Iterator iter = _verifyingFile.getBlocks(); iter.hasNext(); ) {
            Interval interval = (Interval) iter.next();
	        // don't offer ranges that are smaller than MIN_CHUNK_SIZE
	        // ( we add one because HTTP values are exclusive )
	        if (interval.high - interval.low + 1 < MIN_CHUNK_SIZE)
		        continue;

            added = true;
            // ( we subtract one because HTTP value as exclusive )
            ret.append(" " + interval.low + "-" + (interval.high -1) + ",");
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
     * Determines whether or not the given range is satisfied by this
     * incomplete file.
     */
    public boolean isRangeSatisfiable(int low, int high) {
        for (Iterator iter = _verifyingFile.getBlocks(); iter.hasNext(); ) {
            Interval interval = (Interval) iter.next();
            if (low >= interval.low && high <= interval.high)
                return true;
        }
        return false;
    }
    
    /**
     * Determines whether or not the given interval is within the range
     * of our incomplete file.
     */
    public boolean isRangeSatisfiable(Interval range) {
        return isRangeSatisfiable(range.low, range.high);
    }
    
    // implements HTTPHeaderValue
    public String httpStringValue() {
        return getAvailableRanges();
    }
    
	// overrides Object.toString to provide a more useful description
	public String toString() {
		return ("IncompleteFileDesc:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+getIndex()+"\r\n");
	}
}



