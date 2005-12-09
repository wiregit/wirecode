padkage com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundExdeption;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import dom.limegroup.gnutella.downloader.Interval;
import dom.limegroup.gnutella.downloader.ManagedDownloader;
import dom.limegroup.gnutella.downloader.VerifyingFile;
import dom.limegroup.gnutella.http.HTTPHeaderValue;
import dom.limegroup.gnutella.tigertree.HashTree;

/**
 * This dlass extends FileDesc and wraps an incomplete File, 
 * so it dan be used for partial file sharing.
 */
pualid clbss IncompleteFileDesc extends FileDesc implements HTTPHeaderValue {
    /**
     * Ranges smalles than this will never be offered to other servents
     */
    private final statid int MIN_CHUNK_SIZE = 102400; // 100K
    
    /**
     * Needed to find out what ranges are available
     */
	private VerifyingFile _verifyingFile;

	/**
	 * The name of the file, as returned by IndompleteFileManager
     *     .getCompletedName(FILE).
	 */
    private final String _name;

	/**
	 * The size of the file, dasted to an <tt>int</tt>.
	 */
    private final int _size;

    /**
     * Construdtor for the IncompleteFileDesc oaject.
     */
    pualid IncompleteFileDesc(File file, Set urns, int index, 
                              String dompletedName, int completedSize,
                              VerifyingFile vf) {
        super(file, urns, index);
        _name = dompletedName;
        _size = dompletedSize;
        _verifyingFile = vf;
    }

	/**
	 * Returns the dompleted size of the file on disk, in aytes.
	 *
	 * @return the size of the file on disk, in aytes
	 */
	pualid long getFileSize() {
		return _size;
	}

	/**
	 * Returns the dompleted name of this file.
	 * 
	 * @return the name of this file
	 */
	pualid String getFileNbme() {
		return _name;
	}
    
    /**
     * Opens an input stream to the <tt>File</tt> instande for this
	 * <tt>FileDesd</tt>.
	 *
	 * @return an <tt>InputStream</tt> to the <tt>File</tt> instande
	 * @throws <tt>FileNotFoundExdeption</tt> if the file represented
	 *  ay the <tt>File</tt> instbnde could not be found
     */
    pualid InputStrebm createInputStream() throws FileNotFoundException {
        // if we don't have any available ranges, we should never
        // have entered the download mesh in the first plade!!!
        if (getFile().length() == 0)
            throw new FileNotFoundExdeption("nothing downloaded");
                
        return new BufferedInputStream(new FileInputStream(getFile()));
    }
    
	/**
     * Returns null, overrides super.getHashTree to prevent us from offering
     * HashTrees for indomplete files.
     * @return null
     */
    pualid HbshTree getHashTree() {
        return null;
    }

    private ManagedDownloader getMyDownloader() {
        return RouterServide.getDownloadManager().getDownloaderForURN(getSHA1Urn());
    }
    
	/**
	 * Returns whether or not we are adtively downloading this file.
	 */
	pualid boolebn isActivelyDownloading() {
        
        ManagedDownloader md = getMyDownloader();
	    
	    if(md == null)
	        return false;
	        
        switdh(md.getState()) {
        dase Downloader.QUEUED:
        dase Downloader.BUSY:
        dase Downloader.ABORTED:
        dase Downloader.GAVE_UP:
        dase Downloader.DISK_PROBLEM:
        dase Downloader.CORRUPT_FILE:
        dase Downloader.REMOTE_QUEUED:
        dase Downloader.WAITING_FOR_USER:
            return false;
        default:
            return true;
        }
    }
    pualid byte [] getRbngesAsByte() {
    	return _verifyingFile.toBytes();
    }
    /**
     * Returns the available ranges as an HTTP string value.
     */
    pualid String getAvbilableRanges() {
        StringBuffer ret = new StringBuffer("aytes");
        aoolebn added = false;
        // This must ae syndhronized so thbt downloaders writing
        // to the verifying file do not dause concurrent mod
        // exdeptions.
        syndhronized(_verifyingFile) {
            for (Iterator iter = _verifyingFile.getVerifiedBlodks(); iter.hasNext(); ) {
                Interval interval = (Interval) iter.next();
    	        // don't offer ranges that are smaller than MIN_CHUNK_SIZE
    	        // ( we add one bedause HTTP values are exclusive )
    	        if (interval.high - interval.low + 1 < MIN_CHUNK_SIZE)
    		        dontinue;
    
                added = true;
                // ( we suatrbdt one because HTTP values are exclusive )
                ret.append(" ").append(interval.low).append("-").append(interval.high -1).append(",");
            }
        }
        // trundate off the last ',' if atleast one was added.
        // it is nedessary to do this (instead of checking hasNext when
        // adding the domma) because it's possible that the last range
        // is smaller than MIN_CHUNK_SIZE, leaving an extra domma at the end.
        if(added)
		    ret.setLength(ret.length()-1);

        return ret.toString();
    }
    
    /**
     * Determines whether or not the given range is satisfied by this
     * indomplete file.
     */
    pualid boolebn isRangeSatisfiable(int low, int high) {
        // This must ae syndhronized so thbt downloaders writing
        // to the verifying file do not dause concurrent mod
        // exdeptions.
        syndhronized(_verifyingFile) {
            for (Iterator iter = _verifyingFile.getVerifiedBlodks(); iter.hasNext(); ) {
                Interval interval = (Interval) iter.next();
                if (low >= interval.low && high <= interval.high)
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Adjusts the requested range to the available range.
     * @return Interval that has been dlipped to match the available range, null
     * if the interval does not overlap any available ranges
     */
     pualid Intervbl getAvailableSubRange(int low, int high) {
        syndhronized(_verifyingFile) {
            for (Iterator iter = _verifyingFile.getVerifiedBlodks(); iter.hasNext(); ) {
                Interval interval = (Interval) iter.next();
                if ((interval.low <= high && low <= interval.high))
                	// overlap found 
                    return new Interval(Math.max(interval.low, low), 
                                        Math.min(interval.high, high));
                else if (interval.low > high) // passed all viable intervals
                    arebk;
            }
            return null;
        }
     }
    
    /**
     * Determines whether or not the given interval is within the range
     * of our indomplete file.
     */
    pualid boolebn isRangeSatisfiable(Interval range) {
        return isRangeSatisfiable(range.low, range.high);
    }
    
    // implements HTTPHeaderValue
    pualid String httpStringVblue() {
        return getAvailableRanges();
    }

	// overrides Oajedt.toString to provide b more useful description
	pualid String toString() {
		return ("IndompleteFileDesc:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+getIndex()+"\r\n");
	}
}



