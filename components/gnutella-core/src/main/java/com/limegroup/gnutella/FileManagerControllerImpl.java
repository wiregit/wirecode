package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLProperties;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLReplyCollectionFactory;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

@Singleton
public class FileManagerControllerImpl implements FileManagerController {
    
    private final Provider<UrnCache> urnCache;
    private final Provider<DownloadManager> downloadManager;
    private final Provider<CreationTimeCache> creationTimeCache;
    private final Provider<ContentManager> contentManager;
    private final Provider<AltLocManager> altLocManager;
    private final Provider<ResponseFactory> responseFactory;
    private final Provider<SavedFileManager> savedFileManager;
    private final Provider<SimppManager> simppManager;
    private final Provider<UpdateHandler> updateHandler;
    private final Provider<ActivityCallback> activityCallback;
    private final ScheduledExecutorService backgroundExecutor;
    private final LimeXMLReplyCollectionFactory limeXMLReplyCollectionFactory;
    private final LimeXMLDocumentFactory limeXMLDocumentFactory;
    private final MetaDataReader metaDataReader;
    private final Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper;
    private final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;
    
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
            Provider<AltLocManager> altLocManager,
            Provider<ResponseFactory> responseFactory,
            Provider<SavedFileManager> savedFileManager,
            Provider<SimppManager> simppManager,
            Provider<UpdateHandler> updateHandler,
            Provider<ActivityCallback> activityCallback,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            LimeXMLReplyCollectionFactory limeXMLReplyCollectionFactory,
            LimeXMLDocumentFactory limeXMLDocumentFactory,
            MetaDataReader metaDataReader,
            Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.urnCache = urnCache;
        this.downloadManager = downloadManager;
        this.creationTimeCache = creationTimeCache;
        this.contentManager = contentManager;
        this.altLocManager = altLocManager;
        this.responseFactory = responseFactory;
        this.savedFileManager = savedFileManager;
        this.simppManager = simppManager;
        this.updateHandler = updateHandler;
        this.activityCallback = activityCallback;
        this.backgroundExecutor = backgroundExecutor;
        this.limeXMLReplyCollectionFactory = limeXMLReplyCollectionFactory;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.metaDataReader = metaDataReader;
        this.schemaReplyCollectionMapper = schemaReplyCollectionMapper;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
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
    public List<URN> getNewestSharedUrns(QueryRequest qr, int number) {
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

    public Response createPureMetadataResponse() {
        return responseFactory.get().createResponse(LimeXMLProperties.DEFAULT_NONFILE_INDEX, 0, " ");
    }

    public Response createResponse(FileDesc desc) {
        return responseFactory.get().createResponse(desc);
    }

    public void loadFinishedPostSave() {
        savedFileManager.get().run();
        updateHandler.get().tryToDownloadUpdates();
        activityCallback.get().fileManagerLoaded();
    }

    public void addSimppListener(SimppListener listener) {
        simppManager.get().addListener(listener);
    }

    public void removeSimppListener(SimppListener listener) {
        simppManager.get().removeListener(listener);
    }

    public void fileManagerLoading() {
        activityCallback.get().fileManagerLoading();
    }

    public void handleSharedFileUpdate(File file) {
        activityCallback.get().handleSharedFileUpdate(file);
    }

    public void scheduleWithFixedDelay(Runnable command, int initialDelay, int delay, TimeUnit unit) {
        backgroundExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public void setAnnotateEnabled(boolean enabled) {
        activityCallback.get().setAnnotateEnabled(enabled);
    }

    public boolean warnAboutSharingSensitiveDirectory(File directory) {
        return activityCallback.get().warnAboutSharingSensitiveDirectory(directory);
    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI) {
        return limeXMLReplyCollectionFactory.createLimeXMLReplyCollection(URI);
    }

    public LimeXMLDocument createLimeXMLDocument(
            Collection<? extends Entry<String, String>> nameValueList,
            String schemaURI) {
        return limeXMLDocumentFactory.createLimeXMLDocument(nameValueList, schemaURI);
    }

    public LimeXMLDocument readDocument(File file) throws IOException {
        return metaDataReader.readDocument(file);
    }

    public void add(String schemaURI,
            LimeXMLReplyCollection replyCollection) {
        schemaReplyCollectionMapper.get().add(schemaURI, replyCollection);
    }

    public Collection<LimeXMLReplyCollection> getCollections() {
        return schemaReplyCollectionMapper.get().getCollections();
    }

    public LimeXMLReplyCollection getReplyCollection(String schemaURI) {
        return schemaReplyCollectionMapper.get().getReplyCollection(schemaURI);
    }

    public String[] getAvailableSchemaURIs() {
        return limeXMLSchemaRepository.get().getAvailableSchemaURIs();
    }

    public LimeXMLSchema getSchema(String uri) {
        return limeXMLSchemaRepository.get().getSchema(uri);
    }

}
