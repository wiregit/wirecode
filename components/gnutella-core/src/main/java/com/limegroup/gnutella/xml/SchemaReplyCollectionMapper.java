package com.limegroup.gnutella.xml;

import com.sun.java.util.collections.*;


/** 
 * Used to map schema URIs to Reply Collections.
 *<p>
 * Uses the singleton pattern.
 * <p> 
 * Used by the RichInfoLoader, which is responsible for creating this mapping
 * 
 * @author Sumeet Thadani
 */
public class SchemaReplyCollectionMapper{
    
    private Map mapper;
    private static SchemaReplyCollectionMapper instance;
    
    //constructor
    private SchemaReplyCollectionMapper() {
        mapper = new HashMap();
    }


    //To enforce the instance pattern
    public static synchronized SchemaReplyCollectionMapper instance(){
        if (instance != null)
            return instance;
        else{
            instance = new SchemaReplyCollectionMapper();
            return instance;
        }
    }
    

    /**
     * Adds the SchemaURI to a HashMap with the replyCollection.
     * <p>
     * Warning/Note:If the schemaURI already corresponds to a ReplyCollection
     * this method will replace thet old reply collection with the new one. 
     * The old collection will be lost!
     */
    public void add(String schemaURI, LimeXMLReplyCollection replyCollection){
        synchronized(mapper){
            mapper.put(schemaURI,replyCollection);        
        }
    }
    
    /**
     * Returns null if the schemaURI does not correspond to any ReplyCollection
     */
    public LimeXMLReplyCollection getReplyCollection(String schemaURI){
        LimeXMLReplyCollection replyCollection;
        synchronized(mapper){
            replyCollection = (LimeXMLReplyCollection)mapper.get(schemaURI);
        }
        return replyCollection;
    }
}
