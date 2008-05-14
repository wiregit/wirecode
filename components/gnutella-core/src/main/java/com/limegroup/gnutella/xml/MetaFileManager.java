package com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.NameValue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileManagerController;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.FileManagerImpl;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.FileManagerEvent.Type;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 * This class handles querying shared files with XML data and returning XML data
 * in replies.
 */
@Singleton
public class MetaFileManager extends FileManagerImpl {

    private static final Log LOG = LogFactory.getLog(MetaFileManager.class);

    private Saver saver;

    @Inject
    public MetaFileManager(FileManagerController fileManagerController) {
        super(fileManagerController);
    }
    

    /**
     * Overrides FileManager.query.
     * 
     * Used to search XML information in addition to normal searches.
     */
    @Override
    public synchronized Response[] query(QueryRequest request) {
        Response[] result = super.query(request);

        if (shouldIncludeXMLInResponse(request)) {
            LimeXMLDocument doc = request.getRichQuery();
            if (doc != null) {
                Response[] metas = query(doc);
                if (metas != null) // valid query & responses.
                    result = union(result, metas, doc);
            }
        }

        return result;
    }

    /**
     * Determines if this file has a valid XML match.
     */
    @Override
    protected boolean isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return LimeXMLUtils.match(r.getDocument(), doc, true);
    }

    /**
     * Returns whether or not a response to this query should include XML.
     * Currently only includes XML if the request desires it or if the request
     * wants an out of band reply.
     */
    @Override
    protected boolean shouldIncludeXMLInResponse(QueryRequest qr) {
        return qr.desiresXMLResponses() || qr.desiresOutOfBandReplies();
    }

    /**
     * Adds XML to the response. This assumes that shouldIncludeXMLInResponse
     * was already consulted and returned true.
     * 
     * If the FileDesc has no XML documents, this does nothing. If the FileDesc
     * has one XML document, this sets it as the response doc. If the FileDesc
     * has multiple XML documents, this does nothing. The reasoning behind not
     * setting the document when there are multiple XML docs is that presumably
     * the query will be a 'rich' query, and we want to include only the schema
     * that was in the query.
     * 
     * @param response the <tt>Response</tt> instance that XML should be added
     *        to
     * @param fd the <tt>FileDesc</tt> that provides access to the
     *        <tt>LimeXMLDocuments</tt> to add to the response
     */
    @Override
    protected void addXMLToResponse(Response response, FileDesc fd) {
        List<LimeXMLDocument> docs = fd.getLimeXMLDocuments();
        if (docs.size() == 0)
            return;
        if (docs.size() == 1)
            response.setDocument(docs.get(0));
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
        final Long cTime = fileManagerController.getCreationTime(fd.getSHA1Urn());

        List<LimeXMLDocument> xmlDocs = fd.getLimeXMLDocuments();
        if (LimeXMLUtils.isEditableFormat(f)) {
            try {
                LimeXMLDocument diskDoc = fileManagerController.readDocument(f);
                xmlDocs = resolveWriteableDocs(xmlDocs, diskDoc);
            } catch (IOException e) {
                // if we were unable to read this document,
                // then simply add the file without metadata.
                xmlDocs = Collections.emptyList();
            }
        }

        final FileDesc removed = removeFileIfSharedOrStore(f, false);
        assert fd == removed : "wanted to remove: " + fd + "\ndid remove: " + removed;

        synchronized (this) {
            _needRebuild = true;
        }

        addFileIfSharedOrStore(f, xmlDocs, false, _revision, new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                // Retarget the event for the GUI.
                FileManagerEvent newEvt = null;

                if (evt.isAddEvent() || evt.isAddStoreEvent()) {
                    FileDesc fd = evt.getFileDescs()[0];
                  	fileManagerController.fileChanged(fd.getSHA1Urn(), cTime);
                    newEvt = new FileManagerEvent(MetaFileManager.this, Type.CHANGE_FILE, removed,
                            fd);
                } else {
                    newEvt = new FileManagerEvent(MetaFileManager.this, Type.REMOVE_FILE, removed);
                }
                dispatchFileEvent(newEvt);
            }
        }, AddType.ADD_SHARE);
    }

    /**
     * Finds the audio metadata document in allDocs, and makes it's id3 fields
     * identical with the fields of id3doc (which are only id3).
     */
    private List<LimeXMLDocument> resolveWriteableDocs(List<LimeXMLDocument> allDocs,
            LimeXMLDocument id3Doc) {
        LimeXMLDocument audioDoc = null;
        LimeXMLSchema audioSchema = fileManagerController.getSchema(LimeXMLNames.AUDIO_SCHEMA);

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
        audioDoc = fileManagerController.createLimeXMLDocument(id3List, LimeXMLNames.AUDIO_SCHEMA);
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
            LimeXMLReplyCollection col = fileManagerController.getReplyCollection(uri);
            if (col != null)
                col.removeDoc(fd);
        }
        _needRebuild = true;
    }

    /**
     * Notification that FileManager loading is starting.
     */
    @Override
    protected void loadStarted(int revision) {
        fileManagerController.setAnnotateEnabled(false);

        // Load up new ReplyCollections.
        String[] schemas = fileManagerController.getAvailableSchemaURIs();
        for (int i = 0; i < schemas.length; i++) {
            fileManagerController.add(schemas[i], fileManagerController
                    .createLimeXMLReplyCollection(schemas[i]));
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
            fileManagerController.scheduleWithFixedDelay(saver, 60 * 1000, 60 * 1000,
                    TimeUnit.MILLISECONDS);
        }

        Collection<LimeXMLReplyCollection> replies = fileManagerController.getCollections();
        for (LimeXMLReplyCollection col : replies)
            col.loadFinished();

        fileManagerController.setAnnotateEnabled(true);

        super.loadFinished(revision);
    }

    /**
     * Notification that a single FileDesc has its URNs.
     */
    @Override
    protected void loadFile(FileDesc fd, File file, List<? extends LimeXMLDocument> metadata,
            Set<? extends URN> urns) {
        super.loadFile(fd, file, metadata, urns);
        boolean added = false;

        Collection<LimeXMLReplyCollection> replies = fileManagerController.getCollections();
        for (LimeXMLReplyCollection col : replies)
            added |= col.initialize(fd, metadata) != null;
        for (LimeXMLReplyCollection col : replies)
            added |= col.createIfNecessary(fd) != null;

        if (added) {
            synchronized (this) {
                _needRebuild = true;
            }
        }

    }

    @Override
    protected void save() {
        if (isLoadFinished()) {
            Collection<LimeXMLReplyCollection> replies = fileManagerController.getCollections();
            for (LimeXMLReplyCollection col : replies)
                col.writeMapToDisk();
        }

        super.save();
    }

    /**
     * Creates a new array, the size of which is less than or equal to
     * normals.length + metas.length.
     */
    private Response[] union(Response[] normals, Response[] metas, LimeXMLDocument requested) {
        if (normals == null || normals.length == 0)
            return metas;
        if (metas == null || metas.length == 0)
            return normals;

        // It is important to use a HashSet here so that duplicate
        // responses are not sent.
        // Unfortunately, it is still possible that one Response
        // did not have metadata but the other did, causing two
        // responses for the same file.

        Set<Response> unionSet = new HashSet<Response>();
        for (int i = 0; i < metas.length; i++)
            unionSet.add(metas[i]);
        for (int i = 0; i < normals.length; i++)
            unionSet.add(normals[i]);

        // The set contains all the elements that are the union of the 2 arrays
        Response[] retArray = new Response[unionSet.size()];
        retArray = unionSet.toArray(retArray);
        return retArray;
    }

    /**
     * build the QRT table call to super.buildQRT and add XML specific Strings
     * to QRT
     */
    @Override
    protected void buildQRT() {
        super.buildQRT();
        for (String string : getXMLKeyWords())
            _queryRouteTable.add(string);

        for (String string : getXMLIndivisibleKeyWords())
            _queryRouteTable.addIndivisible(string);
    }

    /**
     * Returns a list of all the words in the annotations - leaves out numbers.
     * The list also includes the set of words that is contained in the names of
     * the files.
     */
    private List<String> getXMLKeyWords() {
        List<String> words = new ArrayList<String>();
        // Now get a list of keywords from each of the ReplyCollections
        String[] schemas = fileManagerController.getAvailableSchemaURIs();
        LimeXMLReplyCollection collection;
        int len = schemas.length;
        for (int i = 0; i < len; i++) {
            collection = fileManagerController.getReplyCollection(schemas[i]);
            if (collection == null)// not loaded? skip it and keep goin'
                continue;
            words.addAll(collection.getKeyWords());
        }
        return words;
    }

    /**
     * @return A List of KeyWords from the FS that one does NOT want broken upon
     *         hashing into a QRT. Initially being used for schema uri hashing.
     */
    private List<String> getXMLIndivisibleKeyWords() {
        List<String> words = new ArrayList<String>();
        String[] schemas = fileManagerController.getAvailableSchemaURIs();
        LimeXMLReplyCollection collection;
        for (int i = 0; i < schemas.length; i++) {
            if (schemas[i] != null)
                words.add(schemas[i]);
            collection = fileManagerController.getReplyCollection(schemas[i]);
            if (collection == null)// not loaded? skip it and keep goin'
                continue;
            words.addAll(collection.getKeyWordsIndivisible());
        }
        return words;
    }

    /**
     * Returns an array of Responses that correspond to documents that have a
     * match given query document.
     */
    private Response[] query(LimeXMLDocument queryDoc) {
        String schema = queryDoc.getSchemaURI();
        LimeXMLReplyCollection replyCol = fileManagerController.getReplyCollection(schema);
        if (replyCol == null)// no matching reply collection for schema
            return null;

        List<LimeXMLDocument> matchingReplies = replyCol.getMatchingReplies(queryDoc);
        // matchingReplies = a List of LimeXMLDocuments that match the query
        int s = matchingReplies.size();
        if (s == 0) // no matching replies.
            return null;

        Response[] retResponses = new Response[s];
        int z = 0;
        for (LimeXMLDocument currDoc : matchingReplies) {
            File file = currDoc.getIdentifier();// returns null if none
            Response res = null;
            if (file == null) { // pure metadata (no file)
                res = fileManagerController.createPureMetadataResponse();
            } else { // meta-data about a specific file
                FileDesc fd = getFileDescForFile(file);
                if (fd == null || fd instanceof IncompleteFileDesc || isStoreFileLoaded(file)) {
                    // fd == null is bad -- would mean MetaFileManager is out of sync.
                    // fd incomplete should never happen, but apparently is somehow...
                    // fd is store file, shouldn't be returning query hits for it then..
                    continue;
                } else { // we found a file with the right name
                    res = fileManagerController.createResponse(fd);
                    fd.incrementHitCount();
                    fileManagerController.handleSharedFileUpdate(fd.getFile());
                }
            }

            // Note that if any response was invalid,
            // the array will be too small, and we'll
            // have to resize it.
            res.setDocument(currDoc);
            retResponses[z] = res;
            z++;
        }

        if (z == 0)
            return null; // no responses

        // need to ensure that no nulls are returned in my response[]
        // z is a count of responses constructed, see just above...
        // s == retResponses.length
        if (z < s) {
            Response[] temp = new Response[z];
            System.arraycopy(retResponses, 0, temp, 0, z);
            retResponses = temp;
        }

        return retResponses;
    }

    private class Saver implements Runnable {
        public void run() {
            if (!shutdown && isLoadFinished())
                save();
        }
    }
}
