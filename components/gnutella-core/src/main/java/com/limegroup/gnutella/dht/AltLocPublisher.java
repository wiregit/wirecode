package com.limegroup.gnutella.dht;

import java.util.ArrayList;
import java.util.Collection;
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
import org.limewire.mojito.result.StoreResult;
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
    
    private final Map<KUID, DHTValueEntity> values = new HashMap<KUID, DHTValueEntity>();
    
    public AltLocPublisher(MojitoDHT dht) {
        this.dht = dht;
    }
    
    public DHTValueEntity get(KUID key) {
        return values.get(key);
    }

    public Collection<DHTValueEntity> values() {
        FileManager fileManager = RouterService.getFileManager();
        FileDesc[] fds = fileManager.getAllSharedFileDescriptors();
        
        // Turn the FileDesc-Array to a Map<KUID, FileDesc>
        Map<KUID, FileDesc> current = new HashMap<KUID, FileDesc>();
        for (FileDesc fd : fds) {
            KUID key = LimeDHTUtils.toKUID(fd.getSHA1Urn());
            current.put(key, fd);
        }
        
        List<DHTValueEntity> entities = new ArrayList<DHTValueEntity>();
        synchronized (values) {
            
            // For each key that is not in the values map
            // create a new DHTValueEntity.
            for (KUID key : current.keySet()) {
                if (!values.containsKey(key)) {
                    DHTValueFactory valueFactory = dht.getDHTValueFactory();
                    
                    DHTValueEntity entity = valueFactory.createDHTValueEntity(
                            dht.getLocalNode(), dht.getLocalNode(), key, 
                            AltLocDHTValueImpl.SELF, true);
                    
                    values.put(key, entity);
                }
            }
            
            for (Iterator<DHTValueEntity> it = values.values().iterator(); it.hasNext(); ) {
                DHTValueEntity entity = it.next();
                URN urn = LimeDHTUtils.toURN(entity.getKey());
                
                // For each URN check if the FileDesc still exists
                FileDesc fd = fileManager.getFileDescForUrn(urn);
                
                // If it doesn't remove it from the values map and
                // replace the entity value with the empty value
                // which will effectively remove the key-value mapping 
                // from the DHT
                if (fd == null) {
                    entity.setValue(DHTValue.EMPTY_VALUE);
                    it.remove();
                }
                
                // Publish only rare FileDescs and removed entities
                // respectively
                if (fd == null || isRare(fd)) {
                    entities.add(entity);
                }
            }
        }
        
        return entities;
    }
    
    public void published(StoreResult result) {
        
    }

    public void handleContactChange(DHTValueFactory factory, Contact node) {
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
