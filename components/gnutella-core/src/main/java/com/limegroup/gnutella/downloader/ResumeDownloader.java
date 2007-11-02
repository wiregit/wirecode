package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.ObjectInputStream.GetField;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * A ManagedDownloader that tries to resume a specific incomplete file.  The
 * ResumeDownloader initially has no locations to download from.  Instead it
 * immediately requeries--by hash if possible--and only accepts results that
 * would result in resumes from the specified incomplete file.  Do not be
 * confused by the name; ManagedDownloader CAN resume from incomplete files,
 * but it is less strict about its choice of download.
 */
public class ResumeDownloader extends ManagedDownloader 
        implements Serializable {
    /** Ensures backwards compatibility of the downloads.dat file. */
    private static final long serialVersionUID = -4535935715006098724L;
    
    /** The temporary file to resume to. */
    private volatile File _incompleteFile;
    
    /** The name and size of the completed file, extracted from
     *  _incompleteFile. */
    private volatile String _name;
    
    /** The old size field.  Kept around for backwards-compatibility */
    @Deprecated
    @SuppressWarnings("unused")
    private volatile int _size;
    
    /** The size of the file*/
    private volatile long _size64;
    
    /**
     * The hash of the completed file.  This field was not included in the LW
     * 2.7.0/2.7.1 beta, so it may be null when reading downloads.dat files
     * from these rare versions.  That's no big deal; it is like not having the
     * hash in the first place. 
     *
     * This is not used as much anymore, since ManagedDownloader stores the
     * SHA1 anyway.  It is still used, however, to keep the sha1 between
     * sessions, since it is serialized.
     */
    private volatile URN _hash;
    

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
    ResumeDownloader(IncompleteFileManager incompleteFileManager,
                            File incompleteFile,
                            String name,
                            long size, SaveLocationManager saveLocationManager) {
        super( new RemoteFileDesc[0], incompleteFileManager, null, saveLocationManager);
        if( incompleteFile == null )
            throw new NullPointerException("null incompleteFile");
        this._incompleteFile=incompleteFile;
        if(name==null || name.equals(""))
            throw new IllegalArgumentException("Bad name in ResumeDownloader");
        this._name=name;
        this._size64=size;
        this._hash=incompleteFileManager.getCompletedHash(incompleteFile);
    }

    /**
     * Overrides ManagedDownloader to ensure that progress is initially non-zero
     * and file previewing works.
     */
    public void initialize(DownloadReferences downloadReferences) {
        if (_hash != null)
            downloadSHA1 = _hash;
        incompleteFile = _incompleteFile;
        super.initialize(downloadReferences);
        // Auto-activate the requeryManager if this was created
        // from clicking 'Resume' in the library (as opposed to
        // from being deserialized from disk).
        if (!deserializedFromDisk)
            requeryManager.activate();
    }

    /**
     * Overrides ManagedDownloader to reserve _incompleteFile for this download.
     * That is, any download that would use the same incomplete file is
     * rejected, even if this is not currently downloading.
     */
    public boolean conflictsWithIncompleteFile(File incompleteFile) {
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
            _name, _size64, downloadSHA1,     
            other.getFileName(), other.getSize(), other.getSHA1Urn());
    }


    /**
     * Overrides ManagedDownloader to display a reasonable file size even
     * when no locations have been found.
     */
    public synchronized long getContentLength() {
        return _size64;
    }

    protected synchronized String getDefaultFileName() {
        return _name.toString();
    }

    /**
     * Overriden to unset deserializedFromDisk too.
     */
    public synchronized boolean resume() {
        boolean ret = super.resume();
        // unset deserialized once we clicked resume
        if(ret)
            deserializedFromDisk = false;
        return ret;
    }
 
    protected boolean shouldInitAltLocs(boolean deserializedFromDisk) {
        // we shoudl only initialize alt locs when we are started from the
        // library, not when we are resumed from startup.
        return !deserializedFromDisk;
    }

    /** Overrides ManagedDownloader to use the filename and hash (if present) of
     *  the incomplete file. */
    protected QueryRequest newRequery(int numRequeries) {
        // Extract a query string from our filename.
        String queryName = QueryUtils.createQueryString(getDefaultFileName());

        if (downloadSHA1 != null)
            // TODO: we should be sending the URN with the query, but
            // we don't because URN queries are summarily dropped, though
            // this may change
            return queryRequestFactory.createQuery(queryName);
        else
            return queryRequestFactory.createQuery(queryName);
    }
    
    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        GetField gets = stream.readFields();
        _hash = (URN)gets.get("_hash", null);
        _name = (String) gets.get("_name", null);
        _incompleteFile = (File)gets.get("_incompleteFile",null);
        
        // try to read the long size first, if not there read the int
        _size64 = gets.get("_size64", -1L);
        if (_size64 == -1L)
            _size64 = gets.get("_size",0);
    }
}
