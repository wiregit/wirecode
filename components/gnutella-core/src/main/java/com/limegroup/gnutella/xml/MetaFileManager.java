package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.*;
import java.io.*;
import com.sun.java.util.collections.*;

//imports to make the test code work
import com.limegroup.gnutella.util.NameValue;

public class MetaFileManager extends FileManager {
    
    Object metaLocker = new Object();
    boolean initialized = false;
    
    /**
     * keeps a hash of files -> hash of files (file -> string)
     * <p>
     * <b>You must use the synchronized method in this class to read
     *  or write from this map
     * </b>
     */
    private Map fileToHash = new HashMap();

    //constructor
    public MetaFileManager(){
        super();
    }

    public synchronized Response[] query(QueryRequest request) {        
        String rich = request.getRichQuery();
        Response[] normals=super.query(request);//normal text query.
        addAudioMetadata(normals);
        RichQueryHandler richHandler = RichQueryHandler.instance();
        Response[] metas=richHandler.query(rich,this);
        if (metas == null)// the rich query is malformed OR non-existent
            return normals;
        Response[] result = union(normals,metas);
        return result;
    }

    /**
     * @modifies this
     * @effects calls addFileIfShared(file), then stores any metadata from the
     *  given XML documents.  metadata may be null if there is no data.  Returns
     *  the value from addFileIfShared.  Returns the value from addFileIfShared.
     *  <b>WARNING: this is a potential security hazard.</b> 
     */
	public synchronized boolean addFileIfShared(File file,
                                                LimeXMLDocument[] metadata) {
        boolean added=super.addFileIfShared(file, metadata);
        if (added && metadata!=null) {
            SchemaReplyCollectionMapper mapper =
                SchemaReplyCollectionMapper.instance();

            // compute the hash of the file.  integral for proceeding.
            String hash;
            try {
                hash = new String(LimeXMLUtils.hashFile(file));
                if (hash == null)
                    throw new Exception();
                // add to the file manager
                writeToMap(file, hash);
            }
            catch (Exception hashFailed) {
                //TODO: get rid of blanket catch.  Problem is with hashFile.
                return true;  //file added but not metadata!
            }
            
            // add xml docs as appropriate
            for (int i = 0;
                 (metadata != null) && (i < metadata.length);
                 i++) {
                try {
                    LimeXMLDocument currDoc = metadata[i];
                    String uri = currDoc.getSchemaURI();
                    LimeXMLReplyCollection collection =
                    mapper.getReplyCollection(uri);
                    
                    if (collection != null)
                        collection.addReplyWithCommit(file, hash, currDoc);
                }
                catch (Exception ignored) {
                    //TODO: get rid of blanket catch.
                }
            }
        }
        return added;
    }


    private void addAudioMetadata(Response[] responses){
        if (responses == null)//responses may be null
            return;
        String audioURI = "http://www.limewire.com/schemas/audio.xsd";
        SchemaReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        LimeXMLReplyCollection coll = map.getReplyCollection(audioURI);
        if(coll == null)//if there schemas are not loaded
            return;
        int z = responses.length;
        for(int i=0;i<z;i++){
            FileDesc f = get((int)responses[i].getIndex());
            File file = new File(f._path);
            String hash=readFromMap(file);
            if(hash==null)//not an mp3 file
                hash = readFromMap(file);
            LimeXMLDocument doc = coll.getDocForHash(hash);
            if(doc==null)
                continue;
            //System.out.println("Sumeet audioXML="+XMLString);
            responses[i].setDocument(doc);            
        }
    }
    
    /**
     * The rule is that to either read or write to/from this
     * map you have to obtain a lock on it
     */    
    public String readFromMap(Object file){
        String hash = null;
        synchronized (fileToHash) {
            hash = (String) fileToHash.get(file);
        }
        return hash;
    }

    
    public void writeToMap(Object file, Object hash) {
        synchronized (fileToHash) {
            fileToHash.put(file, hash);
        }
    }


    /**
     * Looks at the  LimeXMlReplyCollections other than the one passed as
     * a parameter, and replaces the old hashValue with the new one.
     * <p>
     * package access, since this method is only called from 
     * LimeXMLReplyCollection. Further the caller is always the 
     * audio LimeXMLReplyCollectin and, even further, it is only called
     * when the file being edited is an mp3 file
     */
    void handleChangedHash(String oldHash, String newHash, 
                                      LimeXMLReplyCollection collection){
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();
        SchemaReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        String[] schemas = rep.getAvailableSchemaURIs();
        int l = schemas.length;
        for(int i=0;i<l;i++){
            LimeXMLReplyCollection coll = map.getReplyCollection(schemas[i]);
            if(coll!=collection){//only look at other collections
                LimeXMLDocument d=coll.getDocForHash(oldHash);
                if(d!=null){//we have a value...must replace
                    coll.removeDoc(oldHash);
                    coll.addReply(newHash,d);
                    coll.write();
                }//affected collection done
            }
        }
    }


    /**This method overrides FileManager.loadSettingsBlocking(), though
     * it calls the super method to load up the shared file DB.  Then, it
     * processes these files and annotates them automatically as apropos.
     * TODO2: Eventually we will think that its too much of a burden
     * to have this thread be blocking in which case we will have to 
     * have the load thread also handle the reloading of the meta-data.
     * Question: Do we really want to reload the meta-data whenever a we
     * want to update the file information?? It depends on how we want to 
     * handle the meta-data and its relation to the file system
     */
    protected void loadSettingsBlocking(boolean notifyOnClear){
        // let FileManager do its work....
        super.loadSettingsBlocking(notifyOnClear);
        if (loadThreadInterrupted())
            return;
        synchronized(metaLocker){
            if (!initialized){//do this only on startup
                //clear out the HashMap, don't want to have old and potentially
                //unshared hashes around anymore.
                fileToHash.clear();
                // now recreate the hashes
                createFileToHashMaps();
                SchemaReplyCollectionMapper mapper = 
                      SchemaReplyCollectionMapper.instance();
                //created maper schemaURI --> ReplyCollection
                LimeXMLSchemaRepository schemaRepository = 
                      LimeXMLSchemaRepository.instance();                

                if (loadThreadInterrupted())
                    return;

                //now the schemaRepository contains all the schemas.
                String[] schemas = schemaRepository.getAvailableSchemaURIs();
                //we have a list of schemas
                int len = schemas.length;
                LimeXMLReplyCollection collection;  
                for(int i=0;
                    (i<len) && !loadThreadInterrupted();
                    i++){
                    //One ReplyCollection per schema
                    String s = LimeXMLSchema.getDisplayString(schemas[i]);
                    collection = 
                    new LimeXMLReplyCollection(fileToHash, schemas[i], this, 
                                               s.equalsIgnoreCase("audio"));
                    //Note: the collection may have size==0!
                    mapper.add(schemas[i],collection);
                }                
            }//end of if, we may be initialized, may have been interrupted 
            // fell through...
            /* We never set it to true.
              if (!loadThreadInterrupted())
              initialized = true;
            */
            //showXMLData();
        }//end of synchronized block
    }//end of loadSettings.

    private Response[] union(Response[] normals, Response[] metas){       
        if(normals == null)
            return metas;
        if(metas==null)
            return normals;
        //So they are both not null
        HashSet unionSet = new HashSet();
        for (int j =0; j<metas.length; j++){
            if(metas[j] != null){
                //There will be nulls at the end of metas if meta-data 
                // inconsistent see bad case in RichQueryHandler for details
                unionSet.add(metas[j]);
            }
        }
        for(int i =0; i<normals.length; i++)
            unionSet.add(normals[i]);
        //The set contains all the elements that are the union of the 2 arrays
        int size = unionSet.size();
        Iterator iter = unionSet.iterator();
        unionSet = null;//clear the memory
        Response[] retArray = new Response[size];
        for (int k =0; k< size; k++){
            Response r = (Response)iter.next();
            retArray[k]=r;
        }
        return retArray;
    }

    /**
     * Scans all the shared directories recursively and finds files that
     * have .mp3 extension, and adds them to a hashmap keyed by hashes.
     * <p> 
     * Also creates another map that stores the hash to File of non mp3 files
     */
    private void createFileToHashMaps(){
        SettingsManager man = SettingsManager.instance();
        //ArrayList dirs = new 
		//            ArrayList(Arrays.asList(man.getDirectoriesAsArray()));
        ArrayList dirs = new 
                      ArrayList(Arrays.asList(man.getDirectories()));

        int k=0;
        while(k<dirs.size() && !loadThreadInterrupted()) {
            //String dir = (String)dirs.get(k);
            //File currDir = new File(dir);
            File currDir = (File)dirs.get(k);
            k++;
            //add all subdirectories to dirs
            String[] subFiles = currDir.list();
            int z = subFiles.length;
            for(int j=0;j<z;j++){
                File f = new File(currDir,subFiles[j]);
                if(f.isDirectory())
                    dirs.add(f);
            }
            //check files in this dir for .mp3 files.
            File[] files = getSharedFiles(currDir);
            int size = 0;
            if (files != null)
                size = files.length;
            for(int i=0;i<size && !loadThreadInterrupted();i++){
                    String name="";
                    String hash="";
                    try{
                        name = files[i].getCanonicalPath();
                        hash = new String(LimeXMLUtils.hashFile(files[i]));
                    }catch(Exception e){
                        continue;
                    }
                    writeToMap(files[i],hash);
            }
        }
    }


    /**
     * Returns a list of all the words in the annotations - leaves out
     * numbers. The list also includes the set of words that is contained
     * in the names of the files.
     */
    public List getKeyWords(){
        List words = super.getKeyWords();
        if (words == null)
            words = new ArrayList();
        //Now get a list of keywords from each of the ReplyCollections
        SchemaReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();
        String[] schemas = rep.getAvailableSchemaURIs();
        LimeXMLReplyCollection collection;
        int len = schemas.length;
        for(int i=0;i<len;i++){
            collection = map.getReplyCollection(schemas[i]);
            if(collection==null)//not loaded? skip it and keep goin'
                continue;
            words.addAll(collection.getKeyWords());
        }
        return words;
    }
    

    /** @return A List of KeyWords from the FS that one does NOT want broken
     *  upon hashing into a QRT.  Initially being used for schema uri hashing.
     */
    public List getIndivisibleKeyWords() {
        List words = super.getIndivisibleKeyWords();
        if (words == null)
            words = new ArrayList();
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();
        String[] schemas = rep.getAvailableSchemaURIs();
        for (int i = 0; i < schemas.length; i++) 
            if (schemas[i] != null)
                words.add(schemas[i]);        
        return words;
    }
    
    /**
     * returns the document corresponding to the schame and the file
     * passed as parametere. 
     * <p>
     * Returns null if the document is not found in the schema
     */
    public LimeXMLDocument getDocument(String schemaURI, File f){
        String hash = null;
        hash = readFromMap(f);//try mp3 first
        if(hash == null){//not mp3...try non mp3
            //System.out.println("Sumeet hashNot found with mp3");
            hash = readFromMap(f);
        }
        if (hash==null){//still null? return null
            //System.out.println("Sumeet hashNot found...returning");
            return null;
        }
        //OK we have the hash now
        SchemaReplyCollectionMapper map=SchemaReplyCollectionMapper.instance();
        LimeXMLReplyCollection coll = map.getReplyCollection(schemaURI);
        if(coll==null){//lets be defensive
            //System.out.println("Collection is null...returning");
            return null;
        }
        return coll.getDocForHash(hash);
    }


    /**
     * Used only for showing the current XML data in the system. This method
     * is used only for the purpose of testing. It is not used for anything 
     * else.
     */
    private void showXMLData(){
        //get all the schemas
        LimeXMLSchemaRepository rep = LimeXMLSchemaRepository.instance();
        String[] schemas = rep.getAvailableSchemaURIs();
        SchemaReplyCollectionMapper mapper = 
                SchemaReplyCollectionMapper.instance();
        int len = schemas.length;
        LimeXMLReplyCollection collection;
        for(int i=0; i<len; i++){
            System.out.println("Schema : " + schemas[i]);
            System.out.println("-----------------------");
            collection = mapper.getReplyCollection(schemas[i]);
            if (collection == null || collection.getCount()<1){
                System.out.println("No docs corresponding to this schema ");
                continue;
            }
            List replies = collection.getCollectionList();
            int size = replies.size();
            for(int j=0; j< size; j++){
                System.out.println("Doc number "+j);
                System.out.println("-----------------------");
                LimeXMLDocument doc = (LimeXMLDocument)replies.get(j);
                List elements = doc.getNameValueList();
                int t = elements.size();
                for(int k=0; k<t; k++){
                    NameValue nameValue = (NameValue)elements.get(k);
                    System.out.println("Name " + nameValue.getName());
                    System.out.println("Value " + nameValue.getValue());
                
                }
            }
        }
    }

}

        
