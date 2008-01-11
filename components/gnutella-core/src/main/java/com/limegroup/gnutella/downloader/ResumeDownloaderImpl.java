package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * A ManagedDownloader that tries to resume a specific incomplete file.  The
 * ResumeDownloader initially has no locations to download from.  Instead it
 * immediately requeries--by hash if possible--and only accepts results that
 * would result in resumes from the specified incomplete file.  Do not be
 * confused by the name; ManagedDownloader CAN resume from incomplete files,
 * but it is less strict about its choice of download.
 */
class ResumeDownloaderImpl extends ManagedDownloaderImpl implements ResumeDownloader {
    
    /** The temporary file to resume to. */
    private volatile File _incompleteFile;
    
    /** The name and size of the completed file, extracted from
     *  _incompleteFile. */
    private volatile String _name;
    
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
    @Inject
    ResumeDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            FileManager fileManager, IncompleteFileManager incompleteFileManager,
            DownloadCallback downloadCallback, NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory, RequeryManagerFactory requeryManagerFactory,
            QueryRequestFactory queryRequestFactory, OnDemandUnicaster onDemandUnicaster,
            DownloadWorkerFactory downloadWorkerFactory, AltLocManager altLocManager,
            ContentManager contentManager, SourceRankerFactory sourceRankerFactory,
            UrnCache urnCache, SavedFileManager savedFileManager,
            VerifyingFileFactory verifyingFileFactory, DiskController diskController,
            @Named("ipFilter") IPFilter ipFilter, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter, Provider<TigerTreeCache> tigerTreeCache,
            ApplicationServices applicationServices) {
        super(saveLocationManager, downloadManager, fileManager, incompleteFileManager,
                downloadCallback, networkManager, alternateLocationFactory, requeryManagerFactory,
                queryRequestFactory, onDemandUnicaster, downloadWorkerFactory, altLocManager,
                contentManager, sourceRankerFactory, urnCache, savedFileManager,
                verifyingFileFactory, diskController, ipFilter, backgroundExecutor, messageRouter,
                tigerTreeCache, applicationServices);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.ResumeDownloader#initIncompleteFile(java.io.File, java.lang.String, long)
     */
    public void initIncompleteFile(File incompleteFile, String name, long size) {
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
    @Override
    public void initialize() {
        if (_hash != null)
            downloadSHA1 = _hash;
        incompleteFile = _incompleteFile;
        super.initialize();
        // Auto-activate the requeryManager if this was created
        // from clicking 'Resume' in the library (as opposed to
        // from being deserialized from disk).
        requeryManager.activate();
    }

    /**
     * Overrides ManagedDownloader to reserve _incompleteFile for this download.
     * That is, any download that would use the same incomplete file is
     * rejected, even if this is not currently downloading.
     */
    @Override
    public boolean conflictsWithIncompleteFile(File incompleteFile) {
        return incompleteFile.equals(_incompleteFile);
    }

    /**
     * Overrides ManagedDownloader to allow any RemoteFileDesc that would
     * resume from _incompleteFile.
     */
    @Override
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
    @Override
    public synchronized long getContentLength() {
        return _size64;
    }

    @Override
    protected boolean shouldInitAltLocs() {
        return true;
    }

    /** Overrides ManagedDownloader to use the filename and hash (if present) of
     *  the incomplete file. */
    @Override
    public  QueryRequest newRequery() {
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
}
