package com.limegroup.gnutella.xml;

import java.util.HashMap;

/** 
 * Used to map schema URIs to Reply Collections.
 *<p>
 * Uses the singleton pattern.
 * <p> 
 * Used by the RichInfoLoader, which is responsible for creating this mapping
 * 
 * @author Sumeet Thadani
 */

class SchemaReplyCollectionMapper{
    
    private HashMap mapper;
    static SchemaReplyCollectionMapper instance;
    
    //constructor
    public SchemaReplyCollectionMapper(){
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
    
    public void add(String schemaURI, LimeXMLReplyCollection replyCollection){
        //Note: The reply collection that is being passes in 
        // corresponds to all the ReplyDocuments in out repository 
        // that have the same given schema.
        mapper.put(schemaURI,replyCollection);
    }

    public LimeXMLReplyCollection getReplyCollection(String schemaURI){
        LimeXMLReplyCollection replyCollection = 
                           (LimeXMLReplyCollection)mapper.get(schemaURI);
        return replyCollection;
    }

}
