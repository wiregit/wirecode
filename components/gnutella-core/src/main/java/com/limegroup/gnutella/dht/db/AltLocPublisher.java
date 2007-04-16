package com.limegroup.gnutella.dht.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueEntityPublisher;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.DatabaseUtils;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * The AltLocPublisher publishes the localhost as an alternate 
 * locations for rare files. Rare files are files that haven't 
 * been uploaded for a certain amount of time. There are various
 * other ways to determinate the rareness of a file like using
 * the number of query hits instead of upload attempts or even
 * keeping track of the file activities over multiple sessions.
 */
public class AltLocPublisher implements DHTValueEntityPublisher {
    
    private final MojitoDHT dht;
    
    private final Map<KUID, DHTValueEntity> values 
        = Collections.synchronizedMap(new HashMap<KUID, DHTValueEntity>());
    
    public AltLocPublisher(MojitoDHT dht) {
        this.dht = dht;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#getValuesToPublish()
     */
    public Collection<DHTValueEntity> getValuesToPublish() {
        if (!DHTSettings.PUBLISH_ALT_LOCS.getValue()) {
            return Collections.emptySet();
        }
        
        FileManager fileManager = RouterService.getFileManager();
        FileDesc[] fds = fileManager.getAllSharedFileDescriptors();
        
        DHTValueFactory valueFactory = dht.getDHTValueFactory();
        
        // List of DHTValueEntities we're going to publish
        List<DHTValueEntity> publish = new ArrayList<DHTValueEntity>();
        
        synchronized (values) {
            
            // Step One: Add every new FileDesc to the Map
            for (FileDesc fd : fds) {
                if (!(fd instanceof IncompleteFileDesc)) {
                    KUID key = KUIDUtils.toKUID(fd.getSHA1Urn());
                    if (!values.containsKey(key)) {
                        DHTValueEntity entity = valueFactory.createDHTValueEntity(
                                dht.getLocalNode(), dht.getLocalNode(), key, 
                                AltLocDHTValueImpl.SELF, true);
                        
                        values.put(key, entity);
                    }
                }
            }
            
            // Step Two: Remove every DHTValueEntity that is no longer
            // associated with a FileDesc (i.e. the FileDesc was deleted)
            // and create a List of DHTValueEntities that are rare and
            // must be republished
            for (Iterator<DHTValueEntity> it = values.values().iterator(); it.hasNext(); ) {
                DHTValueEntity entity = it.next();
                KUID primaryKey = entity.getKey();
                URN urn = KUIDUtils.toURN(primaryKey);
                
                // For each URN check if the FileDesc still exists
                FileDesc fd = fileManager.getFileDescForUrn(urn);
                
                // If it doesn't then remove it from the values map and
                // replace the entity value with the empty value
                // which will effectively remove the key-value mapping 
                // from the DHT.
                if (fd == null) {
                    entity.setValue(DHTValue.EMPTY_VALUE);
                    it.remove();
                }
                
                // Publish only rare FileDescs and removed entities
                // respectively
                if (fd == null || (isRareFile(fd) && DatabaseUtils.isPublishingRequired(entity))) {
                    publish.add(entity);
                }
            }
        }
        
        return publish;
    }

    public void changeContact(Contact node) {
        synchronized (values) {
            DHTValueFactory valueFactory = dht.getDHTValueFactory();
            
            // Get a copy of the current values
            Collection<DHTValueEntity> entities 
                = new ArrayList<DHTValueEntity>(values.values());
            
            // Clear the Map
            values.clear();
            
            // Rebuild the Map with the new local Contact
            // information
            for (DHTValueEntity entity : entities) {
                KUID primaryKey = entity.getKey();
                
                entity = valueFactory.createDHTValueEntity(
                        dht.getLocalNode(), dht.getLocalNode(), primaryKey, 
                        AltLocDHTValueImpl.SELF, true);
                
                values.put(primaryKey, entity);
            }
        }
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder("AltLocPublisher: ");
        synchronized (values) {
            buffer.append(CollectionUtils.toString(values.values()));
        }
        return buffer.toString();
    }
    
    /**
     * Returns true if the FileDesc is considered rare
     */
    private static boolean isRareFile(FileDesc fd) {
        long time = fd.getLastAttemptedUploadTime();
        long delta = System.currentTimeMillis() - time;
        return (delta >= DHTSettings.RARE_FILE_TIME.getValue());
    }
}
