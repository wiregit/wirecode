package com.limegroup.gnutella.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/** 
 * Used to map schema URIs to Reply Collections.
 * 
 * @author Sumeet Thadani
 */
public class SchemaReplyCollectionMapper{
    
    private Map<String, LimeXMLReplyCollection> mapper;
    private static SchemaReplyCollectionMapper instance;
    
    //constructor
    private SchemaReplyCollectionMapper() {
        mapper = new HashMap<String, LimeXMLReplyCollection>();
    }


    public static synchronized SchemaReplyCollectionMapper instance() {
        if (instance == null)
            instance = new SchemaReplyCollectionMapper();
        return instance;
    }
    

    /**
     * Adds the SchemaURI to a HashMap with the replyCollection.
     * <p>
     * Warning/Note:If the schemaURI already corresponds to a ReplyCollection
     * this method will replace thet old reply collection with the new one. 
     * The old collection will be lost!
     */
    public synchronized void add(String schemaURI, LimeXMLReplyCollection replyCollection) {
        mapper.put(schemaURI, replyCollection);
    }
    
    /**
     * Looks up and returns the <tt>LimeXMLReplyCollection</tt> value for the
     * supplied schemaURI key.
     * 
     * @ return the <tt>LimeXMLReplyCollection</tt> for the given schema URI,
     * or <tt>null</tt> if we the requested mapping does not exist
     */
    public synchronized LimeXMLReplyCollection getReplyCollection(String schemaURI) {
        return mapper.get(schemaURI);
    }
    
    /**
     * Returns a collection of all available LimeXMLReplyCollections.
     * YOU MUST SYNCHRONIZE ITERATION OVER THE COLLECTION IF IT CAN BE MODIFIED.
     */
    public synchronized Collection<LimeXMLReplyCollection> getCollections() {
        return mapper.values();
    }
}
