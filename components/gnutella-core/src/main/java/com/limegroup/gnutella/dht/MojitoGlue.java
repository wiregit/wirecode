package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    private final DHTController controller;
    
    public MojitoGlue(DHTController controller) {
        this.controller = controller;
    }
    
    
    /**
     * Finds AlternateLocations for the given URN
     */
    public void getAltLocs(URN urn) {
        if (urn == null) {
            return;
        }
        
        synchronized (controller) {
            MojitoDHT dht = controller.getMojitoDHT();
            if (!dht.isBootstrapped()) {
                return;
            }
            
            KUID key = toKUID(urn);
            DHTFuture<FindValueResult> future = dht.get(key);
            future.addDHTFutureListener(new AltLocResultListener(key));
        }
    }
    
    public void handleFileEvent(FileManagerEvent evt) {
        synchronized (controller) {
            if (!controller.isRunning()) {
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
        reload(evt.getFileManager());
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
     * 
     */
    private void addFileDescAltLoc(FileDesc fd) {
        if (fd == null || fd instanceof IncompleteFileDesc) {
            return;
        }
        
        URN urn = fd.getSHA1Urn();
        if (urn == null) {
            return;
        }
        
        addAltLoc(toKUID(urn));
    }
    
    /**
     * 
     */
    private void addAltLoc(KUID primaryKey) {
        MojitoDHT dht = controller.getMojitoDHT();
        Database database = dht.getDatabase();
        
        synchronized (database) {
            DHTValueBag bag = database.get(primaryKey);
            
            if (!containsLocalAltLoc(bag)) {
                DHTValueFactory valueFactory = dht.getDHTValueFactory();
                
                DHTValueEntity entity = valueFactory.createDHTValueEntity(
                        dht.getLocalNode(), dht.getLocalNode(), primaryKey, 
                        AltLocDHTValueImpl.LOCAL_HOST, true);
                
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
        
        removeAltLoc(toKUID(urn));
    }
    
    /**
     * 
     */
    private void removeAltLoc(KUID primaryKey) {
        MojitoDHT dht = controller.getMojitoDHT();
        Database database = dht.getDatabase();
        
        synchronized (database) {
            DHTValueBag bag = database.get(primaryKey);
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
                                dht.getLocalNode(), dht.getLocalNode(), primaryKey, 
                                DHTValue.EMPTY_VALUE, true);
                        
                        assert (entity.getKey().equals(primaryKey)
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
    private void reload(FileManager fileManager) {
        MojitoDHT dht = controller.getMojitoDHT();
        Database database = dht.getDatabase();
        synchronized (database) {
            Collection<DHTValueEntity> entities 
                = database.values(Selector.LOCAL_VALUES);
            
            for (DHTValueEntity entity : entities) {
                DHTValue value = entity.getValue();
                if (value.getValueType().equals(AltLocDHTValue.ALT_LOC)) {
                    // Replace the current value with an empty value.
                    // The next republish iteration will remove it
                    // from the DHT and the DHTValueManager will remove
                    // it from the Database as well
                    DHTValueFactory valueFactory = dht.getDHTValueFactory();
                    DHTValueEntity e = valueFactory.createDHTValueEntity(
                            dht.getLocalNode(), dht.getLocalNode(), entity.getKey(), 
                            DHTValue.EMPTY_VALUE, true);
                    
                    // Use the add() method instead of store() !!!
                    database.add(e);
                }
            }
            
            FileDesc[] fds = fileManager.getAllSharedFileDescriptors();
            for (FileDesc fd : fds) {
                addFileDescAltLoc(fd);
            }
        }
    }
    
    public void handleDHTEvent(DHTEvent evt) {
        switch(evt.getType()) {
            case STARTING:
            case STOPPED:
            case CONNECTED:
                reload(RouterService.getFileManager());
                break;
        }
    }
    
    private static KUID toKUID(URN urn) {
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException("Expected a SHA-1 URN: " + urn);
        }
        return KUID.createWithBytes(urn.getBytes());
    }
    
    private static URN toURN(KUID kuid) {
        try {
            return URN.createSHA1UrnFromBytes(kuid.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /*private static KUID toKUID(GUID guid) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(guid.bytes());
            byte[] digest = md.digest();
            return KUID.createWithBytes(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    public DHTFuture<FindValueResult> getPushProxies(GUID guid) {
        return getMojitoDHT().get(toKUID(guid));
    }
    
    public DHTFuture<StoreResult> putAltLoc(FileDesc fd) {
        KUID key = toKUID(fd.getSHA1Urn());
        return getMojitoDHT().put(key, AltLocDHTValueImpl.LOCAL_HOST);
    }
    
    public DHTFuture<StoreResult> putAltLoc(URN urn, GUID guid, IpPort ipp, int features, int fwtVersion) {
        if (!RouterService.getConnectionManager().isActiveSupernode()) {
            throw new IllegalStateException("This method works only if we are an Ultrapeer");
        }
        
        KUID key = toKUID(urn);
        return getMojitoDHT().put(key, AltLocDHTValueImpl.createProxyValue(guid, ipp, features, fwtVersion));
    }
    
    public DHTFuture<StoreResult> putPushProxy(GUID guid, Set<? extends IpPort> proxies) {
        KUID key = toKUID(guid);
        return getMojitoDHT().put(key, PushProxiesDHTValueImpl.createProxyValue(proxies));
    }
    
    public static KUID toKUID(URN urn) {
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException("Expected a SHA-1 URN: " + urn);
        }
        return KUID.createWithBytes(urn.getBytes());
    }
    
    public static KUID toKUID(GUID guid) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(guid.bytes());
            byte[] digest = md.digest();
            return KUID.createWithBytes(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static URN toURN(KUID kuid) {
        try {
            return URN.createSHA1UrnFromBytes(kuid.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static boolean isRare(FileDesc fd) {
        long time = fd.getLastAttemptedUploadTime();
        return (System.currentTimeMillis() - time >= 0);
    }
    
    public void publish() {
        FileDesc[] fds = RouterService.getFileManager().getAllSharedFileDescriptors();
        
        MojitoDHT dht = getMojitoDHT();
        Database database = dht.getDatabase();
        synchronized (database) {
            for (FileDesc fd : fds) {
                if (fd instanceof IncompleteFileDesc) {
                    continue;
                }
                
                boolean rare = isRare(fd);
                KUID key = toKUID(fd.getSHA1Urn());
                DHTValueBag bag = database.get(key);
                if (bag == null && rare) {
                    dht.put(key, AltLocDHTValueImpl.LOCAL_HOST);
                } else if (bag != null && !rare){
                    dht.remove(key);
                }
            }
        }
    }*/
    
    /**
     * 
     */
    private static class AltLocResultListener extends DHTFutureAdapter<FindValueResult> {
        
        
        private final KUID key;
        
        private AltLocResultListener(KUID key) {
            this.key = key;
        }
        
        @Override
        public void handleFutureSuccess(FindValueResult result) {
            KUID key = result.getLookupID();
            URN urn = toURN(key);
            
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
                                // TODO continue or exit on failure
                            }
                            
                        } else {
                            IpPort ipp = new IpPortImpl(addr, altLoc.getPort());
                            try {
                                location = AlternateLocation.createDirectAltLoc(ipp, urn);
                            } catch (IOException e) {
                                // TODO continue or exit on failure
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
}
