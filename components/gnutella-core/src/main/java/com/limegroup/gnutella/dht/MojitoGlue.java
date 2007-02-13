package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueBag;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.Database.Selector;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.FileManagerEvent.Type;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;

/**
 * 
 */
public class MojitoGlue implements FileEventListener, DHTEventListener {
    
    private static MojitoGlue GLUE = null;
    
    public synchronized static MojitoGlue instance() {
        return GLUE;
    }
    
    public synchronized static MojitoGlue instance(DHTManager manager) {
        if (GLUE == null) {
            GLUE = new MojitoGlue(manager);
        }
        return GLUE;
    }
    
    private final DHTManager manager;
    
    private MojitoGlue(DHTManager manager) {
        this.manager = manager;
    }
    
    /**
     * Finds AlternateLocations for the given URN
     */
    public void findAltLocs(URN urn) {
        if (urn == null) {
            return;
        }
        
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null || !dht.isBootstrapped()) {
                return;
            }
            
            KUID key = LimeDHTUtils.toKUID(urn);
            DHTFuture<FindValueResult> future = dht.get(key);
            future.addDHTFutureListener(new AltLocResultListener(urn, key));
        }
    }
    
    public void findPushProxies(URN urn, GUID guid) {
        if (urn == null || guid == null) {
            return;
        }
        
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null || !dht.isBootstrapped()) {
                return;
            }
            
            KUID key = LimeDHTUtils.toKUID(guid);
            DHTFuture<FindValueResult> future = dht.get(key);
            future.addDHTFutureListener(new PushProxiesResultListener(urn, guid, key));
        }
    }
    
    public void putPushProxies(GUID guid, PushProxiesDHTValue value) {
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
     * 
     */
    public void handleDHTEvent(DHTEvent evt) {
        synchronized (manager) {
            switch(evt.getType()) {
                //case STARTING:
                //case STOPPED:
                case CONNECTED:
                    refresh(RouterService.getFileManager());
                    break;
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
                        
                        System.out.println("REMOVE: " + e);
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
    
    public void dump() {
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null) {
                return;
            }
            
            System.out.println(dht.getDatabase());
        }
    }
    
    /**
     * 
     */
    private static class AltLocResultListener extends DHTFutureAdapter<FindValueResult> {
        
        private static final Log LOG = LogFactory.getLog(AltLocResultListener.class);
        
        private final URN urn;
        
        private final KUID key;
        
        private AltLocResultListener(URN urn, KUID key) {
            this.urn = urn;
            this.key = key;
        }
        
        @Override
        public void handleFutureSuccess(FindValueResult result) {
            AltLocManager altLocManager = AltLocManager.instance();
            
            for (Future<DHTValueEntity> future : result) {
                try {
                    DHTValueEntity entity = future.get();
                    DHTValue value = entity.getValue();
                    if (value instanceof AltLocDHTValue) {
                        AltLocDHTValue altLoc = (AltLocDHTValue)value;
                        Contact creator = entity.getCreator();
                        
                        // The IP-Address of the Value creator. It can be
                        // two things! It's either the address of the Host
                        // who has the actual file (a non-firewalled Node that's
                        // connected to the DHT) or it's the address of a
                        // Node's Ultrapeer who published the value for the
                        // firewalled Node.
                        InetAddress addr = ((InetSocketAddress)
                                creator.getContactAddress()).getAddress();
                        
                        AlternateLocation location = null;
                        
                        if (altLoc.isFirewalled()) {
                            // The firewalled Leaf
                            byte[] guid = altLoc.getGUID();
                            int features = altLoc.getFeatures();
                            int fwtVersion = altLoc.getFwtVersion();
                            IpPort ipp = new IpPortImpl(altLoc.getInetAddress(), altLoc.getPort());
                            
                            // Its Ultrapeer that published the Value. If they're 
                            // still connected then we're pretty much done. If no
                            // you must probably do a second lookup for SHA-1(GUID)
                            // to find the Leaf's Push Proxies
                            IpPort proxy = new IpPortImpl(addr, altLoc.getPushProxyPort());
                            PushEndpoint pe = new PushEndpoint(
                                    guid, Collections.singleton(proxy), features, fwtVersion, ipp);
                            
                            try {
                                location = AlternateLocation.createPushAltLoc(pe, urn);
                            } catch (IOException e) {
                                // Impossible. Thrown if URN or PushEndpoint is null
                                LOG.error("IOException", e);
                            }
                            
                        } else {
                            IpPort ipp = new IpPortImpl(addr, altLoc.getPort());
                            try {
                                location = AlternateLocation.createDirectAltLoc(ipp, urn);
                            } catch (IOException e) {
                                // As above but possible if IpPort is not a 
                                // valid external address
                                LOG.error("IOException", e);
                            }
                        }
                        
                        if (location != null) {
                            altLocManager.add(location, this);
                        }
                    }
                } catch (ExecutionException e) {
                } catch (InterruptedException e) {
                } 
            }
        }
        
        public int hashCode() {
            return key.hashCode();
        }
        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof AltLocResultListener)) {
                return false;
            }
            
            return key.equals(((AltLocResultListener)o).key);
        }
    }
    
    /**
     * 
     */
    private static class PushProxiesResultListener extends DHTFutureAdapter<FindValueResult> {
        
        private static final Log LOG = LogFactory.getLog(PushProxiesResultListener.class);
        
        private final URN urn;
        
        private final GUID guid;
        
        private final KUID key;
        
        private PushProxiesResultListener(URN urn, GUID guid, KUID key) {
            this.urn = urn;
            this.guid = guid;
            this.key = key;
        }
        
        @Override
        public void handleFutureSuccess(FindValueResult result) {

            AltLocManager altLocManager = AltLocManager.instance();
            
            for (Future<DHTValueEntity> future : result) {
                try {
                    DHTValueEntity entity = future.get();
                    DHTValue value = entity.getValue();
                    if (value instanceof PushProxiesDHTValue) {
                        PushProxiesDHTValue proxiesValue = (PushProxiesDHTValue)value;
                        
                        byte[] guid = this.guid.bytes();
                        Set<? extends IpPort> proxies = proxiesValue.getPushProxies();
                        int features = proxiesValue.getFeatures();
                        int fwtVersion = proxiesValue.getFwtVersion();
                        IpPort ipp = new IpPortImpl(proxiesValue.getInetAddress(), proxiesValue.getPort());
                        
                        PushEndpoint pe = new PushEndpoint(guid, proxies, features, fwtVersion, ipp);
                        
                        AlternateLocation location = null;
                        
                        try {
                            location = AlternateLocation.createPushAltLoc(pe, urn);
                        } catch (IOException e) {
                            // Impossible. Thrown if URN or PushEndpoint is null
                            LOG.error("IOException", e);
                        }
                        
                        if (location != null) {
                            altLocManager.add(location, this);
                        }
                    }
                } catch (ExecutionException e) {
                } catch (InterruptedException e) {
                } 
            }
        }
        
        public int hashCode() {
            return key.hashCode();
        }
        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof PushProxiesResultListener)) {
                return false;
            }
            
            return key.equals(((PushProxiesResultListener)o).key);
        }
    }
}
