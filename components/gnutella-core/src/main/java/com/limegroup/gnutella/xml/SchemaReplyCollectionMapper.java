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
    
    /**
     * Adds the SchemaURI to a HashMap with the replyCollection.
     * If the schemaURI already corresponds to a ReplyCollection, we just 
     * append the list of the new replyCollection to the existing schemaURI
     */
    public void add(String schemaURI, LimeXMLReplyCollection replyCollection){
        LimeXMLReplyCollection l=(LimeXMLReplyCollection)mapper.get(schemaURI);
        if(l==null)
            mapper.put(schemaURI,replyCollection);
        else{//meaning the schemaURI already corresponds to a ReplyCollection
            List newCollectionList = replyCollection.getCollectionList();
            l.appendCollectionList(newCollectionList);
        }
    }
    
    /**
     * Returns null if the schemaURI does not correspond to any ReplyCollection
     */
    public LimeXMLReplyCollection getReplyCollection(String schemaURI){
        LimeXMLReplyCollection replyCollection = 
                           (LimeXMLReplyCollection)mapper.get(schemaURI);
        return replyCollection;
    }

}
