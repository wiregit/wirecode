package com.limegroup.gnutella.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.IncompleteFileDesc;


/** 
 * Used to map schema URIs to Reply Collections.
 * 
 * @author Sumeet Thadani
 */
@Singleton
public class SchemaReplyCollectionMapper implements FileEventListener {
    
    private Map<String, LimeXMLReplyCollection> mapper;
    
    protected final LimeXMLReplyCollectionFactory limeXMLReplyCollectionFactory;
    protected final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;
    

    @Inject
    private SchemaReplyCollectionMapper(LimeXMLReplyCollectionFactory limeXMLReplyCollectionFactory,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.limeXMLReplyCollectionFactory = limeXMLReplyCollectionFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        
        mapper = new HashMap<String, LimeXMLReplyCollection>();
    }


    /**
     * Adds the SchemaURI to a HashMap with the replyCollection.
     * <p>
     * Warning/Note:If the schemaURI already corresponds to a ReplyCollection
     * this method will replace the old reply collection with the new one. 
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

    /**
     * Listen to events from FileManager
     */
    public void handleFileEvent(FileManagerEvent evt) {
        switch(evt.getType()) {
            case FILEMANAGER_LOAD_STARTED:
                loadSchemas();
                break;
            case FILEMANAGER_LOAD_FINISHING:
                finishLoading();
                break;
            case FILEMANAGER_SAVE:
                save(evt);
                break;
            case LOAD_FILE:
                load(evt);
                break;
            case REMOVE_FD:
                removeFileDesc(evt.getFileDescs()[0]);
                break;
        }
    }
    
    private synchronized void removeFileDesc(FileDesc fd) {
        if(fd instanceof IncompleteFileDesc)
            return;
        // Get the schema URI of each document and remove from the collection
        // We must remember the schemas and then remove the doc, or we will
        // get a concurrent mod exception because removing the doc also
        // removes it from the FileDesc.
        List<LimeXMLDocument> xmlDocs = fd.getLimeXMLDocuments();
        List<String> schemas = new LinkedList<String>();
        for (LimeXMLDocument doc : xmlDocs)
            schemas.add(doc.getSchemaURI());
        for (String uri : schemas) {
            LimeXMLReplyCollection col = getReplyCollection(uri);
            if (col != null)
                col.removeDoc(fd);
        }
    }
    
    /**
     * Notifies all the LimeXMLReplyCollections that the initial loading
     * has completed.
     */
    private synchronized void finishLoading() {
        Collection<LimeXMLReplyCollection> replies = getCollections();
        for (LimeXMLReplyCollection col : replies)
            col.loadFinished();
    }
    
    /**
     * Serializes the current LimeXMLReplyCollection to disk.
     */
    private void save(FileManagerEvent evt) {
        if (evt.getFileManager().isLoadFinished()) {
            synchronized (this) {
                Collection<LimeXMLReplyCollection> replies = getCollections();
                for (LimeXMLReplyCollection col : replies)
                    col.writeMapToDisk();                
            }
        }
    }
    
    /**
     * Loads the map with the LimeXMLDocument for a given FileDesc. If no LimeXMLDocument
     * exists for the FileDesc, one is created for it.
     */
    private synchronized void load(FileManagerEvent evt) {
        Collection<LimeXMLReplyCollection> replies = getCollections();
        for (LimeXMLReplyCollection col : replies)
            col.initialize(evt.getFileDescs()[0], evt.getMetaData());
        for (LimeXMLReplyCollection col : replies)
            col.createIfNecessary(evt.getFileDescs()[0]);
    }
    
    /**
     * Loads all the SchemaURI to a HashMap with the replyCollection. 
     */
    private void loadSchemas() {
        String[] schemas = limeXMLSchemaRepository.get().getAvailableSchemaURIs();
        for (int i = 0; i < schemas.length; i++) {
            add(schemas[i], 
                    limeXMLReplyCollectionFactory.createLimeXMLReplyCollection(schemas[i]));
        }
    }
}
