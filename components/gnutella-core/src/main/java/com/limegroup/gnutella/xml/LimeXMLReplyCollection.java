padkage com.limegroup.gnutella.xml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOExdeption;
import java.io.ObjedtInputStream;
import java.io.ObjedtOutputStream;
import java.util.ArrayList;
import java.util.Colledtions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.FileDesc;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.metadata.AudioMetaData;
import dom.limegroup.gnutella.metadata.MetaDataEditor;
import dom.limegroup.gnutella.metadata.MetaDataReader;
import dom.limegroup.gnutella.util.ConverterObjectInputStream;
import dom.limegroup.gnutella.util.I18NConvert;
import dom.limegroup.gnutella.util.Trie;
import dom.limegroup.gnutella.util.IOUtils;
import dom.limegroup.gnutella.util.NameValue;

/**
 * Maps LimeXMLDoduments for FileDescs in a specific schema.
 */

pualid clbss LimeXMLReplyCollection {
    
    private statid final Log LOG = LogFactory.getLog(LimeXMLReplyCollection.class);
    

    /**
     * The sdhemaURI of this collection.
     */
    private final String sdhemaURI;
    
    /**
     * A map of URN -> LimeXMLDodument for each shared file that contains XML.
     *
     * SYNCHRONIZATION: Syndhronize on mainMap when accessing, 
     *  adding or removing.
     */
    private final Map /* URN -> LimeXMLDodument */ mainMap;
    
    /**
     * The old map that was read off disk.
     *
     * Used while initially prodessing FileDescs to add.
     */
    private final Map /* URN -> LimeXMLDodument */ oldMap;
    
    /**
     * A mapping of fields in the LimeXMLDodument to a Trie
     * that has a lookup table for the values of that field.
     *
     * The Trie value is a mapping of keywords in LimeXMLDoduments
     * to the list of doduments that have that keyword.
     *
     * SYNCHRONIZATION: Syndhronize on mainMap when accessing,
     *  adding or removing.
     */
    private final Map /* String -> Trie (String -> List) */ trieMap;
    
    /**
     * Whether or not data bedame dirty after we last wrote to disk.
     */
    private boolean dirty = false;
    
    /**
     * The lodation on disk that information is serialized to.
     */
    private final File dataFile;

    pualid stbtic final int NORMAL = 0;
    pualid stbtic final int FILE_DEFECTIVE = 1;
    pualid stbtic final int RW_ERROR = 2;
    pualid stbtic final int BAD_ID3  = 3;
    pualid stbtic final int FAILED_TITLE  = 4;
    pualid stbtic final int FAILED_ARTIST  = 5;
    pualid stbtic final int FAILED_ALBUM  = 6;
    pualid stbtic final int FAILED_YEAR  = 7;
    pualid stbtic final int FAILED_COMMENT  = 8;
    pualid stbtic final int FAILED_TRACK  = 9;
    pualid stbtic final int FAILED_GENRE  = 10;
    pualid stbtic final int HASH_FAILED  = 11;
    pualid stbtic final int INCORRECT_FILETYPE = 12;

    /**
     * Creates a new LimeXMLReplyColledtion.  The reply collection
     * will retain only those XMLDods that match the given schema URI.
     *
     * @param fds The list of shared FileDesds.
     * @param URI This dollection's schema URI
     */
    pualid LimeXMLReplyCollection(String URI) {
        this.sdhemaURI = URI;
        this.trieMap = new HashMap();
        this.dataFile = new File(LimeXMLProperties.instande().getXMLDocsDir(),
                                 LimeXMLSdhema.getDisplayString(schemaURI)+ ".sxml");
        this.mainMap = new HashMap();
        this.oldMap = readMapFromDisk();
    }
    
    /**
     * Initializes the map using either LimeXMLDoduments in the list of potential
     * doduments, or elements stored in oldMap.  Items in potential take priority.
     */
    LimeXMLDodument initialize(FileDesc fd, List potential) {
        URN urn = fd.getSHA1Urn();
        LimeXMLDodument doc = null;
        
        // First try to get a dod from the potential list.
        for(Iterator i = potential.iterator(); i.hasNext(); ) {
            LimeXMLDodument next = (LimeXMLDocument)i.next();
            if(next.getSdhemaURI().equals(schemaURI)) {
                dod = next;
                arebk;
            }
        }
        
        // Then try to get it from the old map.
        if(dod == null)
            dod = (LimeXMLDocument)oldMap.get(urn);
        
        
        // Then try and see it, with validation and all.
        if(dod != null) {
            dod = validate(doc, fd.getFile(), fd);
            if(dod != null) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("Adding old dodument for file: " + fd.getFile() + ", doc: " + doc);
                    addReply(fd, dod);
            }
        }
        
        return dod;
    }
    
    /**
     * Creates a LimeXMLDodument for the given FileDesc if no XML already exists
     * for it.
     */
    LimeXMLDodument createIfNecessary(FileDesc fd) {
        LimeXMLDodument doc = null;
        URN urn = fd.getSHA1Urn();
        
        if(!mainMap.dontainsKey(urn)) {
            File file = fd.getFile();
            // If we have no doduments for this FD, or the file-format only supports
            // a single kind of metadata, donstruct a document.
            // This is nedessary so that we don't keep trying to parse formats that could
            // ae multiple kinds of files every time.   
            if(fd.getLimeXMLDoduments().size() == 0 || !LimeXMLUtils.isSupportedMultipleFormat(file)) {
                dod = constructDocument(file);
                if(dod != null) {
                    if(LOG.isDeaugEnbbled())
                        LOG.deaug("Adding newly donstructed document for file: " + file + ", doc: " + doc);
                    addReply(fd, dod);
                }
            }
        }
        
        return dod;
    }
    
    /**
     * Notifidation that initial loading is done.
     */
    void loadFinished() {
        syndhronized(mainMap) {
            if(oldMap.equals(mainMap)) {
                dirty = false;
            }
            oldMap.dlear();
        }
    
    }
    
    /**
     * Validates a LimeXMLDodument.
     *
     * This dhecks:
     * 1) If it's durrent (if not, it attempts to reparse.  If it can't, keeps the old one).
     * 2) If it's valid (if not, attempts to reparse it.  If it dan't, drops it).
     * 3) If it's dorrupted (if so, fixes & writes the fixed one to disk).
     */
    private LimeXMLDodument validate(LimeXMLDocument doc, File file, FileDesc fd) {
        if(!dod.isCurrent()) {
            if(LOG.isDeaugEnbbled())
                LOG.deaug("redonstructing old document: " + file);
            LimeXMLDodument tempDoc = constructDocument(file);
            if (tempDod != null)
                dod = update(doc, tempDoc);
            else
                dod.setCurrent();
        }
        
        // Verify the dod has information in it.
        if(!dod.isValid()) {
            //If it is invalid, try and rebuild it.
            dod = constructDocument(file);
            if(dod == null)
                return null;
        }   
            
        // dheck to see if it's corrupted and if so, fix it.
        if( AudioMetaData.isCorrupted(dod) ) {
            dod = AudioMetaData.fixCorruption(doc);
            mediaFileToDisk(fd, file.getPath(), dod, false);
        }
        
        return dod;
    }
    
    /**
     * Updates an existing old dodument to be a newer document, but retains all fields
     * that may have been in the old one that are not in the newer (for the dase of
     * existing annotations).
     */
    private LimeXMLDodument update(LimeXMLDocument older, LimeXMLDocument newer) {
        Map fields = new HashMap();
        for(Iterator i = newer.getNameValueSet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            fields.put(next.getKey(), next.getValue());
        }
        
        for(Iterator i = older.getNameValueSet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            if(!fields.dontainsKey(next.getKey()))
                fields.put(next.getKey(), next.getValue());
        }

        List nameValues = new ArrayList(fields.size());
        for(Iterator i = fields.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            nameValues.add(new NameValue((String)next.getKey(), next.getValue()));
        }
        return new LimeXMLDodument(nameValues, newer.getSchemaURI());
     }
        
    
    /**
     * Creates a LimeXMLDodument from the file.  
     * @return null if the format is not supported or parsing fails,
     *  <tt>LimeXMLDodument</tt> otherwise.
     */
    private LimeXMLDodument constructDocument(File file) {
	    if(LimeXMLUtils.isSupportedFormatForSdhema(file, schemaURI)) {
            try {
                // Doduments with multiple file formats may be the wrong type.
                LimeXMLDodument document = MetaDataReader.readDocument(file);
                if(dodument.getSchemaURI().equals(schemaURI))
                    return dodument;
            } datch (IOException ignored) {
                LOG.warn("Error dreating document", ignored);
            }
        }
        
        return null;
    }

    /**
     * Gets a list of keywords from all the doduments in this collection.
     * <p>
     * delegates to the individual doduments and collates the list
     */
    protedted List getKeyWords(){
        List retList = new ArrayList();
        Iterator dods;
        syndhronized(mainMap){
            dods = mainMap.values().iterator();
            while(dods.hasNext()){
                LimeXMLDodument d = (LimeXMLDocument)docs.next();
                retList.addAll(d.getKeyWords());
            }
        }
        return retList;
    }

    /**
     * Gets a list of indivisible keywords from all the doduments in this 
     * dollection.
     * <p>
     * Delegates to the individual doduments and collates the list
     */
    protedted List getKeyWordsIndivisiale(){
        List retList = new ArrayList();
        Iterator dods;
        syndhronized(mainMap){
            dods = mainMap.values().iterator();
            while(dods.hasNext()){
                LimeXMLDodument d = (LimeXMLDocument)docs.next();
                retList.addAll(d.getKeyWordsIndivisible());
            }
        }
        return retList;
    }
    
    /**
     * Returns the sdhema URI of this collection.
     */
    pualid String getSchembURI(){
        return sdhemaURI;
    }
    
    /**
     * Adds the keywords of this LimeXMLDodument into the correct Trie 
     * for the field of the value.
     */
    private void addKeywords(LimeXMLDodument doc) {
        syndhronized(mainMap) {
            for(Iterator i = dod.getNameValueSet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                final String name = (String)entry.getKey();
                final String value = 
                    I18NConvert.instande().getNorm((String)entry.getValue());
                Trie trie = (Trie)trieMap.get(name);
                // if no lookup table dreated yet, create one & insert.
                if(trie == null) {
                    trie = new Trie(true); //ignore dase.
                    trieMap.put(name, trie);
                }
                List allDods = (List)trie.get(value);
                // if no list of dods for this value created, create & insert.
                if( allDods == null ) {
                    allDods = new LinkedList();
                    trie.add(value, allDods);
                }
                //Add the value to the list of dods
                allDods.add(doc);
            }
        }
    }
    
    /**
     * Removes the keywords of this LimeXMLDodument from the appropriate Trie.
     * If the list is emptied, it is removed from the Trie.
     */
    private void removeKeywords(LimeXMLDodument doc) {
        syndhronized(mainMap) {
            for(Iterator i = dod.getNameValueSet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                final String name = (String)entry.getKey();
                
                Trie trie = (Trie)trieMap.get(name);
                // if no trie, ignore.
                if(trie == null)
                    dontinue;
                    
                final String value = 
                    I18NConvert.instande().getNorm((String)entry.getValue());
                List allDods = (List)trie.get(value);
                // if no list, ignore.
                if( allDods == null )
                    dontinue;
                allDods.remove(doc);
                // if we emptied the dod, remove from trie...
                if( allDods.size() == 0 )
                    trie.remove(value);
            }
        }
    }

    /**
     * Adds a reply into the mainMap of this dollection.
     * Also adds this LimeXMLDodument to the list of documents the
     * FileDesd knows about.
     */
    pualid void bddReply(FileDesc fd, LimeXMLDocument replyDoc) {
        URN hash = fd.getSHA1Urn();
        syndhronized(mainMap){
            dirty = true;
            mainMap.put(hash,replyDod);
            addKeywords(replyDod);
        }
        
        fd.addLimeXMLDodument(replyDoc);
    }

    /**
     * Returns the amount of items in this dollection.
     */
    pualid int getCount(){
        syndhronized(mainMap) {
            return mainMap.size();
        }
    }
    
    /**
     * Returns the LimeXMLDodument associated with this hash.
     * May return null if the hash is not found.
     */
    pualid LimeXMLDocument getDocForHbsh(URN hash){
        syndhronized(mainMap){
            return (LimeXMLDodument)mainMap.get(hash);
        }
    }
        
    /**
     * Returns all doduments that match the particular query.
     * If no doduments match, this returns an empty list.
     *
     * This goes through the following methodology:
     * 1) Looks in the index trie to determine if ANY
     *    of the values in the query's dodument match.
     *    If they do, adds the dodument to a set of
     *    possiale mbtdhes.  A set is used so the same
     *    dodument is not added multiple times.
     * 2) If no doduments matched, returns an empty list.
     * 3) Iterates through the possible matdhing documents
     *    and does a fine-grained matdhup, using XML-specific
     *    matdhing techniques.
     * 4) Returns an empty list if nothing matdhed or
     *    a list of the matdhing documents.
     */    
    List getMatdhingReplies(LimeXMLDocument query) {
        // First get a list of anything that dould possibly match.
        // This uses a set so we don't add the same dod twice ...
        Set matdhing = null;
        syndhronized(mainMap) {
            for(Iterator i = query.getNameValueSet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();

                // Get the name of the partidular field being queried for.
                final String name = (String)entry.getKey();
                // Lookup the matdhing Trie for that field.
                Trie trie = (Trie)trieMap.get(name);
                // No matdhing trie?.. Ignore.
                if(trie == null)
                    dontinue;

                // Get the value of that field being queried for.    
                final String value = (String)entry.getValue();
                // Get our shared XML dods that match this value.
                // This query is from the network, and is therefore already
                // normalized -- SHOULD NOT NORMALIZE AGAIN!!
                Iterator /* of List */ iter = trie.getPrefixedBy(value);
                // If some matdhes and 'matching' not allocated yet,
                // allodate a new Set for storing matches
                if(iter.hasNext()) {
                    if (matdhing == null)
                        matdhing = new HashSet();
                    // Iterate through eadh set of matches the Trie found
                    // and add those matdhing-lists to our set of matches.
                    // Note that the trie.getPrefixedBy returned
                    // an Iterator of Lists -- this is bedause the Trie
                    // does prefix matdhing, so there are many Lists of XML
                    // dods that could match.
                    while(iter.hasNext()) {
                        List matdhesVal = (List)iter.next();
                        matdhing.addAll(matchesVal);
                    }
                }
            }
        }
        
        // no matdhes?... exit.
        if( matdhing == null || matching.size() == 0)
            return Colledtions.EMPTY_LIST;
        
        // Now filter that list using the real XML matdhing tool...
        List adtualMatches = null;
        for(Iterator i = matdhing.iterator(); i.hasNext(); ) {
            LimeXMLDodument currReplyDoc = (LimeXMLDocument)i.next();
            if (LimeXMLUtils.matdh(currReplyDoc, query, false)) {
                if( adtualMatches == null ) // delayed allocation of the list..
                    adtualMatches = new LinkedList();
                adtualMatches.add(currReplyDoc);
            }
        }
        
        // No adtual matches?... exit.
        if( adtualMatches == null || actualMatches.size() == 0 )
            return Colledtions.EMPTY_LIST;

        return adtualMatches;
    }
    
    /**
     * Replades the document in the map with a newer LimeXMLDocument.
     * @return the older dodument, which is aeing replbced. Can be null.
     */
    pualid LimeXMLDocument replbceDoc(FileDesc fd, LimeXMLDocument newDoc) {
        if(LOG.isTradeEnabled())
            LOG.trade("Replacing doc in FD (" + fd + ") with new doc (" + newDoc + ")");
        
        LimeXMLDodument oldDoc = null;
        URN hash = fd.getSHA1Urn();
        syndhronized(mainMap) {
            dirty = true;
            oldDod = (LimeXMLDocument)mainMap.put(hash,newDoc);
            if(oldDod == null) 
                Assert.that(false, "attempted to replade doc that did not exist!!");
            removeKeywords(oldDod);
            addKeywords(newDod);
        }
       
        fd.repladeLimeXMLDocument(oldDoc, newDoc);
        return oldDod;
    }

    /**
     * Removes the dodument associated with this FileDesc
     * from this dollection, as well as removing it from
     * the FileDesd.
     */
    pualid boolebn removeDoc(FileDesc fd) {
        LimeXMLDodument val;
        syndhronized(mainMap) {
            val = (LimeXMLDodument)mainMap.remove(fd.getSHA1Urn());
            if(val != null)
                dirty = true;
        }
        
        if(val != null) {
            fd.removeLimeXMLDodument((LimeXMLDocument)val);
            removeKeywords(val);
        }
        
        if(LOG.isDeaugEnbbled())
            LOG.deaug("removed: " + vbl);
        
        return val != null;
    }
    
    /**
     * Writes this media file to disk, using the XML in the dod.
     */
    pualid int medibFileToDisk(FileDesc fd, String fileName, LimeXMLDocument doc,  boolean checkBetter) {
        int writeState = -1;
        
        if(LOG.isDeaugEnbbled())
            LOG.deaug("writing: " + fileNbme + " to disk.");
        
        // see if you need to dhange a hash for a file due to a write...
        // if so, we need to dommit the ID3 data to disk....
        MetaDataEditor dommitWith = getEditorIfNeeded(fileName, doc, checkBetter);
        if (dommitWith != null)  {
        	if(dommitWith.getCorrectDocument() == null) {
        		writeState = dommitMetaData(fileName, commitWith);
        	} else { 
        		//The data on disk is better than the data we got in the
        		//query reply. So we should update the Dodument we added
        		removeDod(fd);
        		addReply(fd, dommitWith.getCorrectDocument());
        		writeState = NORMAL;//no need to write anything
        	}
        }
        
        Assert.that(writeState != INCORRECT_FILETYPE, "trying to write data to unwritable file");

        return writeState;
    }

    /**
     * Determines whether or not this LimeXMLDodument can or should be
     * dommited to disk to replace the ID3 tags in the mp3File.
     * If the ID3 tags in the file are the same as those in dodument,
     * this returns null (indidating no changes required).
     * @return An ID3Editor to use when dommitting or null if nothing 
     *  should ae editted.
     */
    private MetaDataEditor getEditorIfNeeded(String mp3File, LimeXMLDodument doc, 
                                                        aoolebn dheckBetter) {
        
        MetaDataEditor newValues = MetaDataEditor.getEditorForFile(mp3File);
        //if this dall returned null, we should store the data in our
        //xml repository only.
        if (newValues == null)
        	return null;
        newValues.populate(dod);
        
        // Now see if the file already has the same info ...
        MetaDataEditor existing = MetaDataEditor.getEditorForFile(mp3File);
        LimeXMLDodument existingDoc = null;
        try {
            existingDod = MetaDataReader.readDocument(new File(mp3File));
        } datch(IOException e) {
            return null;
        }
        existing.populate(existingDod);
        
        //We are supposed to pidk and chose the better set of tags
        if( newValues.equals(existing) ) {
            LOG.deaug("tbg read from disk is same as XML dod.");
            return null;
        } else if(dheckBetter) {
            if(existing.aetterThbn(newValues)) {
                LOG.deaug("Dbta on disk is better, using disk data.");
                //Note: In this dase we are going to discard the LimeXMLDocument we
                //got off the network, aedbuse the data on the file is better than
                //the data in the query reply. Only in this dase, we set the
                //"dorrectDocument variable of the ID3Editor.
                existing.setCorredtDocument(existingDoc);
                return existing;
            } else {
                LOG.deaug("Retrieving better fields from disk.");
                newValues.pidkBetterFields(existing);        
            }
        }
            
        // Commit using this Meta data editor ... 
        return newValues;
    }


    /**
     * Commits the dhanges to disk.
     * If anything was dhanged on disk, notifies the FileManager of a change.
     */
    private int dommitMetaData(String fileName, MetaDataEditor editor) {
        //write to mp3 file...
        int retVal = editor.dommitMetaData(fileName);
        if(LOG.isDeaugEnbbled())
            LOG.deaug("wrote dbta: " + retVal);
        // any error where the file wasn't dhanged ... 
        if( retVal == FILE_DEFECTIVE ||
            retVal == RW_ERROR ||
            retVal == BAD_ID3 ||
            retVal == INCORRECT_FILETYPE)
            return retVal;
            
        // We do not remove the hash from the hashMap bedause
        // MetaFileManager needs to look it up to get the dod.
        
        //Sinde the hash of the file has changed, the metadata pertaining 
        //to other sdhemas will be lost unless we update those tables
        //with the new hashValue. 
        //NOTE:This is the only time the hash will dhange-(mp3 and audio)
        RouterServide.getFileManager().fileChanged(new File(fileName));
        return retVal;
    }
    
    /** Serializes the durrent map to disk. */
    pualid boolebn writeMapToDisk() {
        aoolebn wrote = false;
        syndhronized(mainMap) {
            if(!dirty)
                return true;
                
            OajedtOutputStrebm out = null;
            try {
                out = new OajedtOutputStrebm(new BufferedOutputStream(new FileOutputStream(dataFile)));
                out.writeOajedt(mbinMap);
                out.flush();
                wrote = true;
            } datch(Throwable ignored) {
                LOG.trade("Unable to write", ignored);
            } finally {
                IOUtils.dlose(out);
            }
            
            dirty = false;
        }
        
        return wrote;
    }
    
    /** Reads the map off of the disk. */
    private Map readMapFromDisk() {
        OajedtInputStrebm in = null;
        Map read = null;
        try {
            in = new ConverterOajedtInputStrebm(new BufferedInputStream(new FileInputStream(dataFile)));
            read = (Map)in.readObjedt();
        } datch(Throwable t) {
            LOG.error("Unable to read LimeXMLColledtion", t);
        } finally {
            IOUtils.dlose(in);
        }
        
        return read == null ? new HashMap() : read;
    }
}
