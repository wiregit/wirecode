package com.limegroup.gnutella.xml;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.NameValue;
import com.limegroup.gnutella.mp3.*;
import com.limegroup.gnutella.*;
import java.io.*;

/**
 *  Stores a schema and a list of Replies corresponding to the 
 *  the corresponding to this schema.
 *  <p>
 *  So when a search comes in, we only have to look at the set of replies
 *  that correspond to the schema of the query.
 * 
 * @author Sumeet Thadani
 */

public class LimeXMLReplyCollection{
    
    private String schemaURI;
    //a list of reply docs in the client machine that correspond to the Schema
    //Note: Each ReplyCollection is written out to 1 physical file on shutdown.
    private HashMap mainMap;
    public boolean audio = false;//package access
    private ID3Editor editor = null;
    private List replyDocs=null;
    private File dataFile = null;//flat file where all data is stored.
    private int count;
    private Hashtable outMap = null;
    private String changedHash = null;
    private MetaFileManager metaFileManager=null;
    private Object mainMapLock = new Object();

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


    public LimeXMLReplyCollection(Map fileToHash, String URI, 
                                  FileManager fm, boolean audio) {
        this.schemaURI = URI;
        this.metaFileManager = (MetaFileManager)fm;
        this.audio = audio;

        // construct a backing store object (for serialization)
        MapSerializer ms = initialize(URI);
        Map hashToXMLStr;

        //if File is invalid, ms== null
        if (ms == null) // create a dummy
            hashToXMLStr = new HashMap();
        else
            hashToXMLStr = ms.getMap();
        
        if (this.audio) // special processing for mp3s needed
            constructAudioCollection(fileToHash, hashToXMLStr);

        // OLD VS. NEW - this code is here because we are changing the
        // representation of the .sxml file for LimeWire 2.5 and on.  Now the
        // hashToXMLStr is actually hashToXMLDoc - but we don't want older
        // clients to lose any annotations, so we'll convert them at the first
        // opportunity.

        // get the hash to xml (from the serialized file) and create a
        // LimeXMLDocument out of each.  Then add it to the collection.
        Iterator iter = null;
        if ((hashToXMLStr != null) &&
            (hashToXMLStr.keySet() != null))
            iter = hashToXMLStr.keySet().iterator();
        while((iter != null) && iter.hasNext()){
            String hash = (String)iter.next();
            Object xml = hashToXMLStr.get(hash); //cannot be null
            LimeXMLDocument doc=null;
            try{
                if (xml instanceof LimeXMLDocument)
                    doc = (LimeXMLDocument) xml;
                else 
                    doc = new LimeXMLDocument((String) xml);
                addReply(hash, doc);
            }
            catch(Exception e){
            }
        }

        if (fileToHash != null)
            checkDocuments(fileToHash,false);
    }


    /**
     * Handle creation of a Audio Collection.  Special because we need to read
     * ID3s, etc.
     */
    private void constructAudioCollection(Map fileToHash, Map hashToXMLStr) {
        ID3Reader id3Reader = new ID3Reader();
        // for each file, get the hash, then get the fileXMLString, and
        // (possiblY) join it with the ID3 info.  Then add the reply.
        synchronized(fileToHash){
            Iterator iter = fileToHash.keySet().iterator();
            LimeXMLDocument doc = null;
            boolean solo = false; //rich data only from ID3?
            while(iter.hasNext()) {
                File file = (File)iter.next();
                String hash = metaFileManager.readFromMap(file, audio);
                Object xml = hashToXMLStr.get(hash);
                try{
                    if (xml instanceof LimeXMLDocument)
                        doc = (LimeXMLDocument) xml;
                    else {
                        String fileXMLString = (String) xml;
                        solo = ((fileXMLString == null) || 
                                fileXMLString.equals(""));
                        String xmlString = id3Reader.readDocument(file,solo);
                        if(!solo)
                            xmlString = 
                            joinAudioXMLStrings(xmlString,fileXMLString);
                        doc = new LimeXMLDocument(xmlString);
                    }
                    addReply(hash,doc);
                }
                catch(Exception e){
                }
            }
        }//end of synch
    }
    

    /**
     * Gets a list of keywords from all the documents in this collection.
     * <p>
     * delegates to the individual documents and collates the list
     */
    protected List getKeyWords(){
        List retList = new ArrayList();
        Iterator docs;
        synchronized(mainMapLock){
            docs = mainMap.values().iterator();
            while(docs.hasNext()){
                LimeXMLDocument d = (LimeXMLDocument)docs.next();
                retList.addAll(d.getKeyWords());
            }
        }
        return retList;
    }

    private void checkDocuments (Map fileToHash, boolean mp3){
        //compare fileNames  from documents in mainMap to 
        //actual filenames as per the map
        synchronized(fileToHash){
            Iterator iter = null;
            if (fileToHash.keySet() != null)
                iter = fileToHash.keySet().iterator();
            if (iter == null)
                return;
            while(iter.hasNext()){
                File file  = (File)iter.next();
                String hash=metaFileManager.readFromMap(file,mp3);
                LimeXMLDocument doc;
                synchronized(mainMapLock){
                    doc = (LimeXMLDocument)mainMap.get(hash);
                }
                if(doc==null)//File in current round has no docs of this schema
                    continue;
                String actualName = null;
                try {
                    actualName = file.getCanonicalPath();
                }catch (IOException ioe) {
                    synchronized(mainMapLock){
                        mainMap.remove(hash);
                        //File don't exist - remove meta-data
                    }
                    //Assert.that(false,"Cannot find actual file name.");
                }
                String identifier = doc.getIdentifier();
                if(!actualName.equalsIgnoreCase(identifier))
                    doc.setIdentifier(actualName);
                //TODO: Commit this to disk if any of the docs was dirty!
                //toDisk("");//write the change out to disk
            }
        }//end of synchronized block
    }

    /**
     * returns null if there was an exception while creating the
     * MapSerializer
     */
    private MapSerializer initialize(String URI){
        String fname = LimeXMLSchema.getDisplayString(URI)+".sxml";
        LimeXMLProperties props = LimeXMLProperties.instance();
        String path = props.getXMLDocsDir();
        dataFile = new File(path,fname);
        mainMap = new HashMap();
        MapSerializer ret = null;
        try{
            ret = new MapSerializer(dataFile);
        }catch(Exception e){
            //System.out.println("Sumeet file is "+fname);
            //e.printStackTrace();
        }
        return ret;
    }
        
    private String joinAudioXMLStrings(String mp3Str, String fileStr){
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
        synchronized(mainMapLock){
            mainMap.put(hash,replyDoc);
        }
        replyDocs=null;//things have changed
        count++;
    }


    public void addReplyWithCommit(File f, 
                                   String hash, 
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
                toDisk("");
        }
        catch (Exception ignored) {}
    }

    public int getCount(){
        return count;
    }
    
    /**
     * may return null if the hash is not found
     */
    public LimeXMLDocument getDocForHash(String hash){
        synchronized(mainMapLock){
            return (LimeXMLDocument)mainMap.get(hash);
        }
    }

    public List getCollectionList(){
        if (replyDocs !=null)
            return replyDocs;
        replyDocs = new ArrayList();
        synchronized(mainMapLock){
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
        synchronized(mainMapLock){
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
        synchronized(mainMapLock){
            oldDoc = (LimeXMLDocument)mainMap.get(hash);
            mainMap.put(hash,newDoc);
        }
        replyDocs=null;//things have changed
        return oldDoc;
    }

    public boolean removeDoc(String hash){
        replyDocs=null;//things will change
        boolean found;
        Object val;
        synchronized(mainMapLock){
            val = mainMap.remove(hash);
            found = val==null?false:true;
        }
        boolean written = false;
        if(found){
            //ID3Editor editor = null;
            written = toDisk("");//no file modified...just del meta
        }
        if(!written && found){//put it back to maintin consistency
            synchronized(mainMapLock){
                mainMap.put(hash,val);
            }
        }
        else if(found && written)
            return true;
        return false;
    }
    
    /**
     * @param modifiedFile is should be "" for non audio data. It is only used
     * if this.audio==true. When it is an audio collection, we use this
     * modified file to commit data to it. 
     */
    public boolean toDisk(String modifiedFile){
        this.replyDocs=null;//things are about to be changed.
        synchronized(mainMapLock){
            Iterator iter = mainMap.keySet().iterator();
            this.outMap = new Hashtable();
            while(iter.hasNext()){
                String hash = (String)iter.next();            
                LimeXMLDocument currDoc=(LimeXMLDocument)mainMap.get(hash);
                String xml = "";
                try {
                    xml = currDoc.getXMLStringWithIdentifier();
                }
                catch (SchemaNotFoundException snfe) {
                    continue;
                }
                if(audio){
                    String fName = currDoc.getIdentifier();
                    ID3Editor e = new ID3Editor();
                    boolean mp3Doc = LimeXMLUtils.isMP3File(fName);
                    if(mp3Doc){
                        //remove the ID3 tags only if doc corresponds to mp3
                        xml = e.removeID3Tags(xml);
                        if(fName.equals(modifiedFile)){
                            //if mp3Doc and our file 
                            this.editor = e;
                            this.changedHash = hash;
                        }
                    }
                }
                outMap.put(hash,xml);
                //System.out.println("Sumeet: outging XML String=\n"+xml);
            }//OK...all the docs have been converted to XML strings
        }
        if(!audio)//if not audio or (audio and nonmp3)
            return write();
        else//go back to mp3ToDisk()//audio and mp3
            return true;
    }
     
    private boolean write(){
        if(dataFile==null){//calculate it
            String fname = LimeXMLSchema.getDisplayString(schemaURI)+".sxml";
            LimeXMLProperties props = LimeXMLProperties.instance();
            String path = props.getXMLDocsDir();
            dataFile = new File(path,fname);
        }        
        try{
            MapSerializer ms = new MapSerializer(dataFile,outMap);
            this.outMap=null;//reset.
            ms.commit();
        }catch (Exception e){
            return false;
        }
        return true;
    }
    
    public int mp3ToDisk(String mp3FileName){
        boolean mp3= LimeXMLUtils.isMP3File(mp3FileName);
        boolean wrote=false;
        int mp3WriteState = -1;
        //remove nonID3 stuff and store in outMap...and write out to disk 
        //in the regular way
        toDisk(mp3FileName);

        if (this.editor != null){//now outMap is populated            
            //write to mp3 file...
            mp3WriteState = this.editor.writeID3DataToDisk(mp3FileName);
            if(mp3WriteState!=NORMAL)
                return mp3WriteState;
            //Note: above operation has changed the hash of the file.
            File file = new File(mp3FileName);
            String newHash= null;
            try{
                newHash = new String(LimeXMLUtils.hashFile(file));
            }catch (Exception e){
                return HASH_FAILED;
            }
            synchronized(mainMapLock){
                Object mainValue = mainMap.remove(changedHash);
                mainMap.put(newHash,mainValue);
            }
            //replace the old hashValue
            metaFileManager.writeToMap(file,newHash,mp3);
            //Since the hash of the file has changed, the metadata pertaiing 
            //to other schemas will be lost unless we update those tables
            //with the new hashValue. 
            //NOTE:This is the only time the hash will change-(mp3 and audio)
            metaFileManager.handleChangedHash(changedHash,newHash,this);
            Object outValue = outMap.remove(changedHash);
            outMap.put(newHash,outValue);
        }        
        wrote = write();
        this.outMap=null;
        this.changedHash = null;
        this.editor= null; //reset the value
        if(!wrote){//writing serialized map failed
            System.out.println("Sumeet not good");
            return RW_ERROR;
        }
        return mp3WriteState;//wrote successful, return mp3WriteState
    }


    public static class MapSerializer {

        /** Where to serialize/deserialize from.
         */
        private File _backingStoreFile;
        
        /** underlying map for hashmap access.
         */
        private Hashtable _hashMap;

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
                _hashMap = new Hashtable();
        }


        /** @param whereToStore The name of the file to serialize from / 
         *  deserialize to.  
         *  @param storage A HashMap that you want to serialize / deserialize.
         *  @exception Exception Thrown if input file whereToStore is invalid.
         */
        public MapSerializer(File whereToStore, Hashtable storage) 
        throws Exception {
            _backingStoreFile = whereToStore;
            _hashMap = storage;
            if (_backingStoreFile.isDirectory())
                throw new Exception();
        }


        private void deserializeFromFile() throws Exception {            
            FileInputStream istream = new FileInputStream(_backingStoreFile);
            ObjectInputStream objStream = new ObjectInputStream(istream);
            _hashMap = (Hashtable) objStream.readObject();
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
            objStream.writeObject(_hashMap);
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
    
    
    public static void main(String argv[]) throws Exception {
        testMapSerializer(argv);
    }
    

}
