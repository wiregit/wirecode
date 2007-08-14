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
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.nio.observer.Shutdownable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * The AltLocFinder queries the DHT for Alternate Locations
 */
@Singleton
public class AltLocFinder {

    private static final Log LOG = LogFactory.getLog(AltLocFinder.class);
    
    private final DHTManager manager;
    private final AlternateLocationFactory alternateLocationFactory;
    
    @Inject
    public AltLocFinder(DHTManager manager, AlternateLocationFactory alternateLocationFactory) {
        this.manager = manager;
        this.alternateLocationFactory = alternateLocationFactory;
    }
    
    /**
     * Finds AlternateLocations for the given URN
     */
    public Shutdownable findAltLocs(URN urn, AltLocSearchListener listener) {
        if (urn == null) {
            return null;
        }
        
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null || !dht.isBootstrapped()) {
                return null;
            }
            
            KUID key = KUIDUtils.toKUID(urn);
            EntityKey lookupKey = EntityKey.createEntityKey(key, AbstractAltLocValue.ALT_LOC);
            final DHTFuture<FindValueResult> future = dht.get(lookupKey);
            future.addDHTFutureListener(new AltLocsHandler(dht, urn, key, listener));
            return new Shutdownable() {
                public void shutdown() {
                    future.cancel(true);
                }
            };
        }
    }
    
    /**
     * Finds push AlternateLocations for the given GUID and URN
     */
    public boolean findPushAltLocs(GUID guid, URN urn) {
        return findPushAltLocs(guid, urn, null, null);
    }
    
    /**
     * 
     */
    private boolean findPushAltLocs(GUID guid, URN urn, 
            DHTValueEntity altLocEntity, AltLocSearchListener listener) {
        
        if (guid == null || urn == null) {
            return false;
        }
        
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null || !dht.isBootstrapped()) {
                return false;
            }
            
            KUID key = KUIDUtils.toKUID(guid);
            EntityKey lookupKey = EntityKey.createEntityKey(key, AbstractPushProxiesValue.PUSH_PROXIES);
            DHTFuture<FindValueResult> future = dht.get(lookupKey);
            future.addDHTFutureListener(new PushAltLocsHandler(dht, guid, urn, key, altLocEntity, listener));
            return true;
        }
    }
    
    /**
     * An abstract implementation of DHTFutureAdapter to handle AltLocValues
     * and PushAltLocValues.
     */
    private abstract class AbstractResultHandler extends DHTFutureAdapter<FindValueResult> {
        
        protected final MojitoDHT dht;
        
        protected final URN urn;
        
        protected final KUID key;
        
        protected final AltLocSearchListener listener;
        
        protected final DHTValueType valueType;
        
        private AbstractResultHandler(MojitoDHT dht, URN urn, 
                KUID key, AltLocSearchListener listener, 
                DHTValueType valueType) {
            
            this.dht = dht;
            this.urn = urn;
            this.key = key;
            this.listener = listener;
            this.valueType = valueType;
        }
        
        @Override
        public void handleFutureSuccess(FindValueResult result) {
            boolean success = false;
            
            if (result.isSuccess()) {
                for (DHTValueEntity entity : result.getEntities()) {
                    if (handleDHTValueEntity(entity)) {
                        success = true;
                    }
                }
                
                for (EntityKey entityKey : result.getEntityKeys()) {
                    if (!entityKey.getDHTValueType().equals(valueType)) {
                        continue;
                    }
                        
                    try {
                        DHTFuture<FindValueResult> future = dht.get(entityKey);
                        FindValueResult resultFromKey = future.get();
                        
                        if (resultFromKey.isSuccess()) {
                            for (DHTValueEntity entity : resultFromKey.getEntities()) {
                                if (handleDHTValueEntity(entity)) {
                                    success = true;
                                }
                            }
                        }
                    } catch (ExecutionException e) {
                        LOG.error("ExecutionException", e);
                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException", e);
                    } 
                }
            }
            
            if (listener != null) {
                listener.handleAltLocSearchDone(success);
            }
        }
        
        /**
         * Handles a DHTValueEntity (turns it into some Gnutella Object)
         * and returns true on success
         */
        protected abstract boolean handleDHTValueEntity(DHTValueEntity entity);
        
        @Override
        public void handleCancellationException(CancellationException e) {
            LOG.error("CancellationException", e);
            
            if (listener != null) {
                listener.handleAltLocSearchDone(false);
            }
        }

        @Override
        public void handleExecutionException(ExecutionException e) {
            LOG.error("ExecutionException", e);
            
            if (listener != null) {
                listener.handleAltLocSearchDone(false);
            }
        }

        @Override
        public void handleInterruptedException(InterruptedException e) {
            LOG.error("InterruptedException", e);
            
            if (listener != null) {
                listener.handleAltLocSearchDone(false);
            }
        }
        
        public int hashCode() {
            return key.hashCode();
        }
        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof AbstractResultHandler)) {
                return false;
            }
            
            AbstractResultHandler other = (AbstractResultHandler)o;
            return key.equals(other.key)
                    && valueType.equals(other.valueType);
        }
    }
    
    /**
     * The AltLocsHandler listens for the FindValueResult, constructs 
     * AlternateLocations from the results and passes them to AltLocManager 
     * which in turn notifies every Downloader about the new locations.
     */
    private class AltLocsHandler extends AbstractResultHandler {
        
        private AltLocsHandler(MojitoDHT dht, URN urn, KUID key, 
                AltLocSearchListener listener) {
            super(dht, urn, key, listener, AbstractAltLocValue.ALT_LOC);
        }
        
        @Override
        protected boolean handleDHTValueEntity(DHTValueEntity entity) {
            DHTValue value = entity.getValue();
            if (!(value instanceof AltLocValue)) {
                return false;
            }
            
            AltLocValue altLoc = (AltLocValue)value;
            
            // If the AltLoc is firewalled then do a lookup for
            // its PushProxies
            if (altLoc.isFirewalled()) {
                if (DHTSettings.ENABLE_PUSH_PROXY_QUERIES.getValue()) {
                    GUID guid = new GUID(altLoc.getGUID());
                    
                    // TODO: Return false?
                    return findPushAltLocs(guid, urn, entity, listener);
                }
                
            // If it's not then create just an AlternateLocation
            // from the info
            } else {
                Contact creator = entity.getCreator();
                InetAddress addr = ((InetSocketAddress)
                        creator.getContactAddress()).getAddress();
                
                IpPort ipp = new IpPortImpl(addr, altLoc.getPort());
                Connectable c = new ConnectableImpl(ipp, altLoc.supportsTLS());

                long fileSize = altLoc.getFileSize();
                byte[] ttroot = altLoc.getRootHash();
                try {
                    AlternateLocation location = alternateLocationFactory
                            .createDirectDHTAltLoc(c, urn, fileSize, ttroot);
                    ProviderHacks.getAltLocManager().add(location, this);
                    return true;
                } catch (IOException e) {
                    // Thrown if IpPort is an invalid address
                    LOG.error("IOException", e);
                }
            }
            
            return false;
        }
    }
    
    /**
     * The PushAltLocsHandler listens for FindValueResults, constructs PushEndpoints
     * from the results and passes them to AltLocManager which in turn notifies all
     * Downloads about the new alternate locations.
     */
    private class PushAltLocsHandler extends AbstractResultHandler {
        
        private final GUID guid;
        
        private final DHTValueEntity altLocEntity;
        
        private PushAltLocsHandler(MojitoDHT dht, GUID guid, URN urn, 
                KUID key, DHTValueEntity altLocEntity, AltLocSearchListener listener) {
            super(dht, urn, key, listener, AbstractPushProxiesValue.PUSH_PROXIES);
            
            this.guid = guid;
            this.altLocEntity = altLocEntity;
        }
        
        @Override
        protected boolean handleDHTValueEntity(DHTValueEntity entity) {

            DHTValue value = entity.getValue();
            if (!(value instanceof PushProxiesValue)) {
                return false;
            }
            
            Contact creator = entity.getCreator();
            InetAddress addr = ((InetSocketAddress)creator.getContactAddress()).getAddress();
            
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
                    return false;
                }
            }

            // Compare the GUIDs from the AltLoc and PushProxy value!
            // They should be the same!
            byte[] guid = this.guid.bytes();
            if (!Arrays.equals(guid, pushProxies.getGUID())) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("The AltLoc and PushProxy GUIDs do not match!");
                }
                return false;
            }
            
            Set<? extends IpPort> proxies = pushProxies.getPushProxies();
            byte features = pushProxies.getFeatures();
            int fwtVersion = pushProxies.getFwtVersion();
            IpPort ipp = new IpPortImpl(addr, pushProxies.getPort());
            PushEndpoint pe = ProviderHacks.getPushEndpointFactory().createPushEndpoint(guid, proxies, features, fwtVersion, ipp);
            
            try {
                AlternateLocation location = alternateLocationFactory.createPushAltLoc(pe, urn);
                ProviderHacks.getAltLocManager().add(location, this);
                return true;
            } catch (IOException e) {
                // Impossible. Thrown if URN or PushEndpoint is null
                LOG.error("IOException", e);
            }
            
            return false;
        }
    }
}
