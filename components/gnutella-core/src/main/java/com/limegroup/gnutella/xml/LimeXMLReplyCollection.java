package com.limegroup.gnutella.xml;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.NameValue;
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
    /** a list of reply docs in the client machine that correspond to the Schema
     *  Note: Each ReplyCollection is written out to 1 physical file on 
     *  shutdown.
     *  Locking: Never obtain a lock on mainMap PRIOR to obtaining that of
     *  fileToHash (the input hashSet upon construction).
     */
    private HashMap mainMap;
    public boolean audio = false;//package access
    private File dataFile = null;//flat file where all data is stored.
    private MetaFileManager metaFileManager = null;
    /** used for synching when writing...
     */
    private Object writeLock = new Object(); 

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
        Map hashToXMLStr;

        //if File is invalid, ms== null
        if (ms == null) // create a dummy
            hashToXMLStr = new HashMap();
        else 
            hashToXMLStr = ms.getMap();
        
        // OLD VS. NEW - this code is here because we are changing the
        // representation of the .sxml file for LimeWire 2.5 and on.  Now
        // the hashToXMLStr is actually hashToXMLDoc - but we don't want
        // older clients to lose any annotations, so we'll convert them at
        // the first opportunity.
        
        // get the hash to xml (from the serialized file) and create a
        // LimeXMLDocument out of each.=  Then add it to the collection.
        // we assume the hashSet input into the collection is the aggregate of
        // files shared by LimeWire....
        synchronized (fileToHash) {
        Iterator iter = fileToHash.keySet().iterator();
        ID3Reader id3Reader = new ID3Reader();
        while((iter != null) && iter.hasNext()) {
            File file = (File)iter.next();
            //Note: Ordinarily, we should have called mfm.readMap, but since
            //we already have the lock to what that method synchronizes on,
            //its OK to do it in this case.
            String hash = (String) fileToHash.get(file);
            Object xml = hashToXMLStr.get(hash); //lookup in store from disk
            // at this point, xml can be either 1. a LimeXMLDoc, 2. a string 
            // (a xml string), or 3. null
            LimeXMLDocument doc=null;
            if ((xml != null) && xml instanceof LimeXMLDocument)  {// NEW
                // easy, the whole serialized doc was on disk, just reuse it
                doc = (LimeXMLDocument) xml; //done!
            }
            else { // OLD
                String xmlStr = (String) xml; //xml could be null.
                // old style may exist or there may be no xml associated
                // with this file yet.....
                if (audio && LimeXMLUtils.isMP3File(file)) {
                    // first try to get the id3 out of it.  if this file has
                    // no id3 tag, just construct the doc out of the xml 
                    // string....
                    boolean onlyID3=((xmlStr == null) || xmlStr.equals(""));
                    try {
                        if(!onlyID3) {  //non-id3 values with mp3 file
                            try {
                                String id3XML =
                                id3Reader.readDocument(file,onlyID3);
                                String joinedXML = 
                                joinAudioXMLStrings(id3XML, xmlStr);
                                doc = new LimeXMLDocument(joinedXML);
                            }
                            catch (RuntimeException re) {
                                // our join method can be fragile, so there
                                // is no reason to discard xml for this file
                                // just use id3 info...
                                doc = id3Reader.readDocument(file);
                            }
                        }
                        else // only id3 data with mp3 files
                            doc = id3Reader.readDocument(file);
                    }
                    catch (SAXException ignored1) { continue; }
                    catch (IOException ignored2) { continue; }
                    catch (SchemaNotFoundException ignored3) { continue; }
                }
                else { // !audio || (audio && !mp3)
                    try {
                        if ((xmlStr != null) && (!xmlStr.equals(""))) 
                            doc = new LimeXMLDocument(xmlStr);
                        else
                            continue;
                    }
                    catch (SAXException ignored1) { continue; }
                    catch (IOException ignored2) { continue; }
                    catch (SchemaNotFoundException ignored3) { continue; }
                }
            }
            // if i've gotten this far, the doc is non-null and should be added.
            addReply(hash, doc);
        }
        checkDocuments(fileToHash, false);
        }
    
        debug("LimeXMLReplyCollection(): returning.");

        write();
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
    private void checkDocuments(Map fileToHash, boolean mp3){
        synchronized (fileToHash) {
        // FLOW:
        // 1. get the set of files
        // 2. for each file, see if it has a doc associated with it.
        // 3. if the file has a doc, then see if the id of the doc conflicts
        //    with the actual file name.
        // 1
        Iterator iter = fileToHash.keySet().iterator();
        if (iter == null)
            return;
        while(iter.hasNext()){
            File file  = (File)iter.next();
            String hash = (String) fileToHash.get(file);
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
        mainMap = new HashMap();
        MapSerializer ret = null;
        try{
            ret = new MapSerializer(dataFile);
        }catch(Exception e){
            debug(e);
        }
        return ret;
    }
        
    private String joinAudioXMLStrings(String mp3Str, String fileStr) 
    throws RuntimeException {
        int p = fileStr.lastIndexOf("></audio>");
        //above line is the one closing the root element
        String a = fileStr.substring(0,p);//all but the closing part
        String b = fileStr.substring(p);//closing part
        //phew, thank god this schema has depth 1.
        return(a+mp3Str+b);
    }

    
    public String getSchemaURI(){
        return schemaURI;
    }

    public void addReply(String hash,LimeXMLDocument replyDoc){
        synchronized(mainMap){
            mainMap.put(hash,replyDoc);
        }
    }


    void addReplyWithCommit(File f, String hash, 
                                   LimeXMLDocument replyDoc) {
        String identifier ="";
        try{
            identifier = f.getCanonicalPath();
        }catch(IOException e){
            //do nothing
        }
        replyDoc.setIdentifier(identifier);
        addReply(hash, replyDoc);
        // commit to disk...
        try {
            if (audio)
                mp3ToDisk(f.getCanonicalPath());
            else
                write();
        }
        catch (Exception ignored) {}
    }

    public int getCount(){
        synchronized(mainMap) {
            return mainMap.size();
        }
    }
    
    /**
     * may return null if the hash is not found
     */
    public LimeXMLDocument getDocForHash(String hash){
        synchronized(mainMap){
            return (LimeXMLDocument)mainMap.get(hash);
        }
    }

    public List getCollectionList(){
        List replyDocs = new ArrayList();
        synchronized(mainMap){
            Iterator iter = mainMap.keySet().iterator();
            while(iter.hasNext()){
                Object hash = iter.next();
                Object doc = mainMap.get(hash);
                replyDocs.add(doc);
            }
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
            Iterator iter = mainMap.keySet().iterator();
            matchingReplyDocs = new ArrayList();
            while(iter.hasNext()){
                Object hash = iter.next();
                LimeXMLDocument currReplyDoc=
                (LimeXMLDocument)mainMap.get(hash); 
                boolean match = LimeXMLUtils.match(currReplyDoc, queryDoc);
                if(match){
                    matchingReplyDocs.add(currReplyDoc);
                    match = false;//reset
                }
            }
        }
        return matchingReplyDocs;
    }

    
    /**
     * @return the older document, which is being replaced. Can be null.
     */
    public LimeXMLDocument replaceDoc(Object hash, LimeXMLDocument newDoc){
        LimeXMLDocument oldDoc = null;
        synchronized(mainMap){
            oldDoc = (LimeXMLDocument)mainMap.get(hash);
            mainMap.put(hash,newDoc);
        }
        return oldDoc;
    }

    public boolean removeDoc(String hash){
        boolean found;
        Object val;
        synchronized(mainMap){
            val = mainMap.remove(hash);
            found = val==null?false:true;
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
        else if(found && written)
            return true;
        return false;
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
        try{
            MapSerializer ms = new MapSerializer(dataFile, mainMap);
            synchronized (writeLock) {
                ms.commit();
            }
        }catch (Exception e){
            return false;
        }
        return true;
    }
    
    public int mp3ToDisk(String mp3FileName){
        boolean wrote=false;
        int mp3WriteState = -1;

        // see if you need to change a hash for a file due to a write...
        // if so, we need to commit the ID3 data to disk....
        Object[] output = ripMP3XML(mp3FileName);
        if (((Boolean) output[0]).booleanValue()) 
            // now we need to commit ID3 data to disk
            mp3WriteState = commitID3Data(mp3FileName,
                                          (String) output[1],
                                          (ID3Editor) output[2]);
        
        // write out the mainmap in serial form...
        wrote = write();

        if(!wrote) //writing serialized map failed
            return RW_ERROR;

        return mp3WriteState;//wrote successful, return mp3WriteState
    }

    /** @return A Object[] of size 3.  First object is a boolean indicated
    whether you should commit this ID3 to disk, second is the new hash (String)
    of the file, and third is the ID3Editor to use....
     */
    private Object[] ripMP3XML(String modifiedFile) {
        Object[] retObjs = new Object[3];
        retObjs[0] = new Boolean(false);
        if (!LimeXMLUtils.isMP3File(modifiedFile))
            return retObjs;
        try {
            String hash = 
            new String(LimeXMLUtils.hashFile(new File(modifiedFile)));
            LimeXMLDocument doc = null;
            synchronized (mainMap) {
                doc = (LimeXMLDocument) mainMap.get(hash);
            }
            String fName = doc.getIdentifier();
            if (LimeXMLUtils.isMP3File(modifiedFile)) {
                ID3Editor e = new ID3Editor();
                String xml = doc.getXMLStringWithIdentifier();
                e.removeID3Tags(xml);
                retObjs[0] = new Boolean(true);
                retObjs[1] = hash;
                retObjs[2] = e;
            }
            return retObjs;
        }
        catch (Exception e) {
            return retObjs;
        }
    }


    private int commitID3Data(String mp3FileName,
                              String changedHash,
                              ID3Editor editor) {
        //write to mp3 file...
        int retVal = editor.writeID3DataToDisk(mp3FileName);
        if (retVal != NORMAL)
            return retVal;
        
        //Note: above operation has changed the hash of the file.
        File file = new File(mp3FileName);
        String newHash= null;
        try {
            newHash = new String(LimeXMLUtils.hashFile(file));
        }
        catch (Exception e){
            retVal = HASH_FAILED;
            return retVal;
        }
        
        synchronized (mainMap) {
            Object mainValue = mainMap.remove(changedHash);
            mainMap.put(newHash, mainValue);
        }
        //replace the old hashValue
        metaFileManager.writeToMap(file, newHash);
        //Since the hash of the file has changed, the metadata pertaiing 
        //to other schemas will be lost unless we update those tables
        //with the new hashValue. 
        //NOTE:This is the only time the hash will change-(mp3 and audio)
        metaFileManager.handleChangedHash(changedHash, newHash, this);
        return retVal;
    }


    public static class MapSerializer {

        /** Where to serialize/deserialize from.
         */
        private File _backingStoreFile;
        
        /** underlying map for hashmap access.
         */
        private Map _hashMap;

        /** @param whereToStore The name of the file to serialize from / 
         *  deserialize to.  
         *  @exception Exception Thrown if input file whereToStore is invalid.
         */
        public MapSerializer(File whereToStore) throws Exception {
            _backingStoreFile = whereToStore;
            if (_backingStoreFile.isDirectory())
                throw new Exception();
            else if (_backingStoreFile.exists())
                deserializeFromFile();
            else
                _hashMap = new HashMap();
        }


        /** @param whereToStore The name of the file to serialize from / 
         *  deserialize to.  
         *  @param storage A HashMap that you want to serialize / deserialize.
         *  @exception Exception Thrown if input file whereToStore is invalid.
         */
        public MapSerializer(File whereToStore, Map storage) 
        throws Exception {
            _backingStoreFile = whereToStore;
            _hashMap = storage;
            if (_backingStoreFile.isDirectory())
                throw new Exception();
        }


        private void deserializeFromFile() throws Exception {            
            FileInputStream istream = new FileInputStream(_backingStoreFile);
            ObjectInputStream objStream = new ObjectInputStream(istream);
            _hashMap = (Map) objStream.readObject();
            istream.close();
        }

        /** Call this method when you want to force the contents to the HashMap
         *  to disk.
         *  @exception Exception Thrown if force to disk failed.
         */
        public void commit() throws Exception {
            serializeToFile();
        }

        
        private void serializeToFile() throws Exception {
            FileOutputStream ostream = new FileOutputStream(_backingStoreFile);
            ObjectOutputStream objStream = new ObjectOutputStream(ostream);
            synchronized (_hashMap) {
                objStream.writeObject(_hashMap);
            }
            ostream.close();
        }

        /** @return The Map this class encases.
         */
        public Map getMap() {
            return _hashMap;
        }

    }


    
    public static void testMapSerializer(String argv[]) throws Exception {   
        LimeXMLReplyCollection.MapSerializer hms =
        new LimeXMLReplyCollection.MapSerializer(new File(argv[0]));
        
        Map hm = hms.getMap();
        
        System.out.println(""+hm);
        
        for (int i = 1; i < argv.length; i+=2) {
            try{
                hm.put(argv[i],argv[i+1]);
            }
            catch (Exception e) {};
        }
        hms.commit();
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

    
    
    public static void main(String argv[]) throws Exception {
        testMapSerializer(argv);
    }
    

}
