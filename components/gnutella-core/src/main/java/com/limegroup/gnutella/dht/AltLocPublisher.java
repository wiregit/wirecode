package com.limegroup.gnutella.dht;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueEntityPublisher;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.routing.Contact;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;

/**
 * 
 */
public class AltLocPublisher implements DHTValueEntityPublisher {
    
    private final MojitoDHT dht;
    
    private final Map<KUID, DHTValueEntity> values 
        = Collections.synchronizedMap(new HashMap<KUID, DHTValueEntity>());
    
    public AltLocPublisher(MojitoDHT dht) {
        this.dht = dht;
    }
    
    public DHTValueEntity get(KUID secondaryKey) {
        return values.get(secondaryKey);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#getValues()
     */
    public Collection<DHTValueEntity> getValues() {
        FileManager fileManager = RouterService.getFileManager();
        FileDesc[] fds = fileManager.getAllSharedFileDescriptors();
        
        // Turn the FileDesc-Array to a Map<KUID, FileDesc>
        Map<KUID, FileDesc> current = new HashMap<KUID, FileDesc>();
        for (FileDesc fd : fds) {
            KUID key = LimeDHTUtils.toKUID(fd.getSHA1Urn());
            current.put(key, fd);
        }
        
        synchronized (values) {
            DHTValueFactory valueFactory = dht.getDHTValueFactory();
            
            // For each key that is not in the values map
            // create a new DHTValueEntity.
            for (KUID key : current.keySet()) {
                if (!values.containsKey(key)) {
                    DHTValueEntity entity = valueFactory.createDHTValueEntity(
                            dht.getLocalNode(), dht.getLocalNode(), key, 
                            AltLocDHTValueImpl.SELF, true);
                    
                    values.put(key, entity);
                }
            }
            
            return new ArrayList<DHTValueEntity>(values.values());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#getValuesToPublish()
     */
    public Collection<DHTValueEntity> getValuesToPublish() {
        FileManager fileManager = RouterService.getFileManager();
        List<DHTValueEntity> publish = new ArrayList<DHTValueEntity>();
        
        synchronized (values) {
            Collection<DHTValueEntity> entities = getValues();
            for (DHTValueEntity entity : entities) {
                KUID primaryKey = entity.getKey();
                URN urn = LimeDHTUtils.toURN(primaryKey);
                
                // For each URN check if the FileDesc still exists
                FileDesc fd = fileManager.getFileDescForUrn(urn);
                
                // If it doesn't then remove it from the values map and
                // replace the entity value with the empty value
                // which will effectively remove the key-value mapping 
                // from the DHT.
                if (fd == null) {
                    entity.setValue(DHTValue.EMPTY_VALUE);
                    values.remove(primaryKey);
                }
                
                // Publish only rare FileDescs and removed entities
                // respectively
                if (fd == null || isRare(fd)) {
                    publish.add(entity);
                }
            }
        }
        
        return publish;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#getValuesToForward()
     */
    public Collection<DHTValueEntity> getValuesToForward() {
        FileManager fileManager = RouterService.getFileManager();
        List<DHTValueEntity> forward = new ArrayList<DHTValueEntity>();
        synchronized (values) {
            Collection<DHTValueEntity> entities = getValues();
            for (DHTValueEntity entity : entities) {
                KUID primaryKey = entity.getKey();
                URN urn = LimeDHTUtils.toURN(primaryKey);
                
                // For each URN check if the FileDesc still exists
                FileDesc fd = fileManager.getFileDescForUrn(urn);
                
                // And forward only if it still exists and is rare
                if (fd != null && isRare(fd)) {
                    forward.add(entity);
                }
            }
        }
        return forward;
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
    
    /**
     * Returns true if the FileDesc is considered rare
     * 
     * TODO: Define rare
     */
    private static boolean isRare(FileDesc fd) {
        long time = fd.getLastAttemptedUploadTime();
        return (System.currentTimeMillis() - time >= 0L);
    }
}
