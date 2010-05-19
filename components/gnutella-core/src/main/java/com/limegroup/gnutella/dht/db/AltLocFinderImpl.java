package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import net.sf.fmj.utility.ExceptionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.dht2.DHTManager;

/**
 * Default implementation of {@link AltLocFinder}, uses the DHT to find
 * alternate locations.
 */
@Singleton
public class AltLocFinderImpl implements AltLocFinder {

    private static final Log LOG = LogFactory.getLog(AltLocFinderImpl.class);
    
    private final DHTManager dhtManager;

    private final AlternateLocationFactory alternateLocationFactory;

    private final AltLocManager altLocManager;

    private final PushEndpointService pushEndpointManager;
    
    @Inject
    public AltLocFinderImpl(DHTManager dhtManager, 
            AlternateLocationFactory alternateLocationFactory, 
            AltLocManager altLocManager, 
            @Named("pushEndpointManager") PushEndpointService pushEndpointManager) {
        this.dhtManager = dhtManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.altLocManager = altLocManager;
        this.pushEndpointManager = pushEndpointManager;
    }

    @Override
    public DHTFuture<AlternateLocation[]> findAltLocs(final URN urn) {
        final Object lock = new Object();
        
        synchronized (lock) {
            final DHTValueFuture<AlternateLocation[]> future 
                = new DHTValueFuture<AlternateLocation[]>();
            
            KUID key = KUIDUtils.toKUID(urn);
            EntityKey lookupKey = EntityKey.createEntityKey(
                    key, AltLocValue2.ALT_LOC);
            
            final DHTFuture<ValueEntity[]> lookup 
                = dhtManager.getAll(lookupKey);
            
            lookup.addFutureListener(new EventListener<FutureEvent<ValueEntity[]>>() {
                @Override
                public void handleEvent(FutureEvent<ValueEntity[]> event) {
                    try {
                        switch (event.getType()) {
                            case SUCCESS:
                                onSuccess(event.getResult());
                                break;
                            case EXCEPTION:
                                onException(event.getException());
                                break;
                            case CANCELLED:
                                onCancellation();
                                break;
                        }
                    } catch (Throwable t) {
                        onException(t);
                        ExceptionUtils.reportOrReturn(t);
                    }
                }
                
                private void onSuccess(ValueEntity[] entities) {
                    try {
                        AlternateLocation[] locations 
                            = createAlternateLocations(urn, entities);
                        future.setValue(locations);
                    } catch (NoSuchElementException err) {
                        onException(err);
                    } catch (IOException err) {
                        onException(err);
                    }
                }
                
                private void onException(Throwable t) {
                    future.setException(t);
                }
                
                private void onCancellation() {
                    future.cancel(true);
                }
            });
            
            future.addFutureListener(new EventListener<FutureEvent<AlternateLocation[]>>() {
                @Override
                public void handleEvent(FutureEvent<AlternateLocation[]> event) {
                    lookup.cancel(true);
                }
            });
            
            return future;
        }
    }
    
    private DHTFuture<PushEndpoint> findPushAltLocs(GUID guid, final URN urn, 
            final DHTValueEntity entity) {
        
        DHTFuture<PushEndpoint> future 
            = pushEndpointManager.findPushEndpoint(guid);
        future.addFutureListener(new EventListener<FutureEvent<PushEndpoint>>() {
            @Override
            public void handleEvent(FutureEvent<PushEndpoint> event) {
                if (event.getType() == Type.SUCCESS) {
                    onSuccess(event.getResult());
                }
            }
            
            private void onSuccess(PushEndpoint endpoint) {
                // So we made a lookup for AltLocs, the found AltLoc was 
                // firewalled and we made a lookup for its PushProxies.
                // In any case the creator of both values should be the 
                // same Node!
                InetAddress creatorAddress = ((InetSocketAddress)entity.getCreator().getContactAddress()).getAddress();
                InetAddress externalAddress = endpoint.getInetAddress();
                
                if (externalAddress != null 
                        && !externalAddress.equals(creatorAddress)) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Creator of " + entity + " and found " 
                                + endpoint + " do not match!");
                    }
                    return;
                }
                
                AlternateLocation alternateLocation 
                    = alternateLocationFactory.createPushAltLoc(endpoint, urn);
                altLocManager.add(alternateLocation, AltLocFinderImpl.this);
            }
        });
        
        return future;
    }
    
    private AlternateLocation[] createAlternateLocations(URN urn, 
            ValueEntity... entities) throws NoSuchElementException, IOException {
        
        List<AlternateLocation> locations 
            = new ArrayList<AlternateLocation>();
        
        for (ValueEntity entity : entities) {
            for (DHTValueEntity values : entity.getEntities()) {
                DHTValue value = values.getValue();
                
                DHTValueType valueType = value.getValueType();
                if (!valueType.equals(AltLocValue2.ALT_LOC)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Not a PushProxy: " + value);
                    }
                    continue;
                }
                
                AltLocValue2 altLoc 
                    = new AltLocValue2.Impl(value);
                if (altLoc.isFirewalled()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Location is Firewalled: " + altLoc);
                    }
                    
                    if (DHTSettings.ENABLE_PUSH_PROXY_QUERIES.getValue()) {
                        GUID guid = new GUID(altLoc.getGUID());
                        findPushAltLocs(guid, urn, values);
                    }
                    
                    continue;
                }
                
                Contact creator = values.getCreator();
                InetAddress addr = ((InetSocketAddress)
                        creator.getContactAddress()).getAddress();
                
                IpPort ipp = new IpPortImpl(addr, altLoc.getPort());
                Connectable c = new ConnectableImpl(
                        ipp, altLoc.supportsTLS());

                long fileSize = altLoc.getFileSize();
                byte[] ttroot = altLoc.getRootHash();
                
                AlternateLocation location = alternateLocationFactory
                    .createDirectDHTAltLoc(c, urn, fileSize, ttroot);
                
                altLocManager.add(location, AltLocFinderImpl.this);
                locations.add(location);
            }
        }
        
        if (!locations.isEmpty()) {
            return locations.toArray(new AlternateLocation[0]);
        }
        
        throw new NoSuchElementException(
                "ValueEntities=" + Arrays.toString(entities));
    }
}
