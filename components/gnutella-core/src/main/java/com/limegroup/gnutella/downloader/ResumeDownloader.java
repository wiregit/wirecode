package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A ManagedDownloader that tries to resume to a specific incomplete file.  The
 * ResumeDownloader initially has no locations to download from.  Instead it
 * immediately requeries--by hash if possible--and only accepts results that
 * would result in resumes from the specified incomplete file.  Do not be
 * confused by the name; ManagedDownloader CAN resume from incomplete files,
 * but it is less strict about its choice of download.
 */
public class ResumeDownloader extends ManagedDownloader 
        implements Serializable {
    /** Ensures backwards compatibility of the downloads.dat file. */
    static final long serialVersionUID = -4535935715006098724L;

    /** The temporary file to resume to. */
    private final File _incompleteFile;
    /** The name and size of the completed file, extracted from
     *  _incompleteFile. */
    private final String _name;
    private final int _size;
    /** The hash of the completed file.  This field was not included in the LW
     *  2.7.0/2.7.1 beta, so it may be null when reading downloads.dat files
     *  from these rare versions.  That's no big deal; its like not having the
     *  hash in the first place.  */
    private final URN _hash;
    

    /** 
     * Creates a RequeryDownloader to finish downloading incompleteFile.  This
     * constructor has preconditions on several parameters; putting the burden
     * on the caller makes the method easier to implement, since the superclass
     * constructor immediately starts a download thread.
     *
     * @param incompleteFile the incomplete file to resume to, which
     *  MUST be the result of IncompleteFileManager.getFile.
     * @param name the name of the completed file, which MUST be the result of
     *  IncompleteFileManager.getCompletedName(incompleteFile)
     * @param size the size of the completed file, which MUST be the result of
     *  IncompleteFileManager.getCompletedSize(incompleteFile) */
    public ResumeDownloader(IncompleteFileManager incompleteFileManager,
                            File incompleteFile,
                            String name,
                            int size) {
        super(name, new RemoteFileDesc[0], incompleteFileManager);
        this._incompleteFile=incompleteFile;
        this._name=name;
        this._size=size;
        this._hash=incompleteFileManager.getCompletedHash(incompleteFile);
    }

    /** Overrides ManagedDownloader to ensure that progress is initially
     *  non-zero and file previewing works. */
    public void initialize(DownloadManager manager, 
                           FileManager fileManager, 
                           ActivityCallback callback) {
        initializeIncompleteFile(_incompleteFile);
        super.initialize(manager, fileManager, callback);
    }

    /**
     * Overrides ManagedDownloader to reserve _incompleteFile for this download.
     * That is, any download that would use the same incomplete file is 
     * rejected, even if this is not currently downloading.
     */
    public boolean conflicts(File incompleteFile) {
        return incompleteFile.equals(_incompleteFile);
    }

    /**
     * Overrides ManagedDownloader to allow any RemoteFileDesc that would
     * resume from _incompleteFile.
     */
    protected boolean allowAddition(RemoteFileDesc other) { 
        //Like "_incompleteFile.equals(_incompleteFileManager.getFile(other))"
        //but more efficient since no allocations in IncompleteFileManager.
        return IncompleteFileManager.same(
            _name, _size, _hash,     
            other.getFileName(), other.getSize(), other.getSHA1Urn());
    }


    /**
     * Overrides ManagedDownloader to display a reasonable file size even
     * when no locations have been found.
     */
    public synchronized int getContentLength() {
        return _size;
    }

    /** 
     * Overrides ManagedDownloader to ensure that the first requery happens as
     * soon as necessary.
     */
    protected long nextRequeryTime(int requeries) {
        if (requeries==0)
            return System.currentTimeMillis();   //now!
        else
            return super.nextRequeryTime(requeries);        
    }

    /**
     * Overrides ManagedDownloader to display a reasonable file name even
     * when no locations have been found.
     */
    public synchronized String getFileName() {
        return _name;
    }

    /** Overrides ManagedDownloader to use the filename and hash (if present) of
     *  the incomplete file. */
    protected QueryRequest newRequery(int numRequeries) {
        Set queryUrns=null;
        if (_hash!=null) {
            queryUrns=new HashSet(1);
            queryUrns.add(_hash);
        }
        //TODO: we always include the file name since HUGE specifies that
        //results should be sent if the name OR the hashes match.  But
        //ultrapeers may insist that all keywords are in the QRP tables.
        boolean isRequery=numRequeries!=0;
		if(isRequery) {
		    if (_hash != null)
		        return QueryRequest.createRequery(_hash);
		    else
		        return QueryRequest.createRequery(getFileName());
		} else {
		    if (_hash != null)
		        return QueryRequest.createQuery(_hash);
		    else
		        return QueryRequest.createQuery(getFileName());
	    }
    }
}
