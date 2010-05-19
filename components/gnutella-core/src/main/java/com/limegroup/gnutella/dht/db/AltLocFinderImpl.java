package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.ValueResponseHandler.NoSuchValueException;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.settings.LookupSettings;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.util.EventUtils;
import org.limewire.util.ExceptionUtils;

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
        AlternateLocationProcess process 
            = new AlternateLocationProcess(urn);
        
        long timeout = LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue();
        return dhtManager.getMojitoDHT().submit(process, 
                timeout, TimeUnit.MILLISECONDS);
    }
    
    private class AlternateLocationProcess 
            implements AsyncProcess<AlternateLocation[]> {
        
        private final URN urn;
        
        private final List<AlternateLocation> locations 
            = new ArrayList<AlternateLocation>();
        
        private final List<ProxyHandle> proxies 
            = new ArrayList<ProxyHandle>();
        
        private final List<DHTFuture<?>> futures 
            = new ArrayList<DHTFuture<?>>();
        
        private volatile DHTFuture<AlternateLocation[]> future = null;
        
        private DHTFuture<ValueEntity[]> lookup = null;
        
        public AlternateLocationProcess(URN urn) {
            this.urn = urn;
        }
        
        private void uncaughtException(Throwable t) {
            onException(t);
            ExceptionUtils.reportOrReturn(t);
        }
        
        private void onException(Throwable t) {
            future.setException(t);
        }
        
        private void onCancellation() {
            future.cancel(true);
        }
        
        private void onComplete() {
            final AlternateLocation[] locations 
                = this.locations.toArray(new AlternateLocation[0]);
            
            if (locations.length == 0) {
                future.setException(new NoSuchValueException(null));
                return;
            }
            
            future.setValue(locations);
            
            Runnable event = new Runnable() {
                @Override
                public void run() {
                    for (AlternateLocation location : locations) {
                        altLocManager.add(location, AltLocFinderImpl.this);
                    }
                }
            };
            
            EventUtils.fireEvent(event);
        }
        
        @Override
        public void start(DHTFuture<AlternateLocation[]> future) {
            this.future = future;
            
            synchronized (future) {
                future.addFutureListener(new EventListener<FutureEvent<AlternateLocation[]>>() {
                    @Override
                    public void handleEvent(FutureEvent<AlternateLocation[]> event) {
                        stop();
                    }
                });
                start();
            }
        }
        
        private void start() {
            synchronized (future) {
                try {
                    doLookup();
                } catch (Throwable t) {
                    uncaughtException(t);
                }
            }
        }
        
        private void stop() {
            synchronized (future) {
                if (lookup != null) {
                    lookup.cancel(true);
                }
                
                for (DHTFuture<?> future : futures) {
                    future.cancel(true);
                }
                futures.clear();
            }
        }
        
        private void doLookup() {
            KUID key = KUIDUtils.toKUID(urn);
            EntityKey lookupKey = EntityKey.createEntityKey(
                    key, AltLocValue2.ALT_LOC);
            
            lookup = dhtManager.getAll(lookupKey);
            lookup.addFutureListener(new EventListener<FutureEvent<ValueEntity[]>>() {
                @Override
                public void handleEvent(FutureEvent<ValueEntity[]> event) {
                    onLookup(event);
                }
            });
        }
        
        private void onLookup(FutureEvent<ValueEntity[]> event) {
            synchronized (future) {
                if (future.isDone()) {
                    return;
                }
                
                try {
                    switch (event.getType()) {
                        case SUCCESS:
                            onLookup(event.getResult());
                            break;
                        case EXCEPTION:
                            onException(event.getException());
                            break;
                        case CANCELLED:
                            onCancellation();
                            break;
                    }
                } catch (IOException err) {
                    onException(err);
                    
                } catch (Throwable t) {
                    uncaughtException(t);
                }
            }
        }
        
        private void onLookup(ValueEntity[] entities) throws IOException {
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
                            proxies.add(new ProxyHandle(values, guid));
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
            
            doNext();
        }
        
        /**
         * This method is called recursively!
         */
        private void doNext() {
            synchronized (future) {
                if (future.isDone()) {
                    return;
                }
                
                // We're done if there is nothing left to be looked up!
                if (proxies.isEmpty()) {
                    onComplete();
                    return;
                }
                
                ProxyHandle handle = proxies.remove(0);
                final DHTValueEntity entity = handle.entity;
                
                DHTFuture<PushEndpoint> future 
                    = pushEndpointManager.findPushEndpoint(handle.guid);
                
                future.addFutureListener(new EventListener<FutureEvent<PushEndpoint>>() {
                    @Override
                    public void handleEvent(FutureEvent<PushEndpoint> event) {
                        onPushEnpoint(entity, event);
                    }
                });
                
                futures.add(future);
            }
        }
        
        private void onPushEnpoint(DHTValueEntity entity, 
                FutureEvent<PushEndpoint> event) {
            synchronized (future) {
                if (future.isDone()) {
                    return;
                }
                
                try {
                    if (event.getType() == Type.SUCCESS) {
                        onPushEndpoint(entity, event.getResult());
                    }
                    
                    doNext();
                } catch (Throwable t) {
                    uncaughtException(t);
                }
            }
        }
        
        private void onPushEndpoint(DHTValueEntity entity, PushEndpoint endpoint) {
            // So we made a lookup for AltLocs, the found AltLoc was firewalled 
            // and we made a lookup for its PushProxies. In any case the creator 
            // of both values should be the same Node!
            InetAddress creatorAddress = ((InetSocketAddress)
                    entity.getCreator().getContactAddress()).getAddress();
            InetAddress externalAddress = endpoint.getInetAddress();
            
            if (externalAddress != null 
                    && !externalAddress.equals(creatorAddress)) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Creator of " + entity + " and found " 
                            + endpoint + " do not match!");
                }
                return;
            }
            
            AlternateLocation location 
                = alternateLocationFactory.createPushAltLoc(endpoint, urn);
            locations.add(location);
        }
    }
    
    private static class ProxyHandle {
        
        private final DHTValueEntity entity;
        
        private final GUID guid;
        
        public ProxyHandle(DHTValueEntity entity, GUID guid) {
            this.entity = entity;
            this.guid = guid;
        }
    }
}
