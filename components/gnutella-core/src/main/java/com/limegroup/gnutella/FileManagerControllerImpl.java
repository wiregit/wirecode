package com.limegroup.gnutella;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.messages.QueryRequest;

@Singleton
public class FileManagerControllerImpl implements FileManagerController {
    
    private final Provider<UrnCache> urnCache;
    private final Provider<DownloadManager> downloadManager;
    private final Provider<CreationTimeCache> creationTimeCache;
    private final Provider<ContentManager> contentManager;
    private final Provider<AltLocManager> altLocManager;
    
    /**
     * @param urnCache
     * @param downloadManager
     * @param creationTimeCache
     * @param contentManager
     * @param altLocManager
     */
    @Inject
    public FileManagerControllerImpl(Provider<UrnCache> urnCache,
            Provider<DownloadManager> downloadManager,
            Provider<CreationTimeCache> creationTimeCache,
            Provider<ContentManager> contentManager,
            Provider<AltLocManager> altLocManager) {
        this.urnCache = urnCache;
        this.downloadManager = downloadManager;
        this.creationTimeCache = creationTimeCache;
        this.contentManager = contentManager;
        this.altLocManager = altLocManager;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#save()
     */
    public void save() {
        urnCache.get().persistCache();
        creationTimeCache.get().persistCache();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#loadStarted()
     */
    public void loadStarted() {
        urnCache.get().clearPendingHashes(this);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#loadFinished()
     */
    public void loadFinished() {
        creationTimeCache.get().pruneTimes();
        downloadManager.get().getIncompleteFileManager().registerAllIncompleteFiles();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#clearPendingShare(java.io.File)
     */
    public void clearPendingShare(File f) {
        urnCache.get().clearPendingHashesFor(f, this);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#calculateAndCacheUrns(java.io.File, com.limegroup.gnutella.UrnCallback)
     */
    public void calculateAndCacheUrns(File file, UrnCallback callback) {
        urnCache.get().calculateAndCacheUrns(file, callback);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#addUrns(java.io.File, java.util.Set)
     */
    public void addUrns(File file, Set<? extends URN> urns) {
        urnCache.get().addUrns(file, urns);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#fileAdded(java.io.File, com.limegroup.gnutella.URN)
     */
    public void fileAdded(File file, URN urn) {
        CreationTimeCache cache = creationTimeCache.get();
        synchronized (cache) {
            Long cTime = cache.getCreationTime(urn);
            if (cTime == null)
                cTime = new Long(file.lastModified());
            // if cTime is non-null but 0, then the IO subsystem is
            // letting us know that the file was FNF or an IOException
            // occurred - the best course of action is to
            // ignore the issue and not add it to the CTC, hopefully
            // we'll get a correct reading the next time around...
            if (cTime.longValue() > 0) {
                // these calls may be superfluous but are quite fast....
                cache.addTime(urn, cTime.longValue());
                cache.commitTime(urn);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#lastUrnRemoved(com.limegroup.gnutella.URN)
     */
    public void lastUrnRemoved(URN urn) {
        altLocManager.get().purge(urn);
        creationTimeCache.get().removeTime(urn);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#getNewestUrns(com.limegroup.gnutella.messages.QueryRequest, int)
     */
    public List<URN> getNewestUrns(QueryRequest qr, int number) {
        return creationTimeCache.get().getFiles(qr, number);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#getResponseDataFor(com.limegroup.gnutella.URN)
     */
    public ContentResponseData getResponseDataFor(URN urn) {
        return contentManager.get().getResponse(urn);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#requestValidation(com.limegroup.gnutella.URN, com.limegroup.gnutella.auth.ContentResponseObserver)
     */
    public void requestValidation(URN urn, ContentResponseObserver observer) {
        contentManager.get().request(urn, observer, 5000);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.FileManagerController#getAlternateLocationCount(com.limegroup.gnutella.URN)
     */
    public int getAlternateLocationCount(URN urn) {
        return altLocManager.get().getNumLocs(urn);
    }

    public void fileChanged(URN urn, Long time) {
        CreationTimeCache cache = creationTimeCache.get();
        // re-populate the ctCache
        synchronized (cache) {
            cache.removeTime(urn);// addFile() put lastModified
            cache.addTime(urn, time);
            cache.commitTime(urn);
        }   
    }

    public Long getCreationTime(URN urn) {
        return creationTimeCache.get().getCreationTime(urn);
    }

}
