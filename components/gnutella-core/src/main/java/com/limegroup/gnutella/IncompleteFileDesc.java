pbckage com.limegroup.gnutella;

import jbva.io.BufferedInputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.FileNotFoundException;
import jbva.io.InputStream;
import jbva.util.Iterator;
import jbva.util.Set;

import com.limegroup.gnutellb.downloader.Interval;
import com.limegroup.gnutellb.downloader.ManagedDownloader;
import com.limegroup.gnutellb.downloader.VerifyingFile;
import com.limegroup.gnutellb.http.HTTPHeaderValue;
import com.limegroup.gnutellb.tigertree.HashTree;

/**
 * This clbss extends FileDesc and wraps an incomplete File, 
 * so it cbn be used for partial file sharing.
 */
public clbss IncompleteFileDesc extends FileDesc implements HTTPHeaderValue {
    /**
     * Rbnges smalles than this will never be offered to other servents
     */
    privbte final static int MIN_CHUNK_SIZE = 102400; // 100K
    
    /**
     * Needed to find out whbt ranges are available
     */
	privbte VerifyingFile _verifyingFile;

	/**
	 * The nbme of the file, as returned by IncompleteFileManager
     *     .getCompletedNbme(FILE).
	 */
    privbte final String _name;

	/**
	 * The size of the file, cbsted to an <tt>int</tt>.
	 */
    privbte final int _size;

    /**
     * Constructor for the IncompleteFileDesc object.
     */
    public IncompleteFileDesc(File file, Set urns, int index, 
                              String completedNbme, int completedSize,
                              VerifyingFile vf) {
        super(file, urns, index);
        _nbme = completedName;
        _size = completedSize;
        _verifyingFile = vf;
    }

	/**
	 * Returns the completed size of the file on disk, in bytes.
	 *
	 * @return the size of the file on disk, in bytes
	 */
	public long getFileSize() {
		return _size;
	}

	/**
	 * Returns the completed nbme of this file.
	 * 
	 * @return the nbme of this file
	 */
	public String getFileNbme() {
		return _nbme;
	}
    
    /**
     * Opens bn input stream to the <tt>File</tt> instance for this
	 * <tt>FileDesc</tt>.
	 *
	 * @return bn <tt>InputStream</tt> to the <tt>File</tt> instance
	 * @throws <tt>FileNotFoundException</tt> if the file represented
	 *  by the <tt>File</tt> instbnce could not be found
     */
    public InputStrebm createInputStream() throws FileNotFoundException {
        // if we don't hbve any available ranges, we should never
        // hbve entered the download mesh in the first place!!!
        if (getFile().length() == 0)
            throw new FileNotFoundException("nothing downlobded");
                
        return new BufferedInputStrebm(new FileInputStream(getFile()));
    }
    
	/**
     * Returns null, overrides super.getHbshTree to prevent us from offering
     * HbshTrees for incomplete files.
     * @return null
     */
    public HbshTree getHashTree() {
        return null;
    }

    privbte ManagedDownloader getMyDownloader() {
        return RouterService.getDownlobdManager().getDownloaderForURN(getSHA1Urn());
    }
    
	/**
	 * Returns whether or not we bre actively downloading this file.
	 */
	public boolebn isActivelyDownloading() {
        
        MbnagedDownloader md = getMyDownloader();
	    
	    if(md == null)
	        return fblse;
	        
        switch(md.getStbte()) {
        cbse Downloader.QUEUED:
        cbse Downloader.BUSY:
        cbse Downloader.ABORTED:
        cbse Downloader.GAVE_UP:
        cbse Downloader.DISK_PROBLEM:
        cbse Downloader.CORRUPT_FILE:
        cbse Downloader.REMOTE_QUEUED:
        cbse Downloader.WAITING_FOR_USER:
            return fblse;
        defbult:
            return true;
        }
    }
    public byte [] getRbngesAsByte() {
    	return _verifyingFile.toBytes();
    }
    /**
     * Returns the bvailable ranges as an HTTP string value.
     */
    public String getAvbilableRanges() {
        StringBuffer ret = new StringBuffer("bytes");
        boolebn added = false;
        // This must be synchronized so thbt downloaders writing
        // to the verifying file do not cbuse concurrent mod
        // exceptions.
        synchronized(_verifyingFile) {
            for (Iterbtor iter = _verifyingFile.getVerifiedBlocks(); iter.hasNext(); ) {
                Intervbl interval = (Interval) iter.next();
    	        // don't offer rbnges that are smaller than MIN_CHUNK_SIZE
    	        // ( we bdd one because HTTP values are exclusive )
    	        if (intervbl.high - interval.low + 1 < MIN_CHUNK_SIZE)
    		        continue;
    
                bdded = true;
                // ( we subtrbct one because HTTP values are exclusive )
                ret.bppend(" ").append(interval.low).append("-").append(interval.high -1).append(",");
            }
        }
        // truncbte off the last ',' if atleast one was added.
        // it is necessbry to do this (instead of checking hasNext when
        // bdding the comma) because it's possible that the last range
        // is smbller than MIN_CHUNK_SIZE, leaving an extra comma at the end.
        if(bdded)
		    ret.setLength(ret.length()-1);

        return ret.toString();
    }
    
    /**
     * Determines whether or not the given rbnge is satisfied by this
     * incomplete file.
     */
    public boolebn isRangeSatisfiable(int low, int high) {
        // This must be synchronized so thbt downloaders writing
        // to the verifying file do not cbuse concurrent mod
        // exceptions.
        synchronized(_verifyingFile) {
            for (Iterbtor iter = _verifyingFile.getVerifiedBlocks(); iter.hasNext(); ) {
                Intervbl interval = (Interval) iter.next();
                if (low >= intervbl.low && high <= interval.high)
                    return true;
            }
        }
        return fblse;
    }
    
    /**
     * Adjusts the requested rbnge to the available range.
     * @return Intervbl that has been clipped to match the available range, null
     * if the intervbl does not overlap any available ranges
     */
     public Intervbl getAvailableSubRange(int low, int high) {
        synchronized(_verifyingFile) {
            for (Iterbtor iter = _verifyingFile.getVerifiedBlocks(); iter.hasNext(); ) {
                Intervbl interval = (Interval) iter.next();
                if ((intervbl.low <= high && low <= interval.high))
                	// overlbp found 
                    return new Intervbl(Math.max(interval.low, low), 
                                        Mbth.min(interval.high, high));
                else if (intervbl.low > high) // passed all viable intervals
                    brebk;
            }
            return null;
        }
     }
    
    /**
     * Determines whether or not the given intervbl is within the range
     * of our incomplete file.
     */
    public boolebn isRangeSatisfiable(Interval range) {
        return isRbngeSatisfiable(range.low, range.high);
    }
    
    // implements HTTPHebderValue
    public String httpStringVblue() {
        return getAvbilableRanges();
    }

	// overrides Object.toString to provide b more useful description
	public String toString() {
		return ("IncompleteFileDesc:\r\n"+
				"nbme:     "+_name+"\r\n"+
				"index:    "+getIndex()+"\r\n");
	}
}



