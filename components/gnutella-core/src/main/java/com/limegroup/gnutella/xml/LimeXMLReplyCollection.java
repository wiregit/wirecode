package com.limegroup.gnutella.xml;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.mp3.*;
import com.limegroup.gnutella.*;
import java.io.*;
import org.xml.sax.*;

/**
 *  Stores a schema and a list of Replies corresponding to the 
 *  the corresponding to this schema.
 *  <p>
 *  So when a search comes in, we only have to look at the set of replies
 *  that correspond to the schema of the query.
 * 
 *  Locking: Never obtain a this' monitor PRIOR to obtaining that of the
 *  FileManager.
 * @author Sumeet Thadani
 */

public class LimeXMLReplyCollection{
    
    private String schemaURI;
    
    /**
     * A map of URN -> LimeXMLDocument for each shared file
     * that contains XML.
     * Note: Each ReplyCollection is written out to 1 physical file on 
     * shutdown.
     * 
     * LOCKING: Never obtain fileToHash's lock after obtaining
     *          mainMap's lock.
     */
    private HashMap /* URN -> LimeXMLDocument */ mainMap;
    public boolean audio = false;//package access
    private File dataFile = null;//flat file where all data is stored.
    private MetaFileManager metaFileManager = null;
    
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
     * @param fileToHash The map of files to hashes for all shared files.
     * @param URI This collection's schema URI
     * @param fm A pointer to the system wide (Meta)FileManager .
     * @param audio Whether this is a collection of audio files.
     */
    public LimeXMLReplyCollection(Map fileToHash, String URI, 
                                  FileManager fm, boolean audio) {
        this.schemaURI = URI;
        this.metaFileManager = (MetaFileManager)fm;
        this.audio = audio;
        debug("LimeXMLReplyCollection(): entered with audio = " +
              audio);

        // construct a backing store object (for serialization)
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
        // After that (before LimeWire 3.3) it was stored as
        //      String (xml mini hash) -> LimeXMLDocument
        // From LimeWire 3.3 on, it is stored as
        //      URN (SHA1 Hash) -> LimeXMLDocument        
        // Because of the changes, and the need to support reading older
        // .sxml files (so we don't lose any annotated XML), we need to
        // ensure that we can handle all cases and update them to the
        // current format.
        
        // This iterates over each entry in fileToHash (File -> URN)
        // to find the associated entry in the map read off disk.
        // If no entry is found, it could be for a few reasons:
        // 1) The file has no XML associated with it.
        // 2) The entry is stored as String -> String or String -> LimeXMLDoc
        // Because reason one is common, and reason two will only occur during
        // the first time LimeWire 3.3 is started (and a previous version
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
        }
        
        synchronized(fileToHash) {
            Iterator iter = fileToHash.entrySet().iterator();
            while( (iter != null) && iter.hasNext() ) {
                Map.Entry entry = (Map.Entry)iter.next();
                File file = (File)entry.getKey();
                URN hash = (URN)entry.getValue();
                Object xml = null;
                LimeXMLDocument doc = null;
                
                //If requiresConversion is true, a lookup of the URN
                //is pointless because the hashToXML's keys are
                //a String (mini-hash).
                if( requiresConversion ) { //Before LimeWire 3.3
                    String miniHash = null;
                    try {
                        miniHash = new String(LimeXMLUtils.hashFile(file));
                    } catch(IOException e) {
                        continue; // oh well.
                    }
                    xml = hashToXML.get(miniHash);
                    // If this was between LimeWire 2.5 and LimeWire 3.3...
                    // and it had some XML..
                    if( xml != null && xml instanceof LimeXMLDocument )
                        doc = (LimeXMLDocument)xml;
                    else // Pre LimeWire 2.5 or no XML stored.
                        doc = constructDocument((String)xml, file);
                } else { // After LimeWire 3.3
                    xml = hashToXML.get(hash);
                    if( xml == null ) // no XML might exist, try and make some
                        doc = constructDocument(null, file);
                    else //it had a doc already.
                        doc = (LimeXMLDocument)xml;
                }
                
                if( doc == null ) // no document, ignore.
                    continue;
                
                // We have a document, add it.
                addReply(hash, doc);
            }
            checkDocuments(fileToHash);
        }
    
        debug("LimeXMLReplyCollection(): returning.");

        write();
    }
    
    /**
     * Creates a LimeXMLDocument from the XML String.
     * If the string is null, it reads the file to create some XML.
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
                    String id3XML =
                    ID3Reader.readDocument(file,onlyID3);
                    String joinedXML = 
                    joinAudioXMLStrings(id3XML, xmlStr);
                    if( joinedXML == null )
                        return ID3Reader.readDocument(file);
                    else 
                        return new LimeXMLDocument(joinedXML);
                }
                else // only id3 data with mp3 files
                    return ID3Reader.readDocument(file);
            }
            catch (SAXException ignored1) { }
            catch (IOException ignored2) { }
            catch (SchemaNotFoundException ignored3) { }
        }
        else { // !audio || (audio && !mp3)
            try {
                if ((xmlStr != null) && (!xmlStr.equals(""))) 
                    return new LimeXMLDocument(xmlStr);
            }
            catch (SAXException ignored1) { }
            catch (IOException ignored2) { }
            catch (SchemaNotFoundException ignored3) { }
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

    /** This method resolves any discrepancy between the identifier in the 
     *  LimeXMLDocument with the actual file name of the file the
     *  LimeXMLDocument refers to.
     */
    private void checkDocuments(Map fileToHash){
        synchronized (fileToHash) {
        // FLOW:
        // 1. get the set of files
        // 2. for each file, see if it has a doc associated with it.
        // 3. if the file has a doc, then see if the id of the doc conflicts
        //    with the actual file name.
        // 1
        Iterator iter = fileToHash.entrySet().iterator();
        if (iter == null)
            return;
        while(iter.hasNext()){
            Map.Entry entry = (Map.Entry)iter.next();
            File file  = (File)entry.getKey();
            URN hash = (URN)entry.getValue();
            LimeXMLDocument doc;
            synchronized(mainMap){
                doc = (LimeXMLDocument)mainMap.get(hash);
            }
            // 2
            if(doc==null)
                continue;

            // 3
            String actualName = null;
            try {
                actualName = file.getCanonicalPath();
            }catch (IOException ioe) {
                synchronized(mainMap){
                    mainMap.remove(hash);
                    //File don't exist - remove meta-data
                }
                //Assert.that(false,"Cannot find actual file name.");
            }
            String identifier = doc.getIdentifier();
            if(!actualName.equalsIgnoreCase(identifier))
                doc.setIdentifier(actualName);
            doc.setXMLUrn(hash);
        }
        }
    }

    /**
     * returns null if there was an exception while creating the
     * MapSerializer
     */
    private MapSerializer initializeMapSerializer(String URI){
        String fname = LimeXMLSchema.getDisplayString(URI)+".sxml";
        LimeXMLProperties props = LimeXMLProperties.instance();
        String path = props.getXMLDocsDir();
        dataFile = new File(path,fname);
        // invalid if directory.
        if( dataFile.isDirectory() )
            return null;
        mainMap = new HashMap();
        try {
            return new MapSerializer(dataFile);
        } catch(IOException e) {
            debug(e);
            return null;
        }
    }

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

    
    public String getSchemaURI(){
        return schemaURI;
    }

    public void addReply(URN hash,LimeXMLDocument replyDoc){
        synchronized(mainMap){
            mainMap.put(hash,replyDoc);
        }
        replyDoc.setXMLUrn(hash);
    }


    void addReplyWithCommit(File f, URN hash, LimeXMLDocument replyDoc) {
        String identifier ="";
        try{
            identifier = f.getCanonicalPath();
        }catch(IOException e){
            //do nothing
        }
        replyDoc.setIdentifier(identifier);
        addReply(hash, replyDoc);
        
        // commit to disk...
        if (audio) {
            try {
                mp3ToDisk(f.getCanonicalPath(), hash, replyDoc);
            } catch(IOException ignored) {}
        } else
            write();
    }

    public int getCount(){
        synchronized(mainMap) {
            return mainMap.size();
        }
    }
    
    /**
     * may return null if the hash is not found
     */
    public LimeXMLDocument getDocForHash(URN hash){
        synchronized(mainMap){
            return (LimeXMLDocument)mainMap.get(hash);
        }
    }

    public List getCollectionList(){
        List replyDocs = new ArrayList();
        synchronized(mainMap){
            replyDocs.addAll(mainMap.values());
        }
        return replyDocs;
    }
        
    /**
     * Returns and empty list if there are not matching documents with
     * that correspond to the same schema as the query.
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
     * @return the older document, which is being replaced. Can be null.
     */
//    public LimeXMLDocument replaceDoc(Object hash, LimeXMLDocument newDoc){
//        LimeXMLDocument oldDoc = null;
//        synchronized(mainMap){
//            oldDoc = (LimeXMLDocument)mainMap.get(hash);
//            mainMap.put(hash,newDoc);
//        }
//        return oldDoc;
//    }

    public boolean removeDoc(URN hash){
        boolean found;
        Object val;
        synchronized(mainMap){
            val = mainMap.remove(hash);
            found = (val != null);
        }
        boolean written = false;
        if(found){
            written = write();
        }
        if(!written && found){//put it back to maintin consistency
            synchronized(mainMap){
                mainMap.put(hash,val);
            }
        }
        
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
    public int mp3ToDisk(String mp3FileName, URN hash, LimeXMLDocument doc) {
        boolean wrote=false;
        int mp3WriteState = -1;

        // see if you need to change a hash for a file due to a write...
        // if so, we need to commit the ID3 data to disk....
        Object[] output = ripMP3XML(mp3FileName, hash, doc);
        if (((Boolean) output[0]).booleanValue()) 
            // now we need to commit ID3 data to disk
            mp3WriteState = commitID3Data(mp3FileName, hash,
                                          (ID3Editor) output[1]);
        
        // write out the mainmap in serial form...
        wrote = write();

        if(!wrote) //writing serialized map failed
            return RW_ERROR;

        return mp3WriteState;//wrote successful, return mp3WriteState
    }

    /**
     * @return A Object[] of size 2.
     *   First object is a boolean indicating whether you should commit this
     *    ID3 to disk
     *   Second is the ID3Editor to use when commiting.
     */
    private Object[] ripMP3XML(String mp3File, URN hash, LimeXMLDocument doc) {
        Object[] retObjs = new Object[2];
        retObjs[0] = Boolean.FALSE;

        if (!LimeXMLUtils.isMP3File(mp3File))
            return retObjs;

        ID3Editor newValues = new ID3Editor();
        String newXML = null;

        try {
            newXML = doc.getXMLStringWithIdentifier();
        } catch(SchemaNotFoundException snfe) {
            return retObjs;
        }       
        newValues.removeID3Tags(newXML);
        
        // Now see if the file already has the same info ...
        ID3Editor existing = new ID3Editor();
        LimeXMLDocument existingDoc = null;
        try {
            existingDoc = ID3Reader.readDocument(new File(mp3File));
        } catch(IOException e) {
            return retObjs;
        }
        String existingXML = null;
        try {
            existingXML = existingDoc.getXMLStringWithIdentifier();
        } catch(SchemaNotFoundException snfe) {
            return retObjs;
        }
        existing.removeID3Tags(existingXML);
        
        // The ID3 tag is the same as the document, don't do anything.
        if( newValues.equals(existing) )
            return retObjs;
        
        // Something will change ... let them know.
        retObjs[0] = Boolean.TRUE;
        retObjs[1] = newValues;

        return retObjs;
    }


    private int commitID3Data(String mp3FileName,
                              URN oldHash,
                              ID3Editor editor) {
        //write to mp3 file...
        int retVal = editor.writeID3DataToDisk(mp3FileName);
        // any error where the file wasn't changed ... 
        if( retVal == FILE_DEFECTIVE ||
            retVal == RW_ERROR ||
            retVal == BAD_ID3 )
            return retVal;
        
        synchronized (mainMap) {
            Object mainValue = mainMap.remove(oldHash);
        }

        //Since the hash of the file has changed, the metadata pertaiing 
        //to other schemas will be lost unless we update those tables
        //with the new hashValue. 
        //NOTE:This is the only time the hash will change-(mp3 and audio)
        metaFileManager.fileChanged(new File(mp3FileName), oldHash, this);
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
