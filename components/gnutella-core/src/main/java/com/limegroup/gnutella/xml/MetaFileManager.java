package com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.NameValue;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.FileManagerImpl;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.FileManagerEvent.Type;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * This class handles querying shared files with XML data and returning XML data
 * in replies.
 */
@Singleton
public class MetaFileManager extends FileManagerImpl {

    private static final Log LOG = LogFactory.getLog(MetaFileManager.class);

    private Saver saver;

    @Inject
    public MetaFileManager(Provider<SimppManager> simppManager,
            Provider<UrnCache> urnCache,
            Provider<DownloadManager> downloadManager,
            Provider<CreationTimeCache> creationTimeCache,
            Provider<ContentManager> contentManager,
            Provider<AltLocManager> altLocManager,
            Provider<SavedFileManager> savedFileManager,
            Provider<UpdateHandler> updateHandler,
            Provider<ActivityCallback> activityCallback,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            LimeXMLReplyCollectionFactory limeXMLReplyCollectionFactory,
            LimeXMLDocumentFactory limeXMLDocumentFactory,
            MetaDataReader metaDataReader,
            Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        super(simppManager, urnCache, downloadManager, creationTimeCache, contentManager, altLocManager, savedFileManager, updateHandler, activityCallback, backgroundExecutor, limeXMLReplyCollectionFactory, limeXMLDocumentFactory, metaDataReader, schemaReplyCollectionMapper, limeXMLSchemaRepository);
    }
    
    /**
     * Notification that a file has changed. This implementation is different
     * than FileManager's in that it maintains the XML.
     * 
     * Important note: This method is called AFTER the file has changed. It is
     * possible that the metadata we wanted to write did not get written out
     * completely. We should NOT attempt to add the old metadata again, because
     * we may end up recursing infinitely trying to write this metadata.
     * However, it isn't very robust to blindly assume that the only metadata
     * associated with this file was audio metadata. So, we make use of the fact
     * that loadFile will only add one type of metadata per file. We read the
     * document tags off the file and insert it first into the list, ensuring
     * that the existing metadata is the one that's added, short-circuiting any
     * infinite loops.
     */
    @Override
    public void fileChanged(File f) {
        if (LOG.isTraceEnabled())
            LOG.debug("File Changed: " + f);

        FileDesc fd = getFileDescForFile(f);
        if (fd == null)
            return;

        // store the creation time for later re-input
        final Long cTime = creationTimeCache.get().getCreationTime(fd.getSHA1Urn());

        List<LimeXMLDocument> xmlDocs = fd.getLimeXMLDocuments();
        if (LimeXMLUtils.isEditableFormat(f)) {
            try {
                LimeXMLDocument diskDoc = metaDataReader.readDocument(f);
                xmlDocs = resolveWriteableDocs(xmlDocs, diskDoc);
            } catch (IOException e) {
                // if we were unable to read this document,
                // then simply add the file without metadata.
                xmlDocs = Collections.emptyList();
            }
        }

        final FileDesc removed = removeFileIfSharedOrStore(f, false);
        assert fd == removed : "wanted to remove: " + fd + "\ndid remove: " + removed;

        addFileIfSharedOrStore(f, xmlDocs, false, _revision, new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                // Retarget the event for the GUI.
                FileManagerEvent newEvt = null;

                if (evt.isAddEvent() || evt.isAddStoreEvent()) {
                    FileDesc fd = evt.getFileDescs()[0];
                  	fileChanged(fd.getSHA1Urn(), cTime);
                    newEvt = new FileManagerEvent(MetaFileManager.this, Type.CHANGE_FILE, removed,
                            fd);
                } else {
                    newEvt = new FileManagerEvent(MetaFileManager.this, Type.REMOVE_FILE, removed);
                }
                dispatchFileEvent(newEvt);
            }
        }, AddType.ADD_SHARE);
    }
    
    private void fileChanged(URN urn, Long time) {
        CreationTimeCache cache = creationTimeCache.get();
        // re-populate the ctCache
        synchronized (cache) {
            cache.removeTime(urn);// addFile() put lastModified
            cache.addTime(urn, time);
            cache.commitTime(urn);
        }   
    }

    /**
     * Finds the audio metadata document in allDocs, and makes it's id3 fields
     * identical with the fields of id3doc (which are only id3).
     */
    private List<LimeXMLDocument> resolveWriteableDocs(List<LimeXMLDocument> allDocs,
            LimeXMLDocument id3Doc) {
        LimeXMLDocument audioDoc = null;
        LimeXMLSchema audioSchema = limeXMLSchemaRepository.get().getSchema(LimeXMLNames.AUDIO_SCHEMA);
        
        for (LimeXMLDocument doc : allDocs) {
            if (doc.getSchema() == audioSchema) {
                audioDoc = doc;
                break;
            }
        }

        if (id3Doc.equals(audioDoc)) // No issue -- both documents are the
                                        // same
            return allDocs; // did not modify list, keep using it

        List<LimeXMLDocument> retList = new ArrayList<LimeXMLDocument>();
        retList.addAll(allDocs);

        if (audioDoc == null) {// nothing to resolve
            retList.add(id3Doc);
            return retList;
        }

        // OK. audioDoc exists, remove it
        retList.remove(audioDoc);

        // now add the non-id3 tags from audioDoc to id3doc
        List<NameValue<String>> audioList = audioDoc.getOrderedNameValueList();
        List<NameValue<String>> id3List = id3Doc.getOrderedNameValueList();
        for (int i = 0; i < audioList.size(); i++) {
            NameValue<String> nameVal = audioList.get(i);
            if (AudioMetaData.isNonLimeAudioField(nameVal.getName()))
                id3List.add(nameVal);
        }
        audioDoc = limeXMLDocumentFactory.createLimeXMLDocument(id3List, LimeXMLNames.AUDIO_SCHEMA);
        retList.add(audioDoc);
        return retList;
    }

    /**
     * Removes the LimeXMLDocuments associated with the removed FileDesc from
     * the various LimeXMLReplyCollections.
     */
    @Override
    protected synchronized FileDesc removeFileIfShared(File f, boolean notify) {
        FileDesc fd = super.removeFileIfShared(f, notify);
        // nothing removed, ignore.
        if (fd == null)
            return null;

        removeFileDesc(fd, notify);
        return fd;
    }
    
    /**
     * Removes the LimeXMLDocuments associated with the removed FileDesc from
     * the various LimeXMLReplyCollections.
     */
    @Override 
    protected synchronized FileDesc removeStoreFile(File f, boolean notify) {
    	FileDesc fd = super.removeStoreFile(f,notify);
        // nothing removed, ignore.
        if (fd == null)
            return null;

        removeFileDesc(fd, notify);
        return fd;
    }
    
    private synchronized void removeFileDesc(FileDesc fd, boolean notify) {
        // Get the schema URI of each document and remove from the collection
        // We must remember the schemas and then remove the doc, or we will
        // get a concurrent mod exception because removing the doc also
        // removes it from the FileDesc.
        List<LimeXMLDocument> xmlDocs = fd.getLimeXMLDocuments();
        List<String> schemas = new LinkedList<String>();
        for (LimeXMLDocument doc : xmlDocs)
            schemas.add(doc.getSchemaURI());
        for (String uri : schemas) {
            LimeXMLReplyCollection col = schemaReplyCollectionMapper.get().getReplyCollection(uri);
            if (col != null)
                col.removeDoc(fd);
        }
    }

    /**
     * Notification that FileManager loading is starting.
     */
    @Override
    protected void loadStarted(int revision) {
        activityCallback.get().setAnnotateEnabled(false);

        // Load up new ReplyCollections.
        String[] schemas = limeXMLSchemaRepository.get().getAvailableSchemaURIs();
        for (int i = 0; i < schemas.length; i++) {
            schemaReplyCollectionMapper.get().add(schemas[i], 
                    limeXMLReplyCollectionFactory.createLimeXMLReplyCollection(schemas[i]));
        }

        super.loadStarted(revision);
    }

    /**
     * Notification that FileManager loading is finished.
     */
    @Override
    protected void loadFinished(int revision) {
        // save ourselves to disk every minute
        if (saver == null) {
            saver = new Saver();
            backgroundExecutor.scheduleWithFixedDelay(saver, 60 * 1000, 60 * 1000, 
                    TimeUnit.MILLISECONDS);
        }

        Collection<LimeXMLReplyCollection> replies = schemaReplyCollectionMapper.get().getCollections();
        for (LimeXMLReplyCollection col : replies)
            col.loadFinished();

        activityCallback.get().setAnnotateEnabled(true);

        super.loadFinished(revision);
    }

    /**
     * Notification that a single FileDesc has its URNs.
     */
    @Override
    protected void loadFile(FileDesc fd, File file, List<? extends LimeXMLDocument> metadata,
            Set<? extends URN> urns) {
        super.loadFile(fd, file, metadata, urns);

        Collection<LimeXMLReplyCollection> replies = schemaReplyCollectionMapper.get().getCollections();
        for (LimeXMLReplyCollection col : replies)
            col.initialize(fd, metadata);
        for (LimeXMLReplyCollection col : replies)
            col.createIfNecessary(fd);
    }

    @Override
    protected void save() {
        if (isLoadFinished()) {
            Collection<LimeXMLReplyCollection> replies = schemaReplyCollectionMapper.get().getCollections();
            for (LimeXMLReplyCollection col : replies)
                col.writeMapToDisk();
        }

        super.save();
    }

    private class Saver implements Runnable {
        public void run() {
            if (!shutdown && isLoadFinished())
                save();
        }
    }
}
