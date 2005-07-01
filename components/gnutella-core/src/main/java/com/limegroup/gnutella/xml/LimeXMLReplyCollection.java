package com.limegroup.gnutella.xml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.metadata.AudioMetaData;
import com.limegroup.gnutella.metadata.MetaDataEditor;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.util.ConverterObjectInputStream;
import com.limegroup.gnutella.util.I18NConvert;
import com.limegroup.gnutella.util.Trie;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.NameValue;

/**
 * Maps LimeXMLDocuments for FileDescs in a specific schema.
 */

public class LimeXMLReplyCollection {
    
    private static final Log LOG = LogFactory.getLog(LimeXMLReplyCollection.class);
    

    /**
     * The schemaURI of this collection.
     */
    private final String schemaURI;
    
    /**
     * A map of URN -> LimeXMLDocument for each shared file that contains XML.
     *
     * SYNCHRONIZATION: Synchronize on mainMap when accessing, 
     *  adding or removing.
     */
    private final Map /* URN -> LimeXMLDocument */ mainMap;
    
    /**
     * The old map that was read off disk.
     *
     * Used while initially processing FileDescs to add.
     */
    private final Map /* URN -> LimeXMLDocument */ oldMap;
    
    /**
     * A mapping of fields in the LimeXMLDocument to a Trie
     * that has a lookup table for the values of that field.
     *
     * The Trie value is a mapping of keywords in LimeXMLDocuments
     * to the list of documents that have that keyword.
     *
     * SYNCHRONIZATION: Synchronize on mainMap when accessing,
     *  adding or removing.
     */
    private final Map /* String -> Trie (String -> List) */ trieMap;
    
    /**
     * Whether or not data became dirty after we last wrote to disk.
     */
    private boolean dirty = false;
    
    /**
     * The location on disk that information is serialized to.
     */
    private final File dataFile;

    public static final int NORMAL = 0;
    public static final int FILE_DEFECTIVE = 1;
    public static final int RW_ERROR = 2;
    public static final int BAD_ID3  = 3;
    public static final int FAILED_TITLE  = 4;
    public static final int FAILED_ARTIST  = 5;
    public static final int FAILED_ALBUM  = 6;
    public static final int FAILED_YEAR  = 7;
    public static final int FAILED_COMMENT  = 8;
    public static final int FAILED_TRACK  = 9;
    public static final int FAILED_GENRE  = 10;
    public static final int HASH_FAILED  = 11;
    public static final int INCORRECT_FILETYPE = 12;

    /**
     * Creates a new LimeXMLReplyCollection.  The reply collection
     * will retain only those XMLDocs that match the given schema URI.
     *
     * @param fds The list of shared FileDescs.
     * @param URI This collection's schema URI
     */
    public LimeXMLReplyCollection(String URI) {
        this.schemaURI = URI;
        this.trieMap = new HashMap();
        this.dataFile = new File(LimeXMLProperties.instance().getXMLDocsDir(),
                                 LimeXMLSchema.getDisplayString(schemaURI)+ ".sxml");
        this.mainMap = new HashMap();
        this.oldMap = readMapFromDisk();
    }
    
    /**
     * Initializes the map using either LimeXMLDocuments in the list of potential
     * documents, or elements stored in oldMap.  Items in potential take priority.
     */
    LimeXMLDocument initialize(FileDesc fd, List potential) {
        URN urn = fd.getSHA1Urn();
        LimeXMLDocument doc = null;
        
        // First try to get a doc from the potential list.
        for(Iterator i = potential.iterator(); i.hasNext(); ) {
            LimeXMLDocument next = (LimeXMLDocument)i.next();
            if(next.getSchemaURI().equals(schemaURI)) {
                doc = next;
                break;
            }
        }
        
        // Then try to get it from the old map.
        if(doc == null)
            doc = (LimeXMLDocument)oldMap.get(urn);
        
        
        // Then try and see it, with validation and all.
        if(doc != null) {
            doc = validate(doc, fd.getFile(), fd);
            if(doc != null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Adding old document for file: " + fd.getFile() + ", doc: " + doc);
                    addReply(fd, doc);
            }
        }
        
        return doc;
    }
    
    /**
     * Creates a LimeXMLDocument for the given FileDesc if no XML already exists
     * for it.
     */
    LimeXMLDocument createIfNecessary(FileDesc fd) {
        LimeXMLDocument doc = null;
        URN urn = fd.getSHA1Urn();
        
        if(!mainMap.containsKey(urn)) {
            File file = fd.getFile();
            // If we have no documents for this FD, or the file-format only supports
            // a single kind of metadata, construct a document.
            // This is necessary so that we don't keep trying to parse formats that could
            // be multiple kinds of files every time.   
            if(fd.getLimeXMLDocuments().size() == 0 || !LimeXMLUtils.isSupportedMultipleFormat(file)) {
                doc = constructDocument(file);
                if(doc != null) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Adding newly constructed document for file: " + file + ", doc: " + doc);
                    addReply(fd, doc);
                }
            }
        }
        
        return doc;
    }
    
    /**
     * Notification that initial loading is done.
     */
    void loadFinished() {
        synchronized(mainMap) {
            if(oldMap.equals(mainMap)) {
                dirty = false;
            }
            oldMap.clear();
        }
    
    }
    
    /**
     * Validates a LimeXMLDocument.
     *
     * This checks:
     * 1) If it's current (if not, it attempts to reparse.  If it can't, keeps the old one).
     * 2) If it's valid (if not, attempts to reparse it.  If it can't, drops it).
     * 3) If it's corrupted (if so, fixes & writes the fixed one to disk).
     */
    private LimeXMLDocument validate(LimeXMLDocument doc, File file, FileDesc fd) {
        if(!doc.isCurrent()) {
            if(LOG.isDebugEnabled())
                LOG.debug("reconstructing old document: " + file);
            LimeXMLDocument tempDoc = constructDocument(file);
            if (tempDoc != null)
                doc = update(doc, tempDoc);
            else
                doc.setCurrent();
        }
        
        // Verify the doc has information in it.
        if(!doc.isValid()) {
            //If it is invalid, try and rebuild it.
            doc = constructDocument(file);
            if(doc == null)
                return null;
        }   
            
        // check to see if it's corrupted and if so, fix it.
        if( AudioMetaData.isCorrupted(doc) ) {
            doc = AudioMetaData.fixCorruption(doc);
            mediaFileToDisk(fd, file.getPath(), doc, false);
        }
        
        return doc;
    }
    
    /**
     * Updates an existing old document to be a newer document, but retains all fields
     * that may have been in the old one that are not in the newer (for the case of
     * existing annotations).
     */
    private LimeXMLDocument update(LimeXMLDocument older, LimeXMLDocument newer) {
        Map fields = new HashMap();
        for(Iterator i = newer.getNameValueSet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            fields.put(next.getKey(), next.getValue());
        }
        
        for(Iterator i = older.getNameValueSet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            if(!fields.containsKey(next.getKey()))
                fields.put(next.getKey(), next.getValue());
        }

        List nameValues = new ArrayList(fields.size());
        for(Iterator i = fields.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            nameValues.add(new NameValue((String)next.getKey(), next.getValue()));
        }
        return new LimeXMLDocument(nameValues, newer.getSchemaURI());
     }
        
    
    /**
     * Creates a LimeXMLDocument from the file.  
     * @return null if the format is not supported or parsing fails,
     *  <tt>LimeXMLDocument</tt> otherwise.
     */
    private LimeXMLDocument constructDocument(File file) {
	    if(LimeXMLUtils.isSupportedFormatForSchema(file, schemaURI)) {
            try {
                // Documents with multiple file formats may be the wrong type.
                LimeXMLDocument document = MetaDataReader.readDocument(file);
                if(document.getSchemaURI().equals(schemaURI))
                    return document;
            } catch (IOException ignored) {
                LOG.warn("Error creating document", ignored);
            }
        }
        
        return null;
    }

    /**
     * Gets a list of keywords from all the documents in this collection.
     * <p>
     * delegates to the individual documents and collates the list
     */
    protected List getKeyWords(){
        List retList = new ArrayList();
        Iterator docs;
        synchronized(mainMap){
            docs = mainMap.values().iterator();
            while(docs.hasNext()){
                LimeXMLDocument d = (LimeXMLDocument)docs.next();
                retList.addAll(d.getKeyWords());
            }
        }
        return retList;
    }

    /**
     * Gets a list of indivisible keywords from all the documents in this 
     * collection.
     * <p>
     * Delegates to the individual documents and collates the list
     */
    protected List getKeyWordsIndivisible(){
        List retList = new ArrayList();
        Iterator docs;
        synchronized(mainMap){
            docs = mainMap.values().iterator();
            while(docs.hasNext()){
                LimeXMLDocument d = (LimeXMLDocument)docs.next();
                retList.addAll(d.getKeyWordsIndivisible());
            }
        }
        return retList;
    }
    
    /**
     * Returns the schema URI of this collection.
     */
    public String getSchemaURI(){
        return schemaURI;
    }
    
    /**
     * Adds the keywords of this LimeXMLDocument into the correct Trie 
     * for the field of the value.
     */
    private void addKeywords(LimeXMLDocument doc) {
        synchronized(mainMap) {
            for(Iterator i = doc.getNameValueSet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                final String name = (String)entry.getKey();
                final String value = 
                    I18NConvert.instance().getNorm((String)entry.getValue());
                Trie trie = (Trie)trieMap.get(name);
                // if no lookup table created yet, create one & insert.
                if(trie == null) {
                    trie = new Trie(true); //ignore case.
                    trieMap.put(name, trie);
                }
                List allDocs = (List)trie.get(value);
                // if no list of docs for this value created, create & insert.
                if( allDocs == null ) {
                    allDocs = new LinkedList();
                    trie.add(value, allDocs);
                }
                //Add the value to the list of docs
                allDocs.add(doc);
            }
        }
    }
    
    /**
     * Removes the keywords of this LimeXMLDocument from the appropriate Trie.
     * If the list is emptied, it is removed from the Trie.
     */
    private void removeKeywords(LimeXMLDocument doc) {
        synchronized(mainMap) {
            for(Iterator i = doc.getNameValueSet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                final String name = (String)entry.getKey();
                
                Trie trie = (Trie)trieMap.get(name);
                // if no trie, ignore.
                if(trie == null)
                    continue;
                    
                final String value = 
                    I18NConvert.instance().getNorm((String)entry.getValue());
                List allDocs = (List)trie.get(value);
                // if no list, ignore.
                if( allDocs == null )
                    continue;
                allDocs.remove(doc);
                // if we emptied the doc, remove from trie...
                if( allDocs.size() == 0 )
                    trie.remove(value);
            }
        }
    }

    /**
     * Adds a reply into the mainMap of this collection.
     * Also adds this LimeXMLDocument to the list of documents the
     * FileDesc knows about.
     */
    public void addReply(FileDesc fd, LimeXMLDocument replyDoc) {
        URN hash = fd.getSHA1Urn();
        synchronized(mainMap){
            dirty = true;
            mainMap.put(hash,replyDoc);
            addKeywords(replyDoc);
        }
        
        fd.addLimeXMLDocument(replyDoc);
    }

    /**
     * Returns the amount of items in this collection.
     */
    public int getCount(){
        synchronized(mainMap) {
            return mainMap.size();
        }
    }
    
    /**
     * Returns the LimeXMLDocument associated with this hash.
     * May return null if the hash is not found.
     */
    public LimeXMLDocument getDocForHash(URN hash){
        synchronized(mainMap){
            return (LimeXMLDocument)mainMap.get(hash);
        }
    }
        
    /**
     * Returns all documents that match the particular query.
     * If no documents match, this returns an empty list.
     *
     * This goes through the following methodology:
     * 1) Looks in the index trie to determine if ANY
     *    of the values in the query's document match.
     *    If they do, adds the document to a set of
     *    possible matches.  A set is used so the same
     *    document is not added multiple times.
     * 2) If no documents matched, returns an empty list.
     * 3) Iterates through the possible matching documents
     *    and does a fine-grained matchup, using XML-specific
     *    matching techniques.
     * 4) Returns an empty list if nothing matched or
     *    a list of the matching documents.
     */    
    List getMatchingReplies(LimeXMLDocument query) {
        // First get a list of anything that could possibly match.
        // This uses a set so we don't add the same doc twice ...
        Set matching = null;
        synchronized(mainMap) {
            for(Iterator i = query.getNameValueSet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();

                // Get the name of the particular field being queried for.
                final String name = (String)entry.getKey();
                // Lookup the matching Trie for that field.
                Trie trie = (Trie)trieMap.get(name);
                // No matching trie?.. Ignore.
                if(trie == null)
                    continue;

                // Get the value of that field being queried for.    
                final String value = (String)entry.getValue();
                // Get our shared XML docs that match this value.
                // This query is from the network, and is therefore already
                // normalized -- SHOULD NOT NORMALIZE AGAIN!!
                Iterator /* of List */ iter = trie.getPrefixedBy(value);
                // If some matches and 'matching' not allocated yet,
                // allocate a new Set for storing matches
                if(iter.hasNext()) {
                    if (matching == null)
                        matching = new HashSet();
                    // Iterate through each set of matches the Trie found
                    // and add those matching-lists to our set of matches.
                    // Note that the trie.getPrefixedBy returned
                    // an Iterator of Lists -- this is because the Trie
                    // does prefix matching, so there are many Lists of XML
                    // docs that could match.
                    while(iter.hasNext()) {
                        List matchesVal = (List)iter.next();
                        matching.addAll(matchesVal);
                    }
                }
            }
        }
        
        // no matches?... exit.
        if( matching == null || matching.size() == 0)
            return Collections.EMPTY_LIST;
        
        // Now filter that list using the real XML matching tool...
        List actualMatches = null;
        for(Iterator i = matching.iterator(); i.hasNext(); ) {
            LimeXMLDocument currReplyDoc = (LimeXMLDocument)i.next();
            if (LimeXMLUtils.match(currReplyDoc, query, false)) {
                if( actualMatches == null ) // delayed allocation of the list..
                    actualMatches = new LinkedList();
                actualMatches.add(currReplyDoc);
            }
        }
        
        // No actual matches?... exit.
        if( actualMatches == null || actualMatches.size() == 0 )
            return Collections.EMPTY_LIST;

        return actualMatches;
    }
    
    /**
     * Replaces the document in the map with a newer LimeXMLDocument.
     * @return the older document, which is being replaced. Can be null.
     */
    public LimeXMLDocument replaceDoc(FileDesc fd, LimeXMLDocument newDoc) {
        if(LOG.isTraceEnabled())
            LOG.trace("Replacing doc in FD (" + fd + ") with new doc (" + newDoc + ")");
        
        LimeXMLDocument oldDoc = null;
        URN hash = fd.getSHA1Urn();
        synchronized(mainMap) {
            dirty = true;
            oldDoc = (LimeXMLDocument)mainMap.put(hash,newDoc);
            if(oldDoc == null) 
                Assert.that(false, "attempted to replace doc that did not exist!!");
            removeKeywords(oldDoc);
            addKeywords(newDoc);
        }
       
        fd.replaceLimeXMLDocument(oldDoc, newDoc);
        return oldDoc;
    }

    /**
     * Removes the document associated with this FileDesc
     * from this collection, as well as removing it from
     * the FileDesc.
     */
    public boolean removeDoc(FileDesc fd) {
        LimeXMLDocument val;
        synchronized(mainMap) {
            val = (LimeXMLDocument)mainMap.remove(fd.getSHA1Urn());
            if(val != null)
                dirty = true;
        }
        
        if(val != null) {
            fd.removeLimeXMLDocument((LimeXMLDocument)val);
            removeKeywords(val);
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("removed: " + val);
        
        return val != null;
    }
    
    /**
     * Writes this media file to disk, using the XML in the doc.
     */
    public int mediaFileToDisk(FileDesc fd, String fileName, LimeXMLDocument doc,  boolean checkBetter) {
        int writeState = -1;
        
        if(LOG.isDebugEnabled())
            LOG.debug("writing: " + fileName + " to disk.");
        
        // see if you need to change a hash for a file due to a write...
        // if so, we need to commit the ID3 data to disk....
        MetaDataEditor commitWith = getEditorIfNeeded(fileName, doc, checkBetter);
        if (commitWith != null)  {
        	if(commitWith.getCorrectDocument() == null) {
        		writeState = commitMetaData(fileName, commitWith);
        	} else { 
        		//The data on disk is better than the data we got in the
        		//query reply. So we should update the Document we added
        		removeDoc(fd);
        		addReply(fd, commitWith.getCorrectDocument());
        		writeState = NORMAL;//no need to write anything
        	}
        }
        
        Assert.that(writeState != INCORRECT_FILETYPE, "trying to write data to unwritable file");

        return writeState;
    }

    /**
     * Determines whether or not this LimeXMLDocument can or should be
     * commited to disk to replace the ID3 tags in the mp3File.
     * If the ID3 tags in the file are the same as those in document,
     * this returns null (indicating no changes required).
     * @return An ID3Editor to use when committing or null if nothing 
     *  should be editted.
     */
    private MetaDataEditor getEditorIfNeeded(String mp3File, LimeXMLDocument doc, 
                                                        boolean checkBetter) {
        
        MetaDataEditor newValues = MetaDataEditor.getEditorForFile(mp3File);
        //if this call returned null, we should store the data in our
        //xml repository only.
        if (newValues == null)
        	return null;
        newValues.populate(doc);
        
        // Now see if the file already has the same info ...
        MetaDataEditor existing = MetaDataEditor.getEditorForFile(mp3File);
        LimeXMLDocument existingDoc = null;
        try {
            existingDoc = MetaDataReader.readDocument(new File(mp3File));
        } catch(IOException e) {
            return null;
        }
        existing.populate(existingDoc);
        
        //We are supposed to pick and chose the better set of tags
        if( newValues.equals(existing) ) {
            LOG.debug("tag read from disk is same as XML doc.");
            return null;
        } else if(checkBetter) {
            if(existing.betterThan(newValues)) {
                LOG.debug("Data on disk is better, using disk data.");
                //Note: In this case we are going to discard the LimeXMLDocument we
                //got off the network, because the data on the file is better than
                //the data in the query reply. Only in this case, we set the
                //"correctDocument variable of the ID3Editor.
                existing.setCorrectDocument(existingDoc);
                return existing;
            } else {
                LOG.debug("Retrieving better fields from disk.");
                newValues.pickBetterFields(existing);        
            }
        }
            
        // Commit using this Meta data editor ... 
        return newValues;
    }


    /**
     * Commits the changes to disk.
     * If anything was changed on disk, notifies the FileManager of a change.
     */
    private int commitMetaData(String fileName, MetaDataEditor editor) {
        //write to mp3 file...
        int retVal = editor.commitMetaData(fileName);
        if(LOG.isDebugEnabled())
            LOG.debug("wrote data: " + retVal);
        // any error where the file wasn't changed ... 
        if( retVal == FILE_DEFECTIVE ||
            retVal == RW_ERROR ||
            retVal == BAD_ID3 ||
            retVal == INCORRECT_FILETYPE)
            return retVal;
            
        // We do not remove the hash from the hashMap because
        // MetaFileManager needs to look it up to get the doc.
        
        //Since the hash of the file has changed, the metadata pertaining 
        //to other schemas will be lost unless we update those tables
        //with the new hashValue. 
        //NOTE:This is the only time the hash will change-(mp3 and audio)
        RouterService.getFileManager().fileChanged(new File(fileName));
        return retVal;
    }
    
    /** Serializes the current map to disk. */
    public boolean writeMapToDisk() {
        boolean wrote = false;
        synchronized(mainMap) {
            if(!dirty)
                return true;
                
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));
                out.writeObject(mainMap);
                out.flush();
                wrote = true;
            } catch(Throwable ignored) {
                LOG.trace("Unable to write", ignored);
            } finally {
                IOUtils.close(out);
            }
            
            dirty = false;
        }
        
        return wrote;
    }
    
    /** Reads the map off of the disk. */
    private Map readMapFromDisk() {
        ObjectInputStream in = null;
        Map read = null;
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
            read = (Map)in.readObject();
        } catch(Throwable t) {
            LOG.error("Unable to read LimeXMLCollection", t);
        } finally {
            IOUtils.close(in);
        }
        
        return read == null ? new HashMap() : read;
    }
}
