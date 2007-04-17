package com.limegroup.gnutella.dht.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModel;
import org.limewire.mojito.result.StoreResult;
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
public class AltLocPublisher implements StorableModel {
    
    private final Map<KUID, Storable> values 
        = Collections.synchronizedMap(new HashMap<KUID, Storable>());
    
    public Collection<Storable> getStorables() {
        if (!DHTSettings.PUBLISH_ALT_LOCS.getValue()) {
            return Collections.emptySet();
        }
        
        FileManager fileManager = RouterService.getFileManager();
        FileDesc[] fds = fileManager.getAllSharedFileDescriptors();
        
        // List of Storables we're going to publish
        List<Storable> publish = new ArrayList<Storable>();
        
        synchronized (values) {
            
            // Step One: Add every new FileDesc to the Map
            for (FileDesc fd : fds) {
                if (!(fd instanceof IncompleteFileDesc)) {
                    KUID primaryKey = KUIDUtils.toKUID(fd.getSHA1Urn());
                    if (!values.containsKey(primaryKey)) {
                        
                        values.put(primaryKey, new Storable(
                                primaryKey, AltLocDHTValueImpl.SELF));
                    }
                }
            }
            
            // Step Two: Remove every DHTValueEntity that is no longer
            // associated with a FileDesc (i.e. the FileDesc was deleted)
            // and create a List of DHTValueEntities that are rare and
            // must be republished
            for (Iterator<Storable> it = values.values().iterator(); it.hasNext(); ) {
                Storable storable = it.next();
                KUID primaryKey = storable.getPrimaryKey();
                URN urn = KUIDUtils.toURN(primaryKey);
                
                // For each URN check if the FileDesc still exists
                FileDesc fd = fileManager.getFileDescForUrn(urn);
                
                // If it doesn't then remove it from the values map and
                // replace the entity value with the empty value
                // which will effectively remove the key-value mapping 
                // from the DHT.
                if (fd == null) {
                    storable = new Storable(primaryKey, DHTValue.EMPTY_VALUE);
                    it.remove();
                }
                
                // Publish only rare FileDescs and removed entities
                // respectively
                if (fd == null || (isRareFile(fd) && DatabaseUtils.isPublishingRequired(storable))) {
                    publish.add(storable);
                }
            }
        }
        
        return publish;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.StorableModel#handleStoreResult(org.limewire.mojito.db.Storable, org.limewire.mojito.result.StoreResult)
     */
    public void handleStoreResult(Storable storable, StoreResult result) {
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.StorableModel#handleContactChange()
     */
    public void handleContactChange() {
        
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
        if (fd.getAttemptedUploads() < DHTSettings.RARE_FILE_ATTEMPTED_UPLOADS.getValue()
                || fd.getCompletedUploads() < DHTSettings.RARE_FILE_COMPLETED_UPLOADS.getValue()) {
            return false;
        }
        
        long time = fd.getLastAttemptedUploadTime();
        long delta = System.currentTimeMillis() - time;
        return (delta >= DHTSettings.RARE_FILE_TIME.getValue());
    }
}
