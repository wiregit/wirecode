package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.QueryRequest;
import com.limegroup.gnutella.FileDesc;
import java.util.HashSet;
import java.util.Iterator;


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
    
    public FileDesc file2index(String fullName) {  
        // TODO1: precompute and store in table.
        for (int i=0; i<_files.size(); i++) {
            FileDesc fd=(FileDesc)_files.get(i);
            if (fd==null)  file://unshared
            continue;
            else if (fd._path.equals(fullName))
                return fd;
        }
        return null;//The file with this name was not found.
    }
    
    /**This method now breaks the contract of the super class. The super class
     * claims that this method is non-blocking. Because the the loadThread 
     * would asynchronously do the loading.
     * <p>
     * But now, only the regular file stuff is passed off to the over-riden
     * method in the super class. 
     * <p>
     * The meta-information loading is done within this method. That makes
     * this method a blocking method. This is a quick fix for now. because we
     * are interested in loading the meta-database only one time  at startup
     * so the fact thats it is blocking is not so bad. 
     * <p>
     * TODO2: Eventually we will think that its too much of a burden
     * to have this thread be blocking in which case we will have to 
     * have the load thread also handle the reloading of the meta-data.
     * Question: Do we really want to reload the meta-data whenever a we
     * want to update the file information?? It depends on how we want to 
     * handle the meta-data and its relation to the file system
     */
    public void loadSettings(boolean notifyOnClear){
        super.loadSettings(notifyOnClear);//it has its own synchronization
        synchronized(metaLocker){
            if (!initialized){//do this only on startup
                SchemaReplyCollectionMapper mapper = 
                      SchemaReplyCollectionMapper.instance();
                //created maper schemaURI --> ReplyCollection
                LimeXMLSchemaRepository schemaRepository = 
                      LimeXMLSchemaRepository.instance();                
                //now the schemaRepository contains all the schemas.
                String[] schemas = schemaRepository.getAvailableSchemaURIs();
                //we have a list of schemas
                int len = schemas.length;
                for(int i=0;i<len;i++){
                    LimeXMLReplyCollection collection =  
                       new LimeXMLReplyCollection(schemas[i]);
                    //One ReplyCollection per schema
                    //Note: we only want to add a XMLReplyCollection to the 
                    //mapper if there is some valid data in the ReplyCollection
                    if(collection.getDone())//if we have some valid data
                        mapper.add(schemas[i],collection);
                }
            }//end of if
            initialized = true;
        }//end of synchronized block
    }//end of loadSettings.

    private Response[] union(Response[] normals, Response[] metas){       
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

}
        
