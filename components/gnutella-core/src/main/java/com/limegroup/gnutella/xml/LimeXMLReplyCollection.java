package com.limegroup.gnutella.xml;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.mp3.*;
import com.limegroup.gnutella.*;
import java.io.*;
import org.xml.sax.*;

/**
 *  Stores a schema and a list of replies corresponding to that schema.
 *  <p>
 *  So when a search comes in, we only have to look at the set of replies
 *  that correspond to the schema of the query.
 * 
 *  Locking: Never obtain a this' monitor PRIOR to obtaining that of the
 *  FileManager.
 * @author Sumeet Thadani
 */

public class LimeXMLReplyCollection{

    /**
     * The schemaURI of this collection.
     */
    private final String schemaURI;
    
    /**
     * A map of URN -> LimeXMLDocument for each shared file that contains XML.
     * Each ReplyCollection is written out to one physical file on shutdown.
     */
    private final HashMap /* URN -> LimeXMLDocument */ mainMap;
    
    /**
     * Whether or not this LimeXMLReplyCollection is for audio files.
     */
    private final boolean audio;
    
    /**
     * The location on disk that information is serialized to.
     */
    private File dataFile = null;
    
    /**
     * Obtain WRITE_LOCK before writing to disk.
     */
    private final Object WRITE_LOCK = new Object(); 

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

    /**
     * Creates a new LimeXMLReplyCollection.  The reply collection
     * will retain only those XMLDocs that match the given schema URI.
     *
     * @param fds The list of shared FileDescs.
     * @param URI This collection's schema URI
     * @param audio Whether this is a collection of audio files.
     */
    public LimeXMLReplyCollection(FileDesc[] fds, String URI, boolean audio) {
        this.schemaURI = URI;
        this.audio = audio;
        debug("LimeXMLReplyCollection(): entered with audio = " +
              audio);

        // construct a backing store object (for serialization)
        mainMap = new HashMap();
        MapSerializer ms = initializeMapSerializer(URI);
        Map hashToXML;

        //if File is invalid, ms== null
        if (ms == null) // create a dummy
            hashToXML = new HashMap();
        else 
            hashToXML = ms.getMap();
            
        // The serialization of mainMap went through three stages in LimeWire.
        // Prior to LimeWire 2.5, the map was stored as
        //      String (xml mini hash) -> String (XML).
        // After that (before LimeWire 3.4) it was stored as
        //      String (xml mini hash) -> LimeXMLDocument
        // From LimeWire 3.4 on, it is stored as
        //      URN (SHA1 Hash) -> LimeXMLDocument        
        // Because of the changes, and the need to support reading older
        // .sxml files (so we don't lose any annotated XML), we need to
        // ensure that we can handle all cases and update them to the
        // current format.
        
        // This iterates over each shared FileDesc to find the associated entry
        // in the map read off disk. If no entry is found, it could be for a 
        // few reasons:
        // 1) The file has no XML associated with it.
        // 2) The entry is stored as String -> String or String -> LimeXMLDoc
        // Because reason one is common, and reason two will only occur during
        // the first time LimeWire 3.4 is started (and a previous version
        // of LimeWire had already run once), and we don't want to do the mini
        // hash to perform a lookup for every file that doesn't have XML,
        // we glance at the first entry in the deserialized map and see if
        // the key is a String or a URN.  If it is a String, we'll assume
        // all missing entries are because of reason 2.  If it is a URN
        // we'll assume all missing entries are because of 1.
        // If there are no entries in the serialized map, we'll also assume 1.
        
        boolean requiresConversion = false;
        {
            Iterator iter = hashToXML.keySet().iterator();
            if( iter.hasNext() )
                requiresConversion = ( iter.next() instanceof String );
            debug("requiresConversion: " + requiresConversion);
        }
        
        for(int i = 0; i < fds.length; i++) {
            FileDesc fd = fds[i];
            if( fd instanceof IncompleteFileDesc ) //ignore incompletes
                continue;

            File file = fd.getFile();
            URN hash = fd.getSHA1Urn();
            Object xml = null;
            LimeXMLDocument doc = null;
            
            //If requiresConversion is true, a lookup of the URN
            //is pointless because the hashToXML's keys are
            //a String (mini-hash).
            if( requiresConversion ) { //Before LimeWire 3.4
                String miniHash = null;
                try {
                    miniHash = new String(LimeXMLUtils.hashFile(file));
                } catch(IOException e) {
                    continue; // oh well.
                }
                xml = hashToXML.get(miniHash);
                // If this was between LimeWire 2.5 and LimeWire 3.4...
                // and it had some XML..
                if( xml != null && xml instanceof LimeXMLDocument ) {
                    doc = (LimeXMLDocument)xml;
                } else { // Pre LimeWire 2.5 or no XML stored.
                    doc = constructDocument((String)xml, file);
                }
            } else { // After LimeWire 3.4
                xml = hashToXML.get(hash);
                if( xml == null ) { // no XML might exist, try and make some
                    doc = constructDocument(null, file);
                } else { //it had a doc already.
                    doc = (LimeXMLDocument)xml;
                }
            }
            
            if( doc == null ) // no document, ignore.
                continue;
                            
            // We have a document, add it.
            addReply(fd, doc);
        }
    
        debug("LimeXMLReplyCollection(): returning.");

        write();
    }
    
    /**
     * Creates a LimeXMLDocument from the XML String.
     * If the string is null, the collection is for audio files,
     * and the file is an MP3 file, it reads the file to create some XML.
     */
    private LimeXMLDocument constructDocument(String xmlStr, File file) {
        // old style may exist or there may be no xml associated
        // with this file yet.....
        if (audio && LimeXMLUtils.isMP3File(file)) {
            // first try to get the id3 out of it.  if this file has
            // no id3 tag, just construct the doc out of the xml 
            // string....
            boolean onlyID3=((xmlStr == null) || xmlStr.equals(""));
            try {
                if(!onlyID3) {  //non-id3 values with mp3 file
                    String id3XML = ID3Reader.readDocument(file,onlyID3);
                    String joinedXML = joinAudioXMLStrings(id3XML, xmlStr);
                    if( joinedXML != null )
                        return new LimeXMLDocument(joinedXML);
                }
                // no XML data we can use.
                return ID3Reader.readDocument(file);
            }
            catch (SAXException ignored) { }
            catch (IOException ignored) { }
            catch (SchemaNotFoundException ignored) { }
        }
        else { // !audio || !mp3
            try {
                if ((xmlStr != null) && (!xmlStr.equals(""))) 
                    return new LimeXMLDocument(xmlStr);
            }
            catch (SAXException ignored) { }
            catch (IOException ignored) { }
            catch (SchemaNotFoundException ignored) { }
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
     * Creates the MapSerializer object that deserializes the .sxml file.
     * @return the MapSerializer or null if there was an exception
     *   while creating the MapSerializer
     */
    private MapSerializer initializeMapSerializer(String URI){
        String fname = LimeXMLSchema.getDisplayString(URI)+".sxml";
        LimeXMLProperties props = LimeXMLProperties.instance();
        String path = props.getXMLDocsDir();
        dataFile = new File(path,fname);
        // invalid if directory.
        if( dataFile.isDirectory() )
            return null;
        try {
            return new MapSerializer(dataFile);
        } catch(IOException e) {
            debug(e);
            return null;
        }
    }

    /**
     * Joins two XML strings together.
     * Returns null if the second string is malformed.
     */
    private String joinAudioXMLStrings(String mp3Str, String fileStr) {
        int p = fileStr.lastIndexOf("></audio>");
        if( p == -1 )
            return null;
            
        //above line is the one closing the root element
        String a = fileStr.substring(0,p);//all but the closing part
        String b = fileStr.substring(p);//closing part
        //phew, thank god this schema has depth 1.
        return(a+mp3Str+b);
    }

    
    /**
     * Returns the schema URI of this collection.
     */
    public String getSchemaURI(){
        return schemaURI;
    }

    /**
     * Adds a reply into the mainMap of this collection.
     * Also adds this LimeXMLDocument to the list of documents the
     * FileDesc knows about.
     */
    public void addReply(FileDesc fd, LimeXMLDocument replyDoc) {
        URN hash = fd.getSHA1Urn();
        synchronized(mainMap){
            mainMap.put(hash,replyDoc);
        }
        fd.addLimeXMLDocument(replyDoc);
        try {
            String identifier = fd.getFile().getCanonicalPath();
            replyDoc.setIdentifier(identifier);
        } catch(IOException ignored) {}
    }


    /**
     * Adds a reply into the mainMap of this collection, associating
     * the FileDesc with the LimeXMLDocument.
     * If this collection is an audio collection, this will write out the
     * file to disk, possibly adding/changing ID3 tags on an MP3 file.
     * If the file changed because of this operation, the FileManager
     * is notified of the changed file, redoing its hashes.
     * Regardless of whether or not this collection is for audio files,
     * the map of (URN -> LimeXMLDocument) will always be serialized
     * to disk.
     */
    void addReplyWithCommit(File f, FileDesc fd, LimeXMLDocument replyDoc) {
        addReply(fd, replyDoc);
        
        // commit to disk...
        if (audio) {
            try {
                mp3ToDisk(f.getCanonicalPath(), replyDoc);
            } catch(IOException ignored) {}
        } else
            write();
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
     * Returns whether or not this reply collection is for audio files.
     */
    public boolean isAudio() {
        return audio;
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
     */    
    public List getMatchingReplies(LimeXMLDocument queryDoc){
        List matchingReplyDocs;
        synchronized(mainMap){
            Iterator iter = mainMap.values().iterator();
            matchingReplyDocs = new ArrayList();
            while(iter.hasNext()) {
                LimeXMLDocument currReplyDoc = (LimeXMLDocument)iter.next();
                if (LimeXMLUtils.match(currReplyDoc, queryDoc))
                    matchingReplyDocs.add(currReplyDoc);
            }
        }
        return matchingReplyDocs;
    }
    
    /**
     * Replaces the document in the map with a newer LimeXMLDocument.
     * @return the older document, which is being replaced. Can be null.
     */
    public LimeXMLDocument replaceDoc(FileDesc fd, LimeXMLDocument newDoc){
        LimeXMLDocument oldDoc = null;
        URN hash = fd.getSHA1Urn();
        synchronized(mainMap){
            oldDoc = (LimeXMLDocument)mainMap.put(hash,newDoc);
        }
        
        if(oldDoc == null)  {
            Assert.that(false, "attempted to replace doc that did not exist!!");
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
        URN hash = fd.getSHA1Urn();
        boolean found;
        Object val;
        synchronized(mainMap){
            val = mainMap.remove(hash);
            found = (val != null);
        }
        
        boolean written = false;
        
        if(found){
            written = write();
            if( written ) {
                fd.removeLimeXMLDocument((LimeXMLDocument)val);
            } else { // put it back to maintain consistency
                synchronized(mainMap) {
                    mainMap.put(hash,val);
                }
            }
        }
        
        debug("found: " + found + ", written: " + written);
        
        return (found && written);
    }
    

    /** 
     * Simply write() out the mainMap to disk. 
     */
    public boolean write(){
        if(dataFile==null){//calculate it
            String fname = LimeXMLSchema.getDisplayString(schemaURI)+".sxml";
            LimeXMLProperties props = LimeXMLProperties.instance();
            String path = props.getXMLDocsDir();
            dataFile = new File(path,fname);
        }
        // invalid if directory
        if( dataFile.isDirectory() )
            return false;
    
        try {
            MapSerializer ms = new MapSerializer(dataFile, mainMap);
            synchronized (WRITE_LOCK) {
                ms.commit();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    
    /**
     * Writes this mp3 file to disk, using the XML in the doc.
     */
    public int mp3ToDisk(String mp3FileName, LimeXMLDocument doc) {
        boolean wrote=false;
        int mp3WriteState = -1;
        
        debug("writing: " + mp3FileName + " to disk.");

        // see if you need to change a hash for a file due to a write...
        // if so, we need to commit the ID3 data to disk....
        ID3Editor commitWith = ripMP3XML(mp3FileName, doc);
        if (commitWith != null)  // commit to disk.
            mp3WriteState = commitID3Data(mp3FileName, commitWith);
        
        // write out the mainmap in serial form...
        wrote = write();

        if(!wrote) //writing serialized map failed
            return RW_ERROR;

        return mp3WriteState;//wrote successful, return mp3WriteState
    }

    /**
     * Determines whether or not this LimeXMLDocument can or should be
     * commited to disk to replace the ID3 tags in the mp3File.
     * If the ID3 tags in the file are the same as those in document,
     * this returns null (indicating no changes required).
     * @return An ID3Editor to use when committing or null if nothing 
     *  should be editted.
     */
    private ID3Editor ripMP3XML(String mp3File, LimeXMLDocument doc){
        if (!LimeXMLUtils.isMP3File(mp3File))
            return null;

        ID3Editor newValues = new ID3Editor();
        String newXML = null;

        try {
            newXML = doc.getXMLStringWithIdentifier();
        } catch(SchemaNotFoundException snfe) {
            return null;
        }       
        newValues.removeID3Tags(newXML);
        
        // Now see if the file already has the same info ...
        ID3Editor existing = new ID3Editor();
        LimeXMLDocument existingDoc = null;
        try {
            existingDoc = ID3Reader.readDocument(new File(mp3File));
        } catch(IOException e) {
            return null;
        }
        String existingXML = null;
        try {
            existingXML = existingDoc.getXMLStringWithIdentifier();
        } catch(SchemaNotFoundException snfe) {
            return null;
        }
        existing.removeID3Tags(existingXML);
        
        // The ID3 tag is the same as the document, don't do anything.
        if( newValues.equals(existing) ) {
            debug("tag read from disk is same as XML doc.");
            return null;
        }

        // Commit using this ID3Editor ... 
        return newValues;
    }


    /**
     * Commits the changes to disk.
     * If anything was changed on disk, notifies the FileManager of a change.
     */
    private int commitID3Data(String mp3FileName,
                              ID3Editor editor) {
        //write to mp3 file...
        int retVal = editor.writeID3DataToDisk(mp3FileName);
        debug("wrote data: " + retVal);
        // any error where the file wasn't changed ... 
        if( retVal == FILE_DEFECTIVE ||
            retVal == RW_ERROR ||
            retVal == BAD_ID3 )
            return retVal;
            
        // We do not remove the hash from the hashMap because
        // MetaFileManager needs to look it up to get the doc.
        
        //Since the hash of the file has changed, the metadata pertaiing 
        //to other schemas will be lost unless we update those tables
        //with the new hashValue. 
        //NOTE:This is the only time the hash will change-(mp3 and audio)
        RouterService.getFileManager().fileChanged(new File(mp3FileName));
        return retVal;
    }


    static class MapSerializer {

        /** Where to serialize/deserialize from.
         */
        private File _backingStoreFile;
        
        /** underlying map for hashmap access.
         */
        private Map _hashMap;

        /** @param whereToStore The name of the file to serialize from / 
         *  deserialize to.
         *  @exception IOException if there was a problem deserializing the
         *    file.
         */
        MapSerializer(File whereToStore) throws IOException {
            _backingStoreFile = whereToStore;
            if (_backingStoreFile.exists())
                deserializeFromFile();
            else
                _hashMap = new HashMap();
        }


        /** @param whereToStore The name of the file to serialize from / 
         *  deserialize to.  
         *  @param storage A HashMap that you want to serialize / deserialize.
         */
        MapSerializer(File whereToStore, Map storage) {
            _backingStoreFile = whereToStore;
            _hashMap = storage;
        }


        private void deserializeFromFile() throws IOException {
            FileInputStream istream = null;
            ObjectInputStream objStream = null;
            try {
                istream = new FileInputStream(_backingStoreFile);
                objStream = new ObjectInputStream(istream);
                _hashMap = (Map) objStream.readObject();
//              for(Iterator it=_hashMap.entrySet().iterator();it.hasNext();){
//                  Map.Entry ent = (Map.Entry)it.next();
//                  debug("read " + ent.getKey() + ", " + ent.getValue());
//              }
            } catch(ClassNotFoundException cnfe) {
                throw new IOException("class not found");
            } catch(ClassCastException cce) {
                throw new IOException("class cast");
            } finally {
                if( istream != null ) {
                    try {
                        istream.close();
                    } catch(IOException ignored) {}
                }
            }
        }

        /** Call this method when you want to force the contents to the HashMap
         *  to disk.
         *  @exception IOException Thrown if force to disk failed.
         */
        public void commit() throws IOException {
            serializeToFile();
        }

        
        private void serializeToFile() throws IOException {
            FileOutputStream ostream = null;
            ObjectOutputStream objStream = null;
            try {
                ostream = new FileOutputStream(_backingStoreFile);
                objStream = new ObjectOutputStream(ostream);
                synchronized (_hashMap) {
                    objStream.writeObject(_hashMap);
                }
            } finally {
                if( ostream != null ) {
                    try {
                        ostream.close();
                    } catch(IOException ignored) {}
                }
            }
        }

        /** @return The Map this class encases.
         */
        public Map getMap() {
            return _hashMap;
        }

    }

    private final static boolean debugOn = false;
    private final static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final static void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }
}
