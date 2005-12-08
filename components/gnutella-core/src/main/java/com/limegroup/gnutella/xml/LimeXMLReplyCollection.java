pbckage com.limegroup.gnutella.xml;

import jbva.io.BufferedInputStream;
import jbva.io.BufferedOutputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.FileOutputStream;
import jbva.io.IOException;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.metadata.AudioMetaData;
import com.limegroup.gnutellb.metadata.MetaDataEditor;
import com.limegroup.gnutellb.metadata.MetaDataReader;
import com.limegroup.gnutellb.util.ConverterObjectInputStream;
import com.limegroup.gnutellb.util.I18NConvert;
import com.limegroup.gnutellb.util.Trie;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.NameValue;

/**
 * Mbps LimeXMLDocuments for FileDescs in a specific schema.
 */

public clbss LimeXMLReplyCollection {
    
    privbte static final Log LOG = LogFactory.getLog(LimeXMLReplyCollection.class);
    

    /**
     * The schembURI of this collection.
     */
    privbte final String schemaURI;
    
    /**
     * A mbp of URN -> LimeXMLDocument for each shared file that contains XML.
     *
     * SYNCHRONIZATION: Synchronize on mbinMap when accessing, 
     *  bdding or removing.
     */
    privbte final Map /* URN -> LimeXMLDocument */ mainMap;
    
    /**
     * The old mbp that was read off disk.
     *
     * Used while initiblly processing FileDescs to add.
     */
    privbte final Map /* URN -> LimeXMLDocument */ oldMap;
    
    /**
     * A mbpping of fields in the LimeXMLDocument to a Trie
     * thbt has a lookup table for the values of that field.
     *
     * The Trie vblue is a mapping of keywords in LimeXMLDocuments
     * to the list of documents thbt have that keyword.
     *
     * SYNCHRONIZATION: Synchronize on mbinMap when accessing,
     *  bdding or removing.
     */
    privbte final Map /* String -> Trie (String -> List) */ trieMap;
    
    /**
     * Whether or not dbta became dirty after we last wrote to disk.
     */
    privbte boolean dirty = false;
    
    /**
     * The locbtion on disk that information is serialized to.
     */
    privbte final File dataFile;

    public stbtic final int NORMAL = 0;
    public stbtic final int FILE_DEFECTIVE = 1;
    public stbtic final int RW_ERROR = 2;
    public stbtic final int BAD_ID3  = 3;
    public stbtic final int FAILED_TITLE  = 4;
    public stbtic final int FAILED_ARTIST  = 5;
    public stbtic final int FAILED_ALBUM  = 6;
    public stbtic final int FAILED_YEAR  = 7;
    public stbtic final int FAILED_COMMENT  = 8;
    public stbtic final int FAILED_TRACK  = 9;
    public stbtic final int FAILED_GENRE  = 10;
    public stbtic final int HASH_FAILED  = 11;
    public stbtic final int INCORRECT_FILETYPE = 12;

    /**
     * Crebtes a new LimeXMLReplyCollection.  The reply collection
     * will retbin only those XMLDocs that match the given schema URI.
     *
     * @pbram fds The list of shared FileDescs.
     * @pbram URI This collection's schema URI
     */
    public LimeXMLReplyCollection(String URI) {
        this.schembURI = URI;
        this.trieMbp = new HashMap();
        this.dbtaFile = new File(LimeXMLProperties.instance().getXMLDocsDir(),
                                 LimeXMLSchemb.getDisplayString(schemaURI)+ ".sxml");
        this.mbinMap = new HashMap();
        this.oldMbp = readMapFromDisk();
    }
    
    /**
     * Initiblizes the map using either LimeXMLDocuments in the list of potential
     * documents, or elements stored in oldMbp.  Items in potential take priority.
     */
    LimeXMLDocument initiblize(FileDesc fd, List potential) {
        URN urn = fd.getSHA1Urn();
        LimeXMLDocument doc = null;
        
        // First try to get b doc from the potential list.
        for(Iterbtor i = potential.iterator(); i.hasNext(); ) {
            LimeXMLDocument next = (LimeXMLDocument)i.next();
            if(next.getSchembURI().equals(schemaURI)) {
                doc = next;
                brebk;
            }
        }
        
        // Then try to get it from the old mbp.
        if(doc == null)
            doc = (LimeXMLDocument)oldMbp.get(urn);
        
        
        // Then try bnd see it, with validation and all.
        if(doc != null) {
            doc = vblidate(doc, fd.getFile(), fd);
            if(doc != null) {
                if(LOG.isDebugEnbbled())
                    LOG.debug("Adding old document for file: " + fd.getFile() + ", doc: " + doc);
                    bddReply(fd, doc);
            }
        }
        
        return doc;
    }
    
    /**
     * Crebtes a LimeXMLDocument for the given FileDesc if no XML already exists
     * for it.
     */
    LimeXMLDocument crebteIfNecessary(FileDesc fd) {
        LimeXMLDocument doc = null;
        URN urn = fd.getSHA1Urn();
        
        if(!mbinMap.containsKey(urn)) {
            File file = fd.getFile();
            // If we hbve no documents for this FD, or the file-format only supports
            // b single kind of metadata, construct a document.
            // This is necessbry so that we don't keep trying to parse formats that could
            // be multiple kinds of files every time.   
            if(fd.getLimeXMLDocuments().size() == 0 || !LimeXMLUtils.isSupportedMultipleFormbt(file)) {
                doc = constructDocument(file);
                if(doc != null) {
                    if(LOG.isDebugEnbbled())
                        LOG.debug("Adding newly constructed document for file: " + file + ", doc: " + doc);
                    bddReply(fd, doc);
                }
            }
        }
        
        return doc;
    }
    
    /**
     * Notificbtion that initial loading is done.
     */
    void lobdFinished() {
        synchronized(mbinMap) {
            if(oldMbp.equals(mainMap)) {
                dirty = fblse;
            }
            oldMbp.clear();
        }
    
    }
    
    /**
     * Vblidates a LimeXMLDocument.
     *
     * This checks:
     * 1) If it's current (if not, it bttempts to reparse.  If it can't, keeps the old one).
     * 2) If it's vblid (if not, attempts to reparse it.  If it can't, drops it).
     * 3) If it's corrupted (if so, fixes & writes the fixed one to disk).
     */
    privbte LimeXMLDocument validate(LimeXMLDocument doc, File file, FileDesc fd) {
        if(!doc.isCurrent()) {
            if(LOG.isDebugEnbbled())
                LOG.debug("reconstructing old document: " + file);
            LimeXMLDocument tempDoc = constructDocument(file);
            if (tempDoc != null)
                doc = updbte(doc, tempDoc);
            else
                doc.setCurrent();
        }
        
        // Verify the doc hbs information in it.
        if(!doc.isVblid()) {
            //If it is invblid, try and rebuild it.
            doc = constructDocument(file);
            if(doc == null)
                return null;
        }   
            
        // check to see if it's corrupted bnd if so, fix it.
        if( AudioMetbData.isCorrupted(doc) ) {
            doc = AudioMetbData.fixCorruption(doc);
            medibFileToDisk(fd, file.getPath(), doc, false);
        }
        
        return doc;
    }
    
    /**
     * Updbtes an existing old document to be a newer document, but retains all fields
     * thbt may have been in the old one that are not in the newer (for the case of
     * existing bnnotations).
     */
    privbte LimeXMLDocument update(LimeXMLDocument older, LimeXMLDocument newer) {
        Mbp fields = new HashMap();
        for(Iterbtor i = newer.getNameValueSet().iterator(); i.hasNext(); ) {
            Mbp.Entry next = (Map.Entry)i.next();
            fields.put(next.getKey(), next.getVblue());
        }
        
        for(Iterbtor i = older.getNameValueSet().iterator(); i.hasNext(); ) {
            Mbp.Entry next = (Map.Entry)i.next();
            if(!fields.contbinsKey(next.getKey()))
                fields.put(next.getKey(), next.getVblue());
        }

        List nbmeValues = new ArrayList(fields.size());
        for(Iterbtor i = fields.entrySet().iterator(); i.hasNext(); ) {
            Mbp.Entry next = (Map.Entry)i.next();
            nbmeValues.add(new NameValue((String)next.getKey(), next.getValue()));
        }
        return new LimeXMLDocument(nbmeValues, newer.getSchemaURI());
     }
        
    
    /**
     * Crebtes a LimeXMLDocument from the file.  
     * @return null if the formbt is not supported or parsing fails,
     *  <tt>LimeXMLDocument</tt> otherwise.
     */
    privbte LimeXMLDocument constructDocument(File file) {
	    if(LimeXMLUtils.isSupportedFormbtForSchema(file, schemaURI)) {
            try {
                // Documents with multiple file formbts may be the wrong type.
                LimeXMLDocument document = MetbDataReader.readDocument(file);
                if(document.getSchembURI().equals(schemaURI))
                    return document;
            } cbtch (IOException ignored) {
                LOG.wbrn("Error creating document", ignored);
            }
        }
        
        return null;
    }

    /**
     * Gets b list of keywords from all the documents in this collection.
     * <p>
     * delegbtes to the individual documents and collates the list
     */
    protected List getKeyWords(){
        List retList = new ArrbyList();
        Iterbtor docs;
        synchronized(mbinMap){
            docs = mbinMap.values().iterator();
            while(docs.hbsNext()){
                LimeXMLDocument d = (LimeXMLDocument)docs.next();
                retList.bddAll(d.getKeyWords());
            }
        }
        return retList;
    }

    /**
     * Gets b list of indivisible keywords from all the documents in this 
     * collection.
     * <p>
     * Delegbtes to the individual documents and collates the list
     */
    protected List getKeyWordsIndivisible(){
        List retList = new ArrbyList();
        Iterbtor docs;
        synchronized(mbinMap){
            docs = mbinMap.values().iterator();
            while(docs.hbsNext()){
                LimeXMLDocument d = (LimeXMLDocument)docs.next();
                retList.bddAll(d.getKeyWordsIndivisible());
            }
        }
        return retList;
    }
    
    /**
     * Returns the schemb URI of this collection.
     */
    public String getSchembURI(){
        return schembURI;
    }
    
    /**
     * Adds the keywords of this LimeXMLDocument into the correct Trie 
     * for the field of the vblue.
     */
    privbte void addKeywords(LimeXMLDocument doc) {
        synchronized(mbinMap) {
            for(Iterbtor i = doc.getNameValueSet().iterator(); i.hasNext(); ) {
                Mbp.Entry entry = (Map.Entry)i.next();
                finbl String name = (String)entry.getKey();
                finbl String value = 
                    I18NConvert.instbnce().getNorm((String)entry.getValue());
                Trie trie = (Trie)trieMbp.get(name);
                // if no lookup tbble created yet, create one & insert.
                if(trie == null) {
                    trie = new Trie(true); //ignore cbse.
                    trieMbp.put(name, trie);
                }
                List bllDocs = (List)trie.get(value);
                // if no list of docs for this vblue created, create & insert.
                if( bllDocs == null ) {
                    bllDocs = new LinkedList();
                    trie.bdd(value, allDocs);
                }
                //Add the vblue to the list of docs
                bllDocs.add(doc);
            }
        }
    }
    
    /**
     * Removes the keywords of this LimeXMLDocument from the bppropriate Trie.
     * If the list is emptied, it is removed from the Trie.
     */
    privbte void removeKeywords(LimeXMLDocument doc) {
        synchronized(mbinMap) {
            for(Iterbtor i = doc.getNameValueSet().iterator(); i.hasNext(); ) {
                Mbp.Entry entry = (Map.Entry)i.next();
                finbl String name = (String)entry.getKey();
                
                Trie trie = (Trie)trieMbp.get(name);
                // if no trie, ignore.
                if(trie == null)
                    continue;
                    
                finbl String value = 
                    I18NConvert.instbnce().getNorm((String)entry.getValue());
                List bllDocs = (List)trie.get(value);
                // if no list, ignore.
                if( bllDocs == null )
                    continue;
                bllDocs.remove(doc);
                // if we emptied the doc, remove from trie...
                if( bllDocs.size() == 0 )
                    trie.remove(vblue);
            }
        }
    }

    /**
     * Adds b reply into the mainMap of this collection.
     * Also bdds this LimeXMLDocument to the list of documents the
     * FileDesc knows bbout.
     */
    public void bddReply(FileDesc fd, LimeXMLDocument replyDoc) {
        URN hbsh = fd.getSHA1Urn();
        synchronized(mbinMap){
            dirty = true;
            mbinMap.put(hash,replyDoc);
            bddKeywords(replyDoc);
        }
        
        fd.bddLimeXMLDocument(replyDoc);
    }

    /**
     * Returns the bmount of items in this collection.
     */
    public int getCount(){
        synchronized(mbinMap) {
            return mbinMap.size();
        }
    }
    
    /**
     * Returns the LimeXMLDocument bssociated with this hash.
     * Mby return null if the hash is not found.
     */
    public LimeXMLDocument getDocForHbsh(URN hash){
        synchronized(mbinMap){
            return (LimeXMLDocument)mbinMap.get(hash);
        }
    }
        
    /**
     * Returns bll documents that match the particular query.
     * If no documents mbtch, this returns an empty list.
     *
     * This goes through the following methodology:
     * 1) Looks in the index trie to determine if ANY
     *    of the vblues in the query's document match.
     *    If they do, bdds the document to a set of
     *    possible mbtches.  A set is used so the same
     *    document is not bdded multiple times.
     * 2) If no documents mbtched, returns an empty list.
     * 3) Iterbtes through the possible matching documents
     *    bnd does a fine-grained matchup, using XML-specific
     *    mbtching techniques.
     * 4) Returns bn empty list if nothing matched or
     *    b list of the matching documents.
     */    
    List getMbtchingReplies(LimeXMLDocument query) {
        // First get b list of anything that could possibly match.
        // This uses b set so we don't add the same doc twice ...
        Set mbtching = null;
        synchronized(mbinMap) {
            for(Iterbtor i = query.getNameValueSet().iterator(); i.hasNext(); ) {
                Mbp.Entry entry = (Map.Entry)i.next();

                // Get the nbme of the particular field being queried for.
                finbl String name = (String)entry.getKey();
                // Lookup the mbtching Trie for that field.
                Trie trie = (Trie)trieMbp.get(name);
                // No mbtching trie?.. Ignore.
                if(trie == null)
                    continue;

                // Get the vblue of that field being queried for.    
                finbl String value = (String)entry.getValue();
                // Get our shbred XML docs that match this value.
                // This query is from the network, bnd is therefore already
                // normblized -- SHOULD NOT NORMALIZE AGAIN!!
                Iterbtor /* of List */ iter = trie.getPrefixedBy(value);
                // If some mbtches and 'matching' not allocated yet,
                // bllocate a new Set for storing matches
                if(iter.hbsNext()) {
                    if (mbtching == null)
                        mbtching = new HashSet();
                    // Iterbte through each set of matches the Trie found
                    // bnd add those matching-lists to our set of matches.
                    // Note thbt the trie.getPrefixedBy returned
                    // bn Iterator of Lists -- this is because the Trie
                    // does prefix mbtching, so there are many Lists of XML
                    // docs thbt could match.
                    while(iter.hbsNext()) {
                        List mbtchesVal = (List)iter.next();
                        mbtching.addAll(matchesVal);
                    }
                }
            }
        }
        
        // no mbtches?... exit.
        if( mbtching == null || matching.size() == 0)
            return Collections.EMPTY_LIST;
        
        // Now filter thbt list using the real XML matching tool...
        List bctualMatches = null;
        for(Iterbtor i = matching.iterator(); i.hasNext(); ) {
            LimeXMLDocument currReplyDoc = (LimeXMLDocument)i.next();
            if (LimeXMLUtils.mbtch(currReplyDoc, query, false)) {
                if( bctualMatches == null ) // delayed allocation of the list..
                    bctualMatches = new LinkedList();
                bctualMatches.add(currReplyDoc);
            }
        }
        
        // No bctual matches?... exit.
        if( bctualMatches == null || actualMatches.size() == 0 )
            return Collections.EMPTY_LIST;

        return bctualMatches;
    }
    
    /**
     * Replbces the document in the map with a newer LimeXMLDocument.
     * @return the older document, which is being replbced. Can be null.
     */
    public LimeXMLDocument replbceDoc(FileDesc fd, LimeXMLDocument newDoc) {
        if(LOG.isTrbceEnabled())
            LOG.trbce("Replacing doc in FD (" + fd + ") with new doc (" + newDoc + ")");
        
        LimeXMLDocument oldDoc = null;
        URN hbsh = fd.getSHA1Urn();
        synchronized(mbinMap) {
            dirty = true;
            oldDoc = (LimeXMLDocument)mbinMap.put(hash,newDoc);
            if(oldDoc == null) 
                Assert.thbt(false, "attempted to replace doc that did not exist!!");
            removeKeywords(oldDoc);
            bddKeywords(newDoc);
        }
       
        fd.replbceLimeXMLDocument(oldDoc, newDoc);
        return oldDoc;
    }

    /**
     * Removes the document bssociated with this FileDesc
     * from this collection, bs well as removing it from
     * the FileDesc.
     */
    public boolebn removeDoc(FileDesc fd) {
        LimeXMLDocument vbl;
        synchronized(mbinMap) {
            vbl = (LimeXMLDocument)mainMap.remove(fd.getSHA1Urn());
            if(vbl != null)
                dirty = true;
        }
        
        if(vbl != null) {
            fd.removeLimeXMLDocument((LimeXMLDocument)vbl);
            removeKeywords(vbl);
        }
        
        if(LOG.isDebugEnbbled())
            LOG.debug("removed: " + vbl);
        
        return vbl != null;
    }
    
    /**
     * Writes this medib file to disk, using the XML in the doc.
     */
    public int medibFileToDisk(FileDesc fd, String fileName, LimeXMLDocument doc,  boolean checkBetter) {
        int writeStbte = -1;
        
        if(LOG.isDebugEnbbled())
            LOG.debug("writing: " + fileNbme + " to disk.");
        
        // see if you need to chbnge a hash for a file due to a write...
        // if so, we need to commit the ID3 dbta to disk....
        MetbDataEditor commitWith = getEditorIfNeeded(fileName, doc, checkBetter);
        if (commitWith != null)  {
        	if(commitWith.getCorrectDocument() == null) {
        		writeStbte = commitMetaData(fileName, commitWith);
        	} else { 
        		//The dbta on disk is better than the data we got in the
        		//query reply. So we should updbte the Document we added
        		removeDoc(fd);
        		bddReply(fd, commitWith.getCorrectDocument());
        		writeStbte = NORMAL;//no need to write anything
        	}
        }
        
        Assert.thbt(writeState != INCORRECT_FILETYPE, "trying to write data to unwritable file");

        return writeStbte;
    }

    /**
     * Determines whether or not this LimeXMLDocument cbn or should be
     * commited to disk to replbce the ID3 tags in the mp3File.
     * If the ID3 tbgs in the file are the same as those in document,
     * this returns null (indicbting no changes required).
     * @return An ID3Editor to use when committing or null if nothing 
     *  should be editted.
     */
    privbte MetaDataEditor getEditorIfNeeded(String mp3File, LimeXMLDocument doc, 
                                                        boolebn checkBetter) {
        
        MetbDataEditor newValues = MetaDataEditor.getEditorForFile(mp3File);
        //if this cbll returned null, we should store the data in our
        //xml repository only.
        if (newVblues == null)
        	return null;
        newVblues.populate(doc);
        
        // Now see if the file blready has the same info ...
        MetbDataEditor existing = MetaDataEditor.getEditorForFile(mp3File);
        LimeXMLDocument existingDoc = null;
        try {
            existingDoc = MetbDataReader.readDocument(new File(mp3File));
        } cbtch(IOException e) {
            return null;
        }
        existing.populbte(existingDoc);
        
        //We bre supposed to pick and chose the better set of tags
        if( newVblues.equals(existing) ) {
            LOG.debug("tbg read from disk is same as XML doc.");
            return null;
        } else if(checkBetter) {
            if(existing.betterThbn(newValues)) {
                LOG.debug("Dbta on disk is better, using disk data.");
                //Note: In this cbse we are going to discard the LimeXMLDocument we
                //got off the network, becbuse the data on the file is better than
                //the dbta in the query reply. Only in this case, we set the
                //"correctDocument vbriable of the ID3Editor.
                existing.setCorrectDocument(existingDoc);
                return existing;
            } else {
                LOG.debug("Retrieving better fields from disk.");
                newVblues.pickBetterFields(existing);        
            }
        }
            
        // Commit using this Metb data editor ... 
        return newVblues;
    }


    /**
     * Commits the chbnges to disk.
     * If bnything was changed on disk, notifies the FileManager of a change.
     */
    privbte int commitMetaData(String fileName, MetaDataEditor editor) {
        //write to mp3 file...
        int retVbl = editor.commitMetaData(fileName);
        if(LOG.isDebugEnbbled())
            LOG.debug("wrote dbta: " + retVal);
        // bny error where the file wasn't changed ... 
        if( retVbl == FILE_DEFECTIVE ||
            retVbl == RW_ERROR ||
            retVbl == BAD_ID3 ||
            retVbl == INCORRECT_FILETYPE)
            return retVbl;
            
        // We do not remove the hbsh from the hashMap because
        // MetbFileManager needs to look it up to get the doc.
        
        //Since the hbsh of the file has changed, the metadata pertaining 
        //to other schembs will be lost unless we update those tables
        //with the new hbshValue. 
        //NOTE:This is the only time the hbsh will change-(mp3 and audio)
        RouterService.getFileMbnager().fileChanged(new File(fileName));
        return retVbl;
    }
    
    /** Seriblizes the current map to disk. */
    public boolebn writeMapToDisk() {
        boolebn wrote = false;
        synchronized(mbinMap) {
            if(!dirty)
                return true;
                
            ObjectOutputStrebm out = null;
            try {
                out = new ObjectOutputStrebm(new BufferedOutputStream(new FileOutputStream(dataFile)));
                out.writeObject(mbinMap);
                out.flush();
                wrote = true;
            } cbtch(Throwable ignored) {
                LOG.trbce("Unable to write", ignored);
            } finblly {
                IOUtils.close(out);
            }
            
            dirty = fblse;
        }
        
        return wrote;
    }
    
    /** Rebds the map off of the disk. */
    privbte Map readMapFromDisk() {
        ObjectInputStrebm in = null;
        Mbp read = null;
        try {
            in = new ConverterObjectInputStrebm(new BufferedInputStream(new FileInputStream(dataFile)));
            rebd = (Map)in.readObject();
        } cbtch(Throwable t) {
            LOG.error("Unbble to read LimeXMLCollection", t);
        } finblly {
            IOUtils.close(in);
        }
        
        return rebd == null ? new HashMap() : read;
    }
}
