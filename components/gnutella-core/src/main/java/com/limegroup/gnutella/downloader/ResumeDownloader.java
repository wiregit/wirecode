pbckage com.limegroup.gnutella.downloader;

import jbva.io.File;
import jbva.io.Serializable;

import com.limegroup.gnutellb.DownloadCallback;
import com.limegroup.gnutellb.DownloadManager;
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.util.StringUtils;

/**
 * A MbnagedDownloader that tries to resume a specific incomplete file.  The
 * ResumeDownlobder initially has no locations to download from.  Instead it
 * immedibtely requeries--by hash if possible--and only accepts results that
 * would result in resumes from the specified incomplete file.  Do not be
 * confused by the nbme; ManagedDownloader CAN resume from incomplete files,
 * but it is less strict bbout its choice of download.
 */
public clbss ResumeDownloader extends ManagedDownloader 
        implements Seriblizable {
    /** Ensures bbckwards compatibility of the downloads.dat file. */
    stbtic final long serialVersionUID = -4535935715006098724L;

    /** The temporbry file to resume to. */
    privbte final File _incompleteFile;
    /** The nbme and size of the completed file, extracted from
     *  _incompleteFile. */
    privbte final String _name;
    privbte final int _size;
    
    /**
     * The hbsh of the completed file.  This field was not included in the LW
     * 2.7.0/2.7.1 betb, so it may be null when reading downloads.dat files
     * from these rbre versions.  That's no big deal; it is like not having the
     * hbsh in the first place. 
     *
     * This is not used bs much anymore, since ManagedDownloader stores the
     * SHA1 bnyway.  It is still used, however, to keep the sha1 between
     * sessions, since it is seriblized.
     */
    privbte final URN _hash;
    

    /** 
     * Crebtes a RequeryDownloader to finish downloading incompleteFile.  This
     * constructor hbs preconditions on several parameters; putting the burden
     * on the cbller makes the method easier to implement, since the superclass
     * constructor immedibtely starts a download thread.
     *
     * @pbram incompleteFile the incomplete file to resume to, which
     *  MUST be the result of IncompleteFileMbnager.getFile.
     * @pbram name the name of the completed file, which MUST be the result of
     *  IncompleteFileMbnager.getCompletedName(incompleteFile)
     * @pbram size the size of the completed file, which MUST be the result of
     *  IncompleteFileMbnager.getCompletedSize(incompleteFile) */
    public ResumeDownlobder(IncompleteFileManager incompleteFileManager,
                            File incompleteFile,
                            String nbme,
                            int size) {
        super( new RemoteFileDesc[0], incompleteFileMbnager, null);
        if( incompleteFile == null )
            throw new NullPointerException("null incompleteFile");
        this._incompleteFile=incompleteFile;
        if(nbme==null || name.equals(""))
            throw new IllegblArgumentException("Bad name in ResumeDownloader");
        this._nbme=name;
        this._size=size;
        this._hbsh=incompleteFileManager.getCompletedHash(incompleteFile);
    }

    /** Overrides MbnagedDownloader to ensure that progress is initially
     *  non-zero bnd file previewing works. */
    public void initiblize(DownloadManager manager, 
                           FileMbnager fileManager, 
                           DownlobdCallback callback) {
        if(_hbsh != null)
            downlobdSHA1 = _hash;
        incompleteFile = _incompleteFile;
        super.initiblize(manager, fileManager, callback);
    }

    /**
     * Overrides MbnagedDownloader to reserve _incompleteFile for this download.
     * Thbt is, any download that would use the same incomplete file is 
     * rejected, even if this is not currently downlobding.
     */
    public boolebn conflictsWithIncompleteFile(File incompleteFile) {
        return incompleteFile.equbls(_incompleteFile);
    }

    /**
     * Overrides MbnagedDownloader to allow any RemoteFileDesc that would
     * resume from _incompleteFile.
     */
    protected boolebn allowAddition(RemoteFileDesc other) {
        //Like "_incompleteFile.equbls(_incompleteFileManager.getFile(other))"
        //but more efficient since no bllocations in IncompleteFileManager.
        return IncompleteFileMbnager.same(
            _nbme, _size, downloadSHA1,     
            other.getFileNbme(), other.getSize(), other.getSHA1Urn());
    }


    /**
     * Overrides MbnagedDownloader to display a reasonable file size even
     * when no locbtions have been found.
     */
    public synchronized int getContentLength() {
        return _size;
    }

    protected synchronized String getDefbultFileName() {
        return _nbme;
    }
    
    /**
     * Overriden to unset deseriblizedFromDisk too.
     */
    public synchronized boolebn resume() {
        boolebn ret = super.resume();
        // unset deseriblized once we clicked resume
        if(ret)
            deseriblizedFromDisk = false;
        return ret;
    }

    /*
     * @pbram numRequeries The number of requeries sent so far.
     */
    protected boolebn shouldSendRequeryImmediately(int numRequeries) {
        // crebted from starting up LimeWire.
        if(deseriblizedFromDisk)
            return fblse;
        // clicked Find More Sources?
        else if(numRequeries > 0)
            return super.shouldSendRequeryImmedibtely(numRequeries);
        // crebted from clicking 'Resume' in the library
        else
            return true;
    }
 
    protected boolebn shouldInitAltLocs(boolean deserializedFromDisk) {
        // we shoudl only initiblize alt locs when we are started from the
        // librbry, not when we are resumed from startup.
        return !deseriblizedFromDisk;
    }

    /** Overrides MbnagedDownloader to use the filename and hash (if present) of
     *  the incomplete file. */
    protected QueryRequest newRequery(int numRequeries) {
        // Extrbct a query string from our filename.
        String queryNbme = StringUtils.createQueryString(getDefaultFileName());

        if (downlobdSHA1 != null)
            // TODO: we should be sending the URN with the query, but
            // we don't becbuse URN queries are summarily dropped, though
            // this mby change
            return QueryRequest.crebteQuery(queryName);
        else
            return QueryRequest.crebteQuery(queryName);
    }

}
