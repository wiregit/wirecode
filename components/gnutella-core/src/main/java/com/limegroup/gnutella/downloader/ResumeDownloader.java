package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import java.io.*;
import com.sun.java.util.collections.*;

public class ResumeDownloader extends ManagedDownloader 
        implements Serializable {
    private final IncompleteFileManager _incompleteFileManager;
    private final File _incompleteFile;
    private String _name;
    private int _size;

    /** Creates a RequeryDownloader to finish downloading incompleteFile.
     *  @param incompleteFile the incomplete file to resume to */
    public ResumeDownloader(DownloadManager manager,
                            FileManager fileManager,
                            IncompleteFileManager incompleteFileManager,
                            ActivityCallback callback,
                            File incompleteFile) {
        super(manager, new RemoteFileDesc[0], fileManager,
              incompleteFileManager,callback);
        this._incompleteFileManager=incompleteFileManager;
        this._incompleteFile=incompleteFile;
        try {
            this._name=incompleteFileManager.getCompletedName(_incompleteFile);
            this._size=ByteOrder.long2int(
                incompleteFileManager.getCompletedSize(_incompleteFile));
        } catch (IllegalArgumentException e) {
            Assert.that(false, _incompleteFile+" wasn't from IFM");
        }
    }

//      /** Necessary to make tryAllDownloads send a requery right away. */
//      protected int getMinutesToWaitForRequery(int requeries) { 
//          if (requeries==0)
//              return 0;
//          else
//              return 5;
//      }

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
