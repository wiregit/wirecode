package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.*;



//imports to make the test code work
import java.util.List;
import com.limegroup.gnutella.util.NameValue;

public class MetaFileManager extends FileManager {
    
    Object metaLocker = new Object();
    boolean initialized = false;
    
    //constructor
    public MetaFileManager(){
        super();
    }


    public synchronized Response[] query(QueryRequest request) {        
        String rich = request.getRichQuery();
        Response[] normals=super.query(request);//normal text query.
        RichQueryHandler richHandler = RichQueryHandler.instance();
        Response[] metas=richHandler.query(rich);
        if (metas == null)// the rich query is malformed OR non-existent
            return normals;
        Response[] result = union(normals,metas);
        return result;
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
        if (Thread.currentThread().isInterrupted())
            return;
        synchronized(metaLocker){
            if (!initialized){//do this only on startup
                SchemaReplyCollectionMapper mapper = 
                      SchemaReplyCollectionMapper.instance();
                //created maper schemaURI --> ReplyCollection
                LimeXMLSchemaRepository schemaRepository = 
                      LimeXMLSchemaRepository.instance();                

                if (Thread.currentThread().isInterrupted())
                    return;

                //now the schemaRepository contains all the schemas.
                String[] schemas = schemaRepository.getAvailableSchemaURIs();
                //we have a list of schemas
                int len = schemas.length;
                LimeXMLReplyCollection collection;  
                for(int i=0;
                    (i<len) && !Thread.currentThread().isInterrupted();
                    i++){
                    //One ReplyCollection per schema
                    String s = LimeXMLSchema.getDisplayString(schemas[i]);
                    if (s.equalsIgnoreCase("audio")){
                        Map nameToFile = getAllMP3FilesRecursive();
                        collection=new LimeXMLReplyCollection
                        (nameToFile,schemas[i]);
                    }
                    else
                        collection = new LimeXMLReplyCollection(schemas[i]);
                    //Note: we only want to add a XMLReplyCollection to the 
                    //mapper if there is some valid data in the ReplyCollection
                    if(collection.getDone())//if we have some valid data
                        mapper.add(schemas[i],collection);
                }
            }//end of if, we may be initialized, may have been interrupted 
            // fell through...
            if (!Thread.currentThread().isInterrupted())
                initialized = true;
            //System.out.println("Sumeet: Printing current xml data");
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
        for(int i =0; i<normals.length; i++)
            unionSet.add(normals[i]);
        for (int j =0; j<metas.length; j++){
            if(metas[j] != null){
                //There will be nulls at the end of metas if meta-data 
                // inconsistent see bad case in RichQueryHandler for details
                unionSet.add(metas[j]);
            }
        }
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
     * have .mp3 extension. and returns a List of files.
     */
    public Map getAllMP3FilesRecursive(){
        SettingsManager man = SettingsManager.instance();
        ArrayList dirs = new ArrayList(Arrays.asList(man.getDirectoriesAsArray()));
        Map map  = new HashMap();
        int k=0;
        while(k < dirs.size()){
            String dir = (String)dirs.get(k);
            k++;
            File currDir = new File(dir);
            //add all subdirectories to dirs
            String[] subFiles = currDir.list();
            int z = subFiles.length;
            for(int j=0;j<z;j++){
                File f = new File(dir,subFiles[j]);
                if(f.isDirectory()){
                    try {
                        String newDir = f.getCanonicalPath();                  
                        dirs.add(newDir);
                    } catch (IOException ignored) {
                        continue;
                    }
                }
            }
            //check files in this dir for .mp3 files.
            File[] files = getSharedFiles(currDir);
            int size = files.length;
            for(int i=0;i<size;i++){
                    String name="";
                    try{
                        name = files[i].getCanonicalPath();
                    }catch(IOException e){
                        continue;
                    }
                    int j = name.lastIndexOf(".");
                    String ext="";
                    if(j>0)
                        ext = name.substring(j);
                    if(ext.equalsIgnoreCase(".mp3"))
                        map.put(name,files[i]);
            }
        }
        return map;
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
            if (collection == null){
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

        
