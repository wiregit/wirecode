package com.limegroup.gnutella.dht;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueBag;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.Database.Selector;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.FileManagerEvent.Type;

/**
 * 
 */
public class AltLocPublisher implements FileEventListener, DHTEventListener {
    
    private final DHTManager manager;
    
    public AltLocPublisher(DHTManager manager) {
        this.manager = manager;
        
        RouterService.getFileManager().addFileEventListener(this);
        manager.addEventListener(this);
    }
    
    public void handleFileEvent(FileManagerEvent evt) {
        synchronized (manager) {
            if (!manager.isRunning()) {
                return;
            }
            
            if (evt.getType().equals(Type.FILEMANAGER_LOADED)) {
                handleFileManagerLoaded(evt);
                
            } else if (evt.getFileManager().isLoadFinished()) {
                switch(evt.getType()) {
                    case ADD_FILE:
                        handleAddFile(evt);
                        break;
                    case CHANGE_FILE:
                        handleChangeFile(evt);
                        break;
                    case REMOVE_FILE:
                        handleRemoveFile(evt);
                        break;
                }
            }
        }
    }

    /**
     * 
     */
    public void handleDHTEvent(DHTEvent evt) {
        synchronized (manager) {
            switch(evt.getType()) {
                case CONNECTED:
                    refresh(RouterService.getFileManager());
                    break;
            }
        }
    }
    
    /**
     * 
     *
     */
    public void refresh() {
        synchronized (manager) {
            if (manager.isRunning()) {
                refresh(RouterService.getFileManager());
            }
        }
    }
    
    public void putPushAltLocs(GUID guid, PushProxiesDHTValue value) {
        if (guid == null || value == null) {
            return;
        }
        
        if (!RouterService.isSupernode()) {
            return;
        }
        
        /*ConnectionManager cm = RouterService.getConnectionManager();
        List<ManagedConnection> connections = cm.getInitializedClientConnections();
        boolean isLeaf = false;
        for (ManagedConnection mc : connections) {
            if (Arrays.equals(mc.getClientGUID(), guid.bytes())) {
                isLeaf = true;
                break;
            }
        }
        
        if (!isLeaf) {
            return;
        }*/
        
        // TODO do some sanity checks on the value
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null) {
                return;
            }
            
            KUID key = LimeDHTUtils.toKUID(guid);
            DHTValueFactory valueFactory = dht.getDHTValueFactory();
            DHTValueEntity entity = valueFactory.createDHTValueEntity(
                    dht.getLocalNode(), dht.getLocalNode(), key, 
                    value, true);
            
            Database database = dht.getDatabase();
            database.store(entity);
        }
    }
    
    private void handleFileManagerLoaded(FileManagerEvent evt) {
        refresh(evt.getFileManager());
    }

    private void handleAddFile(FileManagerEvent evt) {
        addFileDescAltLoc(evt.getFileDescs()[0]);
    }
    
    private void handleChangeFile(FileManagerEvent evt) {
        removeFileDescAltLoc(evt.getFileDescs()[0]);
        addFileDescAltLoc(evt.getFileDescs()[1]);
    }
    
    private void handleRemoveFile(FileManagerEvent evt) {
        removeFileDescAltLoc(evt.getFileDescs()[0]);
    }
    
    /**
     * Store the localhost as AltLoc for the given FileDesc
     */
    private void addFileDescAltLoc(FileDesc fd) {
        if (fd == null || fd instanceof IncompleteFileDesc) {
            return;
        }
        
        URN urn = fd.getSHA1Urn();
        if (urn == null) {
            return;
        }
        
        addAltLoc(LimeDHTUtils.toKUID(urn));
    }
    
    /**
     * Stores the localhost as AltLoc under the given key
     * in the DHT
     */
    private void addAltLoc(KUID key) {
        
        MojitoDHT dht = manager.getMojitoDHT();
        if (dht == null) {
            return;
        }
        
        Database database = dht.getDatabase();
        
        synchronized (database) {
            DHTValueBag bag = database.get(key);
            
            // Do not store multiple times per key
            if (!containsLocalAltLoc(bag)) {
                DHTValueFactory valueFactory = dht.getDHTValueFactory();
                
                DHTValueEntity entity = valueFactory.createDHTValueEntity(
                        dht.getLocalNode(), dht.getLocalNode(), key, 
                        AltLocDHTValueImpl.SELF, true);
                
                database.store(entity);
            }
        }
    }
    
    /**
     * Returns true if the given DHTValueBag contains a
     * local DHTValue of type ALT_LOC
     */
    private boolean containsLocalAltLoc(DHTValueBag bag) {
        if (bag == null) {
            return false;
        }
        
        synchronized (bag.getValuesLock()) {
            for (DHTValueEntity entity : bag.getAllValues()) {
                if (entity.isLocalValue()) {
                    DHTValue value = entity.getValue();
                    if (value.getValueType().equals(AltLocDHTValue.ALT_LOC)) {
                        
                        // If it's an empty value do as if it's not there
                        return !value.isEmpty();
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Removes the given FileDesc AltLoc information
     * from the DHT
     */
    private void removeFileDescAltLoc(FileDesc fd) {
        if (fd == null || fd instanceof IncompleteFileDesc) {
            return;
        }
        
        URN urn = fd.getSHA1Urn();
        if (urn == null) {
            return;
        }
        
        removeAltLoc(LimeDHTUtils.toKUID(urn));
    }
    
    /**
     * Removes the given Key from the DHT
     */
    private void removeAltLoc(KUID key) {
        MojitoDHT dht = manager.getMojitoDHT();
        if (dht == null) {
            return;
        }
        
        Database database = dht.getDatabase();
        
        synchronized (database) {
            DHTValueBag bag = database.get(key);
            if (bag == null) {
                return;
            }
            
            List<DHTValueEntity> values = null;
            synchronized (bag.getValuesLock()) {
                values = new ArrayList<DHTValueEntity>(bag.getAllValues());
            }
            
            for (DHTValueEntity entity : values) {
                if (entity.isLocalValue()) {
                    DHTValue value = entity.getValue();
                    if (value.getValueType().equals(AltLocDHTValue.ALT_LOC)) {
                        
                        // Replace the current value with an empty value.
                        // The next republish iteration will remove it
                        // from the DHT and the DHTValueManager will remove
                        // it from the Database as well
                        DHTValueFactory valueFactory = dht.getDHTValueFactory();
                        DHTValueEntity e = valueFactory.createDHTValueEntity(
                                dht.getLocalNode(), dht.getLocalNode(), key, 
                                DHTValue.EMPTY_VALUE, true);
                        
                        assert (entity.getKey().equals(key)
                                && entity.getSecondaryKey().equals(dht.getLocalNodeID()));
                        
                        // Use the add() method instead of store() !!!
                        database.add(e);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Matches up the Database with the FileManager
     */
    private void refresh(FileManager fileManager) {
        MojitoDHT dht = manager.getMojitoDHT();
        if (dht == null) {
            return;
        }
        
        Database database = dht.getDatabase();
        synchronized (database) {
            Collection<DHTValueEntity> entities 
                = database.values(Selector.LOCAL_VALUES);
            
            // Go through all local values and check...
            for (DHTValueEntity entity : entities) {
                DHTValue value = entity.getValue();
                
                // 1) Check if it's an AltLoc
                if (value.getValueType().equals(AltLocDHTValue.ALT_LOC)) {
                    
                    // 2) Check if the associated FileDesc exists or not.
                    //    If it exists then skip, else...
                    URN urn = LimeDHTUtils.toURN(entity.getKey());
                    FileDesc fd = fileManager.getFileDescForUrn(urn);
                    if (fd == null) {
                        // 3) Replace the existing entity with an entity
                        //    that has an empty value (an empty value
                        //    is intrepreded as a remove operation). Mojito 
                        //    publishes a local empty value once and deletes 
                        //    it from the Database
                        DHTValueFactory valueFactory = dht.getDHTValueFactory();
                        DHTValueEntity e = valueFactory.createDHTValueEntity(
                                dht.getLocalNode(), dht.getLocalNode(), entity.getKey(), 
                                DHTValue.EMPTY_VALUE, true);
                        
                        // Use the add() method instead of store() !!!
                        database.add(e);
                    }
                }
            }
            
            
            FileDesc[] fds = fileManager.getAllSharedFileDescriptors();
            for (FileDesc fd : fds) {
                addFileDescAltLoc(fd);
            }
        }
    }
}
