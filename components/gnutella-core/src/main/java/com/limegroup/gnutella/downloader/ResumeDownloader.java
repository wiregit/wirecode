package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A ManagedDownloader that tries to resume to a specific incomplete file.  The
 * ResumeDownloader initially has no locations to download from.  Instead it
 * immediately requeries--by hash if possible--and only accepts results that
 * would result in resumes from the specified incomplete file.
 */
public class ResumeDownloader extends ManagedDownloader 
        implements Serializable {
    private final IncompleteFileManager _incompleteFileManager;
    private final File _incompleteFile;
    private final String _name;
    private final int _size;

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
    public ResumeDownloader(DownloadManager manager,
                            FileManager fileManager,
                            IncompleteFileManager incompleteFileManager,
                            ActivityCallback callback,
                            File incompleteFile,
                            String name,
                            int size) {
        super(manager, new RemoteFileDesc[0], fileManager,
              incompleteFileManager,callback);
        this._incompleteFileManager=incompleteFileManager;
        this._incompleteFile=incompleteFile;
        this._name=name;
        this._size=size;
    }

    public boolean conflicts(File incompleteFile) {
        return incompleteFile.equals(_incompleteFile);
    }

    public boolean conflictsLAX(RemoteFileDesc other) {        
        return _incompleteFile.equals(_incompleteFileManager.getFile(other));
    }

    public synchronized String getFileName() {        
        return _name;
    }

    public synchronized int getContentLength() {
        return _size;
    }

    public QueryRequest newRequery() {
        URN hash=_incompleteFileManager.getCompletedHash(_incompleteFile);
        Set queryUrns=null;
        if (hash!=null) {
            queryUrns=new HashSet(1);
            queryUrns.add(hash);
        }
        //TODO: we always include the file name since HUGE specifies that
        //results should be sent if the name OR the hashes match.  But
        //ultrapeers may insist that all keywords are in the QRP tables.
        return new QueryRequest(QueryRequest.newQueryGUID(true), //requery GUID
                                (byte)7, 0,        //TTL, speed
                                getFileName(),     //query string
                                null,              //metadata
                                true,              //is requery
                                null,              //requested types
                                queryUrns);        //requested urns (if any)
    }
}
