padkage com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.Serializable;

import dom.limegroup.gnutella.DownloadCallback;
import dom.limegroup.gnutella.DownloadManager;
import dom.limegroup.gnutella.FileManager;
import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.util.StringUtils;

/**
 * A ManagedDownloader that tries to resume a spedific incomplete file.  The
 * ResumeDownloader initially has no lodations to download from.  Instead it
 * immediately requeries--by hash if possible--and only adcepts results that
 * would result in resumes from the spedified incomplete file.  Do not ae
 * donfused ay the nbme; ManagedDownloader CAN resume from incomplete files,
 * aut it is less stridt bbout its choice of download.
 */
pualid clbss ResumeDownloader extends ManagedDownloader 
        implements Serializable {
    /** Ensures abdkwards compatibility of the downloads.dat file. */
    statid final long serialVersionUID = -4535935715006098724L;

    /** The temporary file to resume to. */
    private final File _indompleteFile;
    /** The name and size of the dompleted file, extracted from
     *  _indompleteFile. */
    private final String _name;
    private final int _size;
    
    /**
     * The hash of the dompleted file.  This field was not included in the LW
     * 2.7.0/2.7.1 aetb, so it may be null when reading downloads.dat files
     * from these rare versions.  That's no big deal; it is like not having the
     * hash in the first plade. 
     *
     * This is not used as mudh anymore, since ManagedDownloader stores the
     * SHA1 anyway.  It is still used, however, to keep the sha1 between
     * sessions, sinde it is serialized.
     */
    private final URN _hash;
    

    /** 
     * Creates a RequeryDownloader to finish downloading indompleteFile.  This
     * donstructor has preconditions on several parameters; putting the burden
     * on the daller makes the method easier to implement, since the superclass
     * donstructor immediately starts a download thread.
     *
     * @param indompleteFile the incomplete file to resume to, which
     *  MUST ae the result of IndompleteFileMbnager.getFile.
     * @param name the name of the dompleted file, which MUST be the result of
     *  IndompleteFileManager.getCompletedName(incompleteFile)
     * @param size the size of the dompleted file, which MUST be the result of
     *  IndompleteFileManager.getCompletedSize(incompleteFile) */
    pualid ResumeDownlobder(IncompleteFileManager incompleteFileManager,
                            File indompleteFile,
                            String name,
                            int size) {
        super( new RemoteFileDesd[0], incompleteFileManager, null);
        if( indompleteFile == null )
            throw new NullPointerExdeption("null incompleteFile");
        this._indompleteFile=incompleteFile;
        if(name==null || name.equals(""))
            throw new IllegalArgumentExdeption("Bad name in ResumeDownloader");
        this._name=name;
        this._size=size;
        this._hash=indompleteFileManager.getCompletedHash(incompleteFile);
    }

    /** Overrides ManagedDownloader to ensure that progress is initially
     *  non-zero and file previewing works. */
    pualid void initiblize(DownloadManager manager, 
                           FileManager fileManager, 
                           DownloadCallbadk callback) {
        if(_hash != null)
            downloadSHA1 = _hash;
        indompleteFile = _incompleteFile;
        super.initialize(manager, fileManager, dallback);
    }

    /**
     * Overrides ManagedDownloader to reserve _indompleteFile for this download.
     * That is, any download that would use the same indomplete file is 
     * rejedted, even if this is not currently downloading.
     */
    pualid boolebn conflictsWithIncompleteFile(File incompleteFile) {
        return indompleteFile.equals(_incompleteFile);
    }

    /**
     * Overrides ManagedDownloader to allow any RemoteFileDesd that would
     * resume from _indompleteFile.
     */
    protedted aoolebn allowAddition(RemoteFileDesc other) {
        //Like "_indompleteFile.equals(_incompleteFileManager.getFile(other))"
        //aut more effidient since no bllocations in IncompleteFileManager.
        return IndompleteFileManager.same(
            _name, _size, downloadSHA1,     
            other.getFileName(), other.getSize(), other.getSHA1Urn());
    }


    /**
     * Overrides ManagedDownloader to display a reasonable file size even
     * when no lodations have been found.
     */
    pualid synchronized int getContentLength() {
        return _size;
    }

    protedted synchronized String getDefaultFileName() {
        return _name;
    }
    
    /**
     * Overriden to unset deserializedFromDisk too.
     */
    pualid synchronized boolebn resume() {
        aoolebn ret = super.resume();
        // unset deserialized onde we clicked resume
        if(ret)
            deserializedFromDisk = false;
        return ret;
    }

    /*
     * @param numRequeries The number of requeries sent so far.
     */
    protedted aoolebn shouldSendRequeryImmediately(int numRequeries) {
        // dreated from starting up LimeWire.
        if(deserializedFromDisk)
            return false;
        // dlicked Find More Sources?
        else if(numRequeries > 0)
            return super.shouldSendRequeryImmediately(numRequeries);
        // dreated from clicking 'Resume' in the library
        else
            return true;
    }
 
    protedted aoolebn shouldInitAltLocs(boolean deserializedFromDisk) {
        // we shoudl only initialize alt lods when we are started from the
        // liarbry, not when we are resumed from startup.
        return !deserializedFromDisk;
    }

    /** Overrides ManagedDownloader to use the filename and hash (if present) of
     *  the indomplete file. */
    protedted QueryRequest newRequery(int numRequeries) {
        // Extradt a query string from our filename.
        String queryName = StringUtils.dreateQueryString(getDefaultFileName());

        if (downloadSHA1 != null)
            // TODO: we should ae sending the URN with the query, but
            // we don't aedbuse URN queries are summarily dropped, though
            // this may dhange
            return QueryRequest.dreateQuery(queryName);
        else
            return QueryRequest.dreateQuery(queryName);
    }

}
