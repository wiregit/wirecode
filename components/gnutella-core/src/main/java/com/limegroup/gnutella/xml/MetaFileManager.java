package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * This class handles querying shared files with XML data and returning XML data
 * in replies.
 */
public class MetaFileManager extends FileManager {
    
    /**
     * Lock used when loading meta-settings.
     */
    private final Object META_LOCK = new Object();
    
    /**
     * Overrides FileManager.query.
     *
     * Used to search XML information in addition to normal searches.
     */
    public synchronized Response[] query(QueryRequest request) {
        Response[] result = super.query(request);

        if (shouldIncludeXMLInResponse(request)) {
            LimeXMLDocument doc = request.getRichQuery();
            if( doc != null ) {
                Response[] metas = RichQueryHandler.instance().query(doc);
                if (metas != null) // valid query & responses.
                    result = union(result, metas);
            }
        }
        
        return result;
    }
    
    /**
     * Returns whether or not a response to this query should include XML.
     * Currently only includes XML if the request desires it or
     * if the request wants an out of band reply.
     */
    protected boolean shouldIncludeXMLInResponse(QueryRequest qr) {
        return qr.desiresXMLResponses() || 
               qr.desiresOutOfBandReplies();
    }
    
    /**
     * Adds XML to the response.  This assumes that shouldIncludeXMLInResponse
     * was already consulted and returned true.
     *
     * If the FileDesc has no XMLDocuments, this does nothing.
     * If the FileDesc has one XML Document, this sets it as the response doc.
     * If the FileDesc has multiple XML Documents, this does nothing.
     * The reasoning behind not setting the document when there are multiple
     * XML docs is that presumably the query will be a 'rich' query,
     * and we want to include only the schema that was in the query.
     * 
     * @param response the <tt>Response</tt> instance that XML should be 
     *  added to 
     * @param fd the <tt>FileDesc</tt> that provides access to the 
     *   <tt>LimeXMLDocuments</tt> to add to the response
     */
    protected void addXMLToResponse(Response response, FileDesc fd) {
        List docs = fd.getLimeXMLDocuments();
        if( docs.size() == 0 )
            return;
        if( docs.size() == 1 )
            response.setDocument((LimeXMLDocument)docs.get(0));
    }
    
    /**
     * Removes the LimeXMLDocuments associated with the removed
     * FileDesc from the various LimeXMLReplyCollections.
     */
    public FileDesc removeFileIfShared(File f) {
        FileDesc fd = super.removeFileIfShared(f);
        // nothing removed, ignore.
        if( fd == null )
            return null;
            
        SchemaReplyCollectionMapper mapper =
            SchemaReplyCollectionMapper.instance();            
            
        //Get the schema URI of each document and remove from the collection
        // We must remember the schemas and then remove the doc, or we will
        // get a concurrent mod exception because removing the doc also
        // removes it from the FileDesc.
        List xmlDocs = fd.getLimeXMLDocuments();
        List schemas = new LinkedList();
        for(Iterator i = xmlDocs.iterator(); i.hasNext(); )
            schemas.add( ((LimeXMLDocument)i.next()).getSchemaURI() );
        for(Iterator i = schemas.iterator(); i.hasNext(); ) {
            String uri = (String)i.next();
            LimeXMLReplyCollection col = mapper.getReplyCollection(uri);
            if( col != null )
                col.removeDoc( fd );
        }
        return fd;
    }

    /**
     * @modifies this
     * @effects calls addFileIfShared(file), then stores any metadata from the
     *  given XML documents.  metadata may be null if there is no data.  Returns
     *  the value from addFileIfShared.  Returns the value from addFileIfShared.
     *  <b>WARNING: this is a potential security hazard.</b> 
     *
     * @return -1 if the file was not added, otherwise the index of the newly
     * added file.
     */
	public FileDesc addFileIfShared(File file, List metadata) {
        FileDesc fd = super.addFileIfShared(file);
        
        // if not added or no metadata, nothing else to do.
        if( fd == null || metadata == null || metadata.size() == 0 )
            return fd;

        SchemaReplyCollectionMapper mapper =
            SchemaReplyCollectionMapper.instance();

        Assert.that( fd != null, "null fd just added.");
        
        
        // add xml docs as appropriate, one per schema.
        List schemasAddedTo = new LinkedList();
        for(Iterator iter = metadata.iterator(); iter.hasNext(); ) {
            LimeXMLDocument currDoc = (LimeXMLDocument)iter.next();
            String uri = currDoc.getSchemaURI();
            LimeXMLReplyCollection collection = mapper.getReplyCollection(uri);
            if (collection != null && !schemasAddedTo.contains(uri)) {
                schemasAddedTo.add(uri);
                collection.addReplyWithCommit(file, fd, currDoc);
            }
        }
        
        return fd;
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
		RouterService.getCallback().setAnnotateEnabled(false);
        // let FileManager do its work....
        super.loadSettingsBlocking(notifyOnClear);
        if (loadThreadInterrupted())
            return;
        synchronized(META_LOCK){
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
            FileDesc fds[] = super.getAllSharedFileDescriptors();
            for(int i=0; i < len && !loadThreadInterrupted(); i++) {
                //One ReplyCollection per schema
                String s = LimeXMLSchema.getDisplayString(schemas[i]);
                collection = 
                    new LimeXMLReplyCollection(fds, schemas[i], 
                                           s.equalsIgnoreCase("audio"));
                //Note: the collection may have size==0!
                mapper.add(schemas[i],collection);
            }
            //showXMLData();
        }//end of synchronized block
		RouterService.getCallback().setAnnotateEnabled(true);
    }

    /**
     * Creates a new array, the size of which is less than or equal
     * to normals.length + metas.length.
     */
    private Response[] union(Response[] normals, Response[] metas){       
        if(normals == null)
            return metas;
        if(metas == null)
            return normals;
            
            
        // It is important to use a HashSet here so that duplicate
        // responses are not sent.
        // Unfortunately, it is still possible that one Response
        // did not have metadata but the other did, causing two
        // responses for the same file.
            
        Set unionSet = new HashSet();
        for(int i = 0; i < metas.length; i++)
            unionSet.add(metas[i]);
        for(int i = 0; i < normals.length; i++)
            unionSet.add(normals[i]);

        //The set contains all the elements that are the union of the 2 arrays
        Response[] retArray = new Response[unionSet.size()];
        retArray = (Response[])unionSet.toArray(retArray);
        return retArray;
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
     * Used only for showing the current XML data in the system. This method
     * is used only for the purpose of testing. It is not used for anything 
     * else.
     */
     /*
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
    } */

}

        
