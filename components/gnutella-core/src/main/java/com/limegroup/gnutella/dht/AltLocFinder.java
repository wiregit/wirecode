package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
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
            
            KUID key = LimeDHTUtils.toKUID(urn);
            DHTFuture<FindValueResult> future = dht.get(key);
            future.addDHTFutureListener(new AltLocsHandler(dht, urn, key));
            return true;
        }
    }
    
    /**
     * Finds push AlternateLocations for the given GUID and URN
     */
    public boolean findPushAltLocs(GUID guid, URN urn) {
        if (guid == null || urn == null) {
            return false;
        }
        
        synchronized (manager) {
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null || !dht.isBootstrapped()) {
                return false;
            }
            
            KUID key = LimeDHTUtils.toKUID(guid);
            DHTFuture<FindValueResult> future = dht.get(key);
            future.addDHTFutureListener(new PushAltLocsHandler(dht, guid, urn, key));
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
                if (!entityKey.equals(AltLocDHTValue.ALT_LOC)) {
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
            AltLocManager altLocManager = AltLocManager.instance();
            
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
                        //findPushAltLocs(urn, new GUID(guid));
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
        
        private PushAltLocsHandler(MojitoDHT dht, GUID guid, URN urn, KUID key) {
            this.dht = dht;
            this.guid = guid;
            this.urn = urn;
            this.key = key;
        }
        
        @Override
        public void handleFutureSuccess(FindValueResult result) {
            for (DHTValueEntity entity : result.getEntities()) {
                handleDHTValueEntity(entity);
            }
            
            for (EntityKey entityKey : result.getEntityKeys()) {
                if (!entityKey.equals(AltLocDHTValue.ALT_LOC)) {
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
            AltLocManager altLocManager = AltLocManager.instance();
            
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
