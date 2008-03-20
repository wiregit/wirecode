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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IdentityHashSet;
import org.limewire.collection.StringTrie;
import org.limewire.io.IOUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.GenericsUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.NameValue;
import org.xml.sax.SAXException;

import com.google.inject.Provider;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.metadata.MetaDataWriter;
import com.limegroup.gnutella.metadata.MetaReader;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;

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
    private final Map<URN, LimeXMLDocument> mainMap;
    
    /**
     * The old map that was read off disk.
     *
     * Used while initially processing FileDescs to add.
     */
    private final Map<URN, LimeXMLDocument> oldMap;
    
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
    private final Map<String, StringTrie<List<LimeXMLDocument>>> trieMap;
    
    /**
     * Whether or not data became dirty after we last wrote to disk.
     */
    private boolean dirty = false;

    public static enum MetaDataState {
        UNCHANGED,
        NORMAL,
        FILE_DEFECTIVE,
        RW_ERROR,
        BAD_ID3,
        FAILED_TITLE,
        FAILED_ARTIST,
        FAILED_ALBUM,
        FAILED_YEAR,
        FAILED_COMMENT,
        FAILED_TRACK,
        FAILED_GENRE,
        HASH_FAILED,
        INCORRECT_FILETYPE;
    }

    private final Provider<FileManager> fileManager;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    private final MetaDataFactory metaDataFactory;

    private final MetaDataReader metaDataReader;
    
    private final File savedDocsDir;

    /**
     * Creates a new LimeXMLReplyCollection.  The reply collection
     * will retain only those XMLDocs that match the given schema URI.
     *
     * @param fds The list of shared FileDescs.
     * @param URI This collection's schema URI
     * @param fileManager 
     */
    LimeXMLReplyCollection(String URI, File path, Provider<FileManager> fileManager,
            LimeXMLDocumentFactory limeXMLDocumentFactory, MetaDataReader metaDataReader,
            MetaDataFactory metaDataFactory) {
        this.schemaURI = URI;
        this.fileManager = fileManager;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.metaDataReader = metaDataReader;
        this.metaDataFactory = metaDataFactory;
        this.trieMap = new HashMap<String, StringTrie<List<LimeXMLDocument>>>();
        this.mainMap = new HashMap<URN, LimeXMLDocument>();
        this.savedDocsDir = path;
        this.oldMap = readMapFromDisk();
    }
    
    /**
     * Initializes the map using either LimeXMLDocuments in the list of potential
     * documents, or elements stored in oldMap.  Items in potential take priority.
     */
    public LimeXMLDocument initialize(FileDesc fd, List<? extends LimeXMLDocument> potential) {
        URN urn = fd.getSHA1Urn();
        LimeXMLDocument doc = null;
        
        // First try to get a doc from the potential list.
        for(LimeXMLDocument next : potential) {
            if(next.getSchemaURI().equals(schemaURI)) {
                doc = next;
                break;
            }
        }
        
        // Then try to get it from the old map.
        if(doc == null)
            doc = oldMap.get(urn);
        
        
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
    public LimeXMLDocument createIfNecessary(FileDesc fd) {
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
            if (tempDoc != null) {
                doc = update(doc, tempDoc);
            } else {
                doc.setCurrent();
            }
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
            doc = AudioMetaData.fixCorruption(doc, limeXMLDocumentFactory);
            mediaFileToDisk(fd, file.getPath(), doc);
        }
        
        return doc;
    }
    
    /**
     * Updates an existing old document to be a newer document, but retains all fields
     * that may have been in the old one that are not in the newer (for the case of
     * existing annotations).
     */
    private LimeXMLDocument update(LimeXMLDocument older, LimeXMLDocument newer) {
        Map<String, String> fields = new HashMap<String, String>();
        for(Map.Entry<String, String> next : newer.getNameValueSet()) {
            fields.put(next.getKey(), next.getValue());
        }
        
        for(Map.Entry<String, String> next : older.getNameValueSet()) {
            if(!fields.containsKey(next.getKey()))
                fields.put(next.getKey(), next.getValue());
        }

        List<NameValue<String>> nameValues = new ArrayList<NameValue<String>>(fields.size());
        for(Map.Entry<String, String> next : fields.entrySet())
            nameValues.add(new NameValue<String>(next.getKey(), next.getValue()));
        
        return limeXMLDocumentFactory.createLimeXMLDocument(nameValues, newer.getSchemaURI());
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
                LimeXMLDocument document = metaDataReader.readDocument(file);
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
    protected List<String> getKeyWords(){
        List<String> retList = new ArrayList<String>();
        synchronized(mainMap){
            for(LimeXMLDocument d : mainMap.values()) {
                if( !isLWSDoc(d))
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
    protected List<String> getKeyWordsIndivisible(){
        List<String> retList = new ArrayList<String>();
        synchronized(mainMap){
            for(LimeXMLDocument d : mainMap.values()) {
                if( !isLWSDoc(d))
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
            for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
                final String name = entry.getKey();
                final String value = I18NConvert.instance().getNorm(entry.getValue());
                StringTrie<List<LimeXMLDocument>> trie = trieMap.get(name);
                // if no lookup table created yet, create one & insert.
                if(trie == null) {
                    trie = new StringTrie<List<LimeXMLDocument>>(true); //ignore case.
                    trieMap.put(name, trie);
                }
                List<LimeXMLDocument> allDocs = trie.get(value);
                // if no list of docs for this value created, create & insert.
                if( allDocs == null ) {
                    allDocs = new LinkedList<LimeXMLDocument>();
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
            for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
                final String name = entry.getKey();
                StringTrie<List<LimeXMLDocument>> trie = trieMap.get(name);
                // if no trie, ignore.
                if(trie == null)
                    continue;
                    
                final String value = I18NConvert.instance().getNorm(entry.getValue());
                List<LimeXMLDocument> allDocs = trie.get(value);
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
    	assert getSchemaURI().equals(replyDoc.getSchemaURI());

        URN hash = fd.getSHA1Urn();
        synchronized(mainMap){
            dirty = true;
            mainMap.put(hash,replyDoc);
            if(!isLWSDoc(replyDoc))
            	addKeywords(replyDoc);
        }
        
        fd.addLimeXMLDocument(replyDoc);
    }
    
    /**
     * Determines if the XMLDocument is from the LWS
     * @return true if this document contains a LWS license, false otherwise
     */
    public boolean isLWSDoc(LimeXMLDocument doc) {
    	if( doc != null && doc.getLicenseString() != null && doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.toString()))
    		return true;
    	return false;
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
            return mainMap.get(hash);
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
    List<LimeXMLDocument> getMatchingReplies(LimeXMLDocument query) {
        // First get a list of anything that could possibly match.
        // This uses a set so we don't add the same doc twice ...
        Set<LimeXMLDocument> matching = null;
        synchronized(mainMap) {
            
            for(Map.Entry<String, String> entry : query.getNameValueSet()) {
                // Get the name of the particular field being queried for.
                final String name = entry.getKey();
                // Lookup the matching Trie for that field.
                StringTrie<List<LimeXMLDocument>> trie = trieMap.get(name);
                // No matching trie?.. Ignore.
                if(trie == null)
                    continue;

                // Get the value of that field being queried for.    
                final String value = entry.getValue();
                // Get our shared XML docs that match this value.
                // This query is from the network, and is therefore already
                // normalized -- SHOULD NOT NORMALIZE AGAIN!!
                Iterator<List<LimeXMLDocument>> iter = trie.getPrefixedBy(value);
                // If some matches and 'matching' not allocated yet,
                // allocate a new Set for storing matches
                if(iter.hasNext()) {
                    if (matching == null)
                        matching = new IdentityHashSet<LimeXMLDocument>();
                    // Iterate through each set of matches the Trie found
                    // and add those matching-lists to our set of matches.
                    // Note that the trie.getPrefixedBy returned
                    // an Iterator of Lists -- this is because the Trie
                    // does prefix matching, so there are many Lists of XML
                    // docs that could match.
                    while(iter.hasNext())
                        matching.addAll(iter.next());
                }
            }
        }
        
        // no matches?... exit.
        if( matching == null || matching.size() == 0)
            return Collections.emptyList();
        
        // Now filter that list using the real XML matching tool...
        List<LimeXMLDocument> actualMatches = null;
        for(LimeXMLDocument currReplyDoc : matching) {
            if (LimeXMLUtils.match(currReplyDoc, query, false)) {
                if( actualMatches == null ) // delayed allocation of the list..
                    actualMatches = new LinkedList<LimeXMLDocument>();
                actualMatches.add(currReplyDoc);
            }
        }
        
        // No actual matches?... exit.
        if( actualMatches == null || actualMatches.size() == 0 )
            return Collections.emptyList();

        return actualMatches;
    }
    
    /**
     * Replaces the document in the map with a newer LimeXMLDocument.
     * @return the older document, which is being replaced. Can be null.
     */
    public LimeXMLDocument replaceDoc(FileDesc fd, LimeXMLDocument newDoc) {
    	assert getSchemaURI().equals(newDoc.getSchemaURI());
    	
        if(LOG.isTraceEnabled())
            LOG.trace("Replacing doc in FD (" + fd + ") with new doc (" + newDoc + ")");
        
        LimeXMLDocument oldDoc = null;
        URN hash = fd.getSHA1Urn();
        synchronized(mainMap) {
            dirty = true;
            oldDoc = mainMap.put(hash,newDoc);
            assert oldDoc != null : "attempted to replace doc that did not exist!!";
            removeKeywords(oldDoc);
	        if(!isLWSDoc(newDoc))
            	addKeywords(newDoc);
        }
       
        boolean replaced = fd.replaceLimeXMLDocument(oldDoc, newDoc);
        assert replaced;
        
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
            val = mainMap.remove(fd.getSHA1Urn());
            if(val != null)
                dirty = true;
        }
        
        if(val != null) {
            fd.removeLimeXMLDocument(val);
            removeKeywords(val);
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("removed: " + val);
        
        return val != null;
    }
    
    /**
     * Writes this media file to disk, using the XML in the doc.
     */
    public MetaDataState mediaFileToDisk(FileDesc fd, String fileName, LimeXMLDocument doc) {
        MetaDataState writeState = MetaDataState.UNCHANGED;
        
        if(LOG.isDebugEnabled())
            LOG.debug("writing: " + fileName + " to disk.");
        
        // see if you need to change a hash for a file due to a write...
        // if so, we need to commit the metadata to disk....
        MetaDataWriter writer = getEditorIfNeeded(fileName, doc);
        if (writer != null)  {
            writeState = commitMetaData(fileName, writer);
        }
        assert writeState != MetaDataState.INCORRECT_FILETYPE : "trying to write data to unwritable file";

        return writeState;
    }

    /**
     * Determines whether or not this LimeXMLDocument can or should be
     * commited to disk to replace the ID3 tags in the audioFile.
     * If the ID3 tags in the file are the same as those in document,
     * this returns null (indicating no changes required).
     * @return An Editor to use when committing or null if nothing 
     *  should be editted.
     */
    private MetaDataWriter getEditorIfNeeded(String fileName, LimeXMLDocument doc) {
        // check if an editor exists for this file, if no editor exists
        //  just store data in xml repository only
        if( !LimeXMLUtils.isSupportedEditableFormat(fileName)) 
        	return null;
        
        //get the editor for this file and populate it with the XML doc info
        MetaDataWriter newValues = new MetaDataWriter(fileName, metaDataFactory);
        newValues.populate(doc);
        
        
        // try reading the file off of disk
        MetaReader existing = null;
        try {
            existing = metaDataFactory.parse(new File(fileName));
        } catch (IOException e) {
            return null;
        }
        
        //We are supposed to pick and chose the better set of tags
        if(!newValues.needsToUpdate(existing.getMetaData())) {
            LOG.debug("tag read from disk is same as XML doc.");
            return null;
        }
            
        // Commit using this Meta data editor ... 
        return newValues;
    }


    /**
     * Commits the changes to disk.
     * If anything was changed on disk, notifies the FileManager of a change.
     */
    private MetaDataState commitMetaData(String fileName, MetaDataWriter editor) {
        //write to mp3 file...
        MetaDataState retVal = editor.commitMetaData();
        if(LOG.isDebugEnabled())
            LOG.debug("wrote data: " + retVal);
        // any error where the file wasn't changed ... 
        if( retVal == MetaDataState.FILE_DEFECTIVE ||
            retVal == MetaDataState.RW_ERROR ||
            retVal == MetaDataState.BAD_ID3 ||
            retVal == MetaDataState.INCORRECT_FILETYPE)
            return retVal;
            
        // We do not remove the hash from the hashMap because
        // MetaFileManager needs to look it up to get the doc.
        
        //Since the hash of the file has changed, the metadata pertaining 
        //to other schemas will be lost unless we update those tables
        //with the new hashValue. 
        //NOTE:This is the only time the hash will change-(mp3 and audio)
        fileManager.get().fileChanged(new File(fileName));
        return retVal;
    }
    
    /** Serializes the current map to disk. */
    public boolean writeMapToDisk() {
        boolean wrote = false;
        Map<URN, String> xmlMap;
        synchronized(mainMap) {
            if(!dirty) {
                LOG.debug("Not writing because not dirty.");
                return true;
            }
            
            xmlMap = new HashMap<URN, String>(mainMap.size());
            for(Map.Entry<URN, LimeXMLDocument> entry : mainMap.entrySet())
                xmlMap.put(entry.getKey(), entry.getValue().getXmlWithVersion());
            
            dirty = false;
        }

        File dataFile = new File(savedDocsDir, LimeXMLSchema.getDisplayString(schemaURI)+ ".sxml2");
        File parent = dataFile.getParentFile();
        if(parent != null)
            parent.mkdirs();
                
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));
            out.writeObject(xmlMap);
            out.flush();
            wrote = true;
        } catch(IOException ignored) {
            LOG.trace("Unable to write", ignored);
        } finally {
            IOUtils.close(out);
        }
        
        return wrote;
    }
    
    /** Reads the map off of the disk. */
    private Map<URN, LimeXMLDocument> readMapFromDisk() {
        File newFile = new File(savedDocsDir, LimeXMLSchema.getDisplayString(schemaURI)+ ".sxml2");
        Map<URN, LimeXMLDocument> map = null;
        if(newFile.exists()) {
            map = readNewFile(newFile);
        } else {
            File oldFile = new File(savedDocsDir, LimeXMLSchema.getDisplayString(schemaURI)+ ".sxml");
            if(oldFile.exists()) {
                map = readOldFile(oldFile);
                oldFile.delete();
            }
        }
        
        return map == null ? new HashMap<URN, LimeXMLDocument>() : map;
    }
    
    /** Reads a file in the new format off disk. */
    private Map<URN, LimeXMLDocument> readNewFile(File input) {
        if(LOG.isDebugEnabled())
            LOG.debug("Reading new format from file: " + input);
        
        ObjectInputStream in = null;
        Map<URN, String> read = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input)));
            read = GenericsUtils.scanForMap(in.readObject(), URN.class, String.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable t) {
            LOG.error("Unable to read LimeXMLCollection", t);
        } finally {
            IOUtils.close(in);
        }
        
        if(read == null)
            read = Collections.emptyMap();
        
        Map<URN, LimeXMLDocument> docMap = new HashMap<URN, LimeXMLDocument>(read.size());
        for(Map.Entry<URN, String> entry : read.entrySet()) {
            try {
                docMap.put(entry.getKey(), limeXMLDocumentFactory.createLimeXMLDocument(entry.getValue()));
            } catch(IOException ignored) {
                LOG.warn("Error creating document for: " + entry.getValue(), ignored);
            } catch(SchemaNotFoundException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            } catch (SAXException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            }
        }
        
        return docMap;
    }
    
    /** Reads a file in the old format off disk. */
    private Map<URN, LimeXMLDocument> readOldFile(File input) {
        if(LOG.isDebugEnabled())
            LOG.debug("Reading old format from file: " + input);
        ConverterObjectInputStream in = null;
        Map<URN, SerialXml> read = null;
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(input)));
            in.addLookup("com.limegroup.gnutella.xml.LimeXMLDocument", SerialXml.class.getName());
            read = GenericsUtils.scanForMap(in.readObject(), URN.class, SerialXml.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable t) {
            LOG.error("Unable to read LimeXMLCollection", t);
        } finally {
            IOUtils.close(in);
        }
        
        if(read == null)
            read = Collections.emptyMap();
        
        Map<URN, LimeXMLDocument> docMap = new HashMap<URN, LimeXMLDocument>(read.size());
        for(Map.Entry<URN, SerialXml> entry : read.entrySet()) {
            try {
                docMap.put(entry.getKey(), limeXMLDocumentFactory.createLimeXMLDocument(entry.getValue().getXml(true)));
            } catch(IOException ignored) {
                LOG.warn("Error creating document for: " + entry.getValue(), ignored);
            } catch(SchemaNotFoundException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            } catch (SAXException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            }
        }
        
        return docMap;
    }
}
