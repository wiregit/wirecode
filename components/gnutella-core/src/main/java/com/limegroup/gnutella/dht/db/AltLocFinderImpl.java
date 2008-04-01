package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

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
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.nio.observer.Shutdownable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * Default implementation of {@link AltLocFinder}, uses the DHT to find
 * alternate locations.
 */
@Singleton
public class AltLocFinderImpl implements AltLocFinder {

    static final Log LOG = LogFactory.getLog(AltLocFinderImpl.class);
    
    private final DHTManager manager;

    private final AlternateLocationFactory alternateLocationFactory;

    private final AltLocManager altLocManager;

    private final PushEndpointService pushEndpointManager;
    
    @Inject
    public AltLocFinderImpl(DHTManager manager, AlternateLocationFactory alternateLocationFactory, 
            AltLocManager altLocManager, 
            @Named("pushEndpointManager") PushEndpointService pushEndpointManager) {
        this.manager = manager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.altLocManager = altLocManager;
        this.pushEndpointManager = pushEndpointManager;
    }
    
    public Shutdownable findAltLocs(URN urn, SearchListener<AlternateLocation> listener) {
        if (urn == null) {
            return null;
        }
        listener = SearchListenerAdapter.nonNullListener(listener);
        
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
     */
    private void findPushAltLocs(final GUID guid, final URN urn, 
            final DHTValueEntity altLocEntity, final SearchListener<AlternateLocation> listener) {
        
        SearchListener<PushEndpoint> pushEndpointListener = new SearchListener<PushEndpoint>() {
            public void handleResult(PushEndpoint pushEndpoint) {
                if (altLocEntity != null) {
                    // So we made a lookup for AltLocs, the found AltLoc was 
                    // firewalled and we made a lookup for its PushProxies.
                    // In any case the creator of both values should be the 
                    // same Node!
                    InetAddress creatorAddress = ((InetSocketAddress)altLocEntity.getCreator().getContactAddress()).getAddress();
                    if (!pushEndpoint.getInetAddress().equals(creatorAddress)) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Creator of " + altLocEntity 
                                    + " and found " + pushEndpoint + " do not match!");
                        }
                        listener.handleSearchDone(false);
                        return;
                    }
                }
                AlternateLocation alternateLocation = alternateLocationFactory.createPushAltLoc(pushEndpoint, urn);
                listener.handleResult(alternateLocation);
                listener.handleSearchDone(true);
            }

            public void handleSearchDone(boolean success) {
                if (!success) {
                    listener.handleSearchDone(success);
                }
            }  
            
        };
        pushEndpointManager.findPushEndpoint(guid, pushEndpointListener);
    }
    
    /**
     * The AltLocsHandler listens for the FindValueResult, constructs 
     * AlternateLocations from the results and passes them to AltLocManager 
     * which in turn notifies every Downloader about the new locations.
     */
    private class AltLocsHandler extends AbstractResultHandler {
        
        private final SearchListener<AlternateLocation> listener;
        private final URN urn;

        private AltLocsHandler(MojitoDHT dht, URN urn, KUID key, 
                SearchListener<AlternateLocation> listener) {
            super(dht, key, listener, AbstractAltLocValue.ALT_LOC);
            this.urn = urn;
            this.listener = listener;
        }
        
        @Override
        protected Result handleDHTValueEntity(DHTValueEntity entity) {
            DHTValue value = entity.getValue();
            if (!(value instanceof AltLocValue)) {
                return Result.NOT_FOUND;
            }
            
            AltLocValue altLoc = (AltLocValue)value;
            
            // If the AltLoc is firewalled then do a lookup for
            // its PushProxies
            if (altLoc.isFirewalled()) {
                if (DHTSettings.ENABLE_PUSH_PROXY_QUERIES.getValue()) {
                    GUID guid = new GUID(altLoc.getGUID());
                    findPushAltLocs(guid, urn, entity, listener);
                    return Result.NOT_YET_FOUND;
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
                    altLocManager.add(location, this);
                    listener.handleResult(location);
                    return Result.FOUND;
                } catch (IOException e) {
                    // Thrown if IpPort is an invalid address
                    LOG.error("IOException", e);
                }
            }
            return Result.NOT_FOUND;
        }
    }
    
}
