package com.limegroup.gnutella.xml;

import java.util.*;


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
     * Looks up and returns the <tt>LimeXMLReplyCollection</tt> value for the
     * supplied schemaURI key.
     * 
     * @ return the <tt>LimeXMLReplyCollection</tt> for the given schema URI,
     * or <tt>null</tt> if we the requested mapping does not exist
     */
    public LimeXMLReplyCollection getReplyCollection(String schemaURI){
        synchronized(mapper){
            return (LimeXMLReplyCollection)mapper.get(schemaURI);
        }
    }
}
