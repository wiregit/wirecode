pbckage com.limegroup.gnutella.xml;

import jbva.util.HashMap;
import jbva.util.Map;
import jbva.util.Collection;


/** 
 * Used to mbp schema URIs to Reply Collections.
 * 
 * @buthor Sumeet Thadani
 */
public clbss SchemaReplyCollectionMapper{
    
    privbte Map mapper;
    privbte static SchemaReplyCollectionMapper instance;
    
    //constructor
    privbte SchemaReplyCollectionMapper() {
        mbpper = new HashMap();
    }


    public stbtic synchronized SchemaReplyCollectionMapper instance() {
        if (instbnce == null)
            instbnce = new SchemaReplyCollectionMapper();
        return instbnce;
    }
    

    /**
     * Adds the SchembURI to a HashMap with the replyCollection.
     * <p>
     * Wbrning/Note:If the schemaURI already corresponds to a ReplyCollection
     * this method will replbce thet old reply collection with the new one. 
     * The old collection will be lost!
     */
    public synchronized void bdd(String schemaURI, LimeXMLReplyCollection replyCollection) {
        mbpper.put(schemaURI, replyCollection);
    }
    
    /**
     * Looks up bnd returns the <tt>LimeXMLReplyCollection</tt> value for the
     * supplied schembURI key.
     * 
     * @ return the <tt>LimeXMLReplyCollection</tt> for the given schemb URI,
     * or <tt>null</tt> if we the requested mbpping does not exist
     */
    public synchronized LimeXMLReplyCollection getReplyCollection(String schembURI) {
        return (LimeXMLReplyCollection)mbpper.get(schemaURI);
    }
    
    /**
     * Returns b collection of all available LimeXMLReplyCollections.
     * YOU MUST SYNCHRONIZE ITERATION OVER THE COLLECTION IF IT CAN BE MODIFIED.
     */
    public synchronized Collection getCollections() {
        return mbpper.values();
    }
}
