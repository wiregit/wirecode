package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * The AltLocFinder queries the DHT for Alternate Locations
 */
public class AltLocFinder {

    private static final Log LOG = LogFactory.getLog(AltLocFinder.class);
    
    private final DHTManager manager;
    
    public AltLocFinder(DHTManager manager) {
        this.manager = manager;
    }
    
    /**
     * Finds AlternateLocations for the given URN
     */
    public boolean findAltLocs(URN urn) {
        if (urn == null) {
            return false;
        }
        
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null || !dht.isBootstrapped()) {
                return false;
            }
            
            KUID key = KUIDUtils.toKUID(urn);
            EntityKey lookupKey = EntityKey.createEntityKey(key, AltLocValue.ALT_LOC);
            DHTFuture<FindValueResult> future = dht.get(lookupKey);
            future.addDHTFutureListener(new AltLocsHandler(dht, urn, key));
            return true;
        }
    }
    
    /**
     * Finds push AlternateLocations for the given GUID and URN
     */
    public boolean findPushAltLocs(GUID guid, URN urn) {
        return findPushAltLocs(guid, urn, null);
    }
    
    /**
     * 
     */
    private boolean findPushAltLocs(GUID guid, URN urn, DHTValueEntity altLocEntity) {
        if (guid == null || urn == null) {
            return false;
        }
        
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null || !dht.isBootstrapped()) {
                return false;
            }
            
            KUID key = KUIDUtils.toKUID(guid);
            EntityKey lookupKey = EntityKey.createEntityKey(key, PushProxiesValue.PUSH_PROXIES);
            DHTFuture<FindValueResult> future = dht.get(lookupKey);
            future.addDHTFutureListener(new PushAltLocsHandler(dht, guid, urn, key, altLocEntity));
            return true;
        }
    }
    
    /**
     * The AltLocsHandler listens for the FindValueResult, constructs 
     * AlternateLocations from the results and passes them to AltLocManager 
     * which in turn notifies every Downloader about the new locations.
     */
    private class AltLocsHandler extends DHTFutureAdapter<FindValueResult> {
        
        private final MojitoDHT dht;
        
        private final URN urn;
        
        private final KUID key;
        
        private AltLocsHandler(MojitoDHT dht, URN urn, KUID key) {
            this.dht = dht;
            this.urn = urn;
            this.key = key;
        }
        
        @Override
        public void handleFutureSuccess(FindValueResult result) {
            for (DHTValueEntity entity : result.getEntities()) {
                handleDHTValueEntity(entity);
            }
            
            for (EntityKey entityKey : result.getEntityKeys()) {
                if (!entityKey.equals(AltLocValue.ALT_LOC)) {
                    continue;
                }
                    
                try {
                    DHTFuture<FindValueResult> future = dht.get(entityKey);
                    result = future.get();
                    for (DHTValueEntity entity : result.getEntities()) {
                        handleDHTValueEntity(entity);
                    }
                } catch (ExecutionException e) {
                    LOG.error("ExecutionException", e);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException", e);
                } 
            }
        }

        private void handleDHTValueEntity(DHTValueEntity entity) {
            DHTValue value = entity.getValue();
            if (!(value instanceof AltLocValue)) {
                return;
            }
            
            AltLocValue altLoc = (AltLocValue)value;
            
            // If the AltLoc is firewalled then do a lookup for
            // its PushProxies
            if (altLoc.isFirewalled()) {
                if (DHTSettings.ENABLE_PUSH_PROXY_QUERIES.getValue()) {
                    GUID guid = new GUID(altLoc.getGUID());
                    findPushAltLocs(guid, urn, entity);
                }
                
            // If it's not then create just an AlternateLocation
            // from the info
            } else {
                Contact creator = entity.getCreator();
                InetAddress addr = ((InetSocketAddress)
                        creator.getContactAddress()).getAddress();
                
                IpPort ipp = new IpPortImpl(addr, altLoc.getPort());
                
                long fileSize = altLoc.getFileSize();
                byte[] ttroot = altLoc.getRootHash();
                
                try {
                    AlternateLocation location 
                        = AlternateLocation.createDirectDHTAltLoc(ipp, urn, fileSize, ttroot);
                    AltLocManager.instance().add(location, this);
                } catch (IOException e) {
                    // As above but possible if IpPort is not a 
                    // valid external address
                    LOG.error("IOException", e);
                }
            }
        }
        
        @Override
        public void handleCancellationException(CancellationException e) {
            LOG.error("CancellationException", e);
        }

        @Override
        public void handleExecutionException(ExecutionException e) {
            LOG.error("ExecutionException", e);
        }

        @Override
        public void handleInterruptedException(InterruptedException e) {
            LOG.error("InterruptedException", e);
        }

        public int hashCode() {
            return key.hashCode();
        }
        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof AltLocsHandler)) {
                return false;
            }
            
            return key.equals(((AltLocsHandler)o).key);
        }
    }
    
    /**
     * The PushAltLocsHandler listens for FindValueResults, constructs PushEndpoints
     * from the results and passes them to AltLocManager which in turn notifies all
     * Downloads about the new alternate locations.
     */
    private class PushAltLocsHandler extends DHTFutureAdapter<FindValueResult> {
        
        private final MojitoDHT dht;
        
        private final GUID guid;
        
        private final URN urn;
        
        private final KUID key;
        
        private final DHTValueEntity altLocEntity;
        
        private PushAltLocsHandler(MojitoDHT dht, GUID guid, URN urn, 
                KUID key, DHTValueEntity altLocEntity) {
            this.dht = dht;
            this.guid = guid;
            this.urn = urn;
            this.key = key;
            this.altLocEntity = altLocEntity;
        }
        
        @Override
        public void handleFutureSuccess(FindValueResult result) {
            for (DHTValueEntity entity : result.getEntities()) {
                handleDHTValueEntity(entity);
            }
            
            for (EntityKey entityKey : result.getEntityKeys()) {
                if (!entityKey.equals(PushProxiesValue.PUSH_PROXIES)) {
                    continue;
                }
                    
                try {
                    DHTFuture<FindValueResult> future = dht.get(entityKey);
                    result = future.get();
                    for (DHTValueEntity entity : result.getEntities()) {
                        handleDHTValueEntity(entity);
                    }
                } catch (ExecutionException e) {
                    LOG.error("ExecutionException", e);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException", e);
                } 
            }
        }
        
        private void handleDHTValueEntity(DHTValueEntity entity) {

            DHTValue value = entity.getValue();
            if (!(value instanceof PushProxiesValue)) {
                return;
            }
            
            Contact creator = entity.getCreator();
            InetAddress addr = ((InetSocketAddress)creator).getAddress();
            
            PushProxiesValue pushProxies = (PushProxiesValue)value;
            
            // Make some sanity checks
            if (altLocEntity != null) {
                
                // So we made a lookup for AltLocs, the found AltLoc was 
                // firewalled and we made a lookup for its PushProxies.
                // In any case the creator of both values should be the 
                // same Node!
                if (!creator.equals(altLocEntity.getCreator())) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Creator of " + altLocEntity 
                                + " and found " + entity + " do not match!");
                    }
                    return;
                }
            }

            // Compare the GUIDs from the AltLoc and PushProxy value!
            // They should be the same!
            byte[] guid = this.guid.bytes();
            if (!Arrays.equals(guid, pushProxies.getGUID())) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("The AltLoc and PushProxy GUIDs do not match!");
                }
                return;
            }
            
            Set<? extends IpPort> proxies = pushProxies.getPushProxies();
            byte features = pushProxies.getFeatures();
            int fwtVersion = pushProxies.getFwtVersion();
            IpPort ipp = new IpPortImpl(addr, pushProxies.getPort());
            
            PushEndpoint pe = new PushEndpoint(guid, proxies, features, fwtVersion, ipp);
            
            try {
                AlternateLocation location 
                    = AlternateLocation.createPushAltLoc(pe, urn);
                AltLocManager.instance().add(location, this);
            } catch (IOException e) {
                // Impossible. Thrown if URN or PushEndpoint is null
                LOG.error("IOException", e);
            }
        }
        
        @Override
        public void handleCancellationException(CancellationException e) {
            LOG.error("CancellationException", e);
        }

        @Override
        public void handleExecutionException(ExecutionException e) {
            LOG.error("ExecutionException", e);
        }

        @Override
        public void handleInterruptedException(InterruptedException e) {
            LOG.error("InterruptedException", e);
        }
        
        public int hashCode() {
            return key.hashCode();
        }
        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof PushAltLocsHandler)) {
                return false;
            }
            
            return key.equals(((PushAltLocsHandler)o).key);
        }
    }
}
