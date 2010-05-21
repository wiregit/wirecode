package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;

import net.sf.fmj.utility.ExceptionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
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
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.dht2.DHTManager;

/**
 * Searches for {@link PushEndpoint push endpoints} in the DHT.
 */
@Singleton
public class DHTPushEndpointFinder implements PushEndpointService {

    private static final Log LOG 
        = LogFactory.getLog(DHTPushEndpointFinder.class);
    
    private final PushEndpointFactory pushEndpointFactory;
    
    private final DHTManager dhtManager;

    @Inject
    public DHTPushEndpointFinder(DHTManager dhtManager, 
            PushEndpointFactory pushEndpointFactory) {
        this.dhtManager = dhtManager;
        this.pushEndpointFactory = pushEndpointFactory;
    }
    
    @Override
    public DHTFuture<PushEndpoint> findPushEndpoint(final GUID guid) {
        
        final Object lock = new Object();
        
        synchronized (lock) {
            final DHTValueFuture<PushEndpoint> future 
                = new DHTValueFuture<PushEndpoint>();
            
            KUID key = KUIDUtils.toKUID(guid);
            EntityKey lookupKey = EntityKey.createEntityKey(
                    key, PushProxiesValue.PUSH_PROXIES);
            
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
                        PushEndpoint pe = createPushEndpoint(
                                pushEndpointFactory, guid, entities);
                        future.setValue(pe);
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
            
            future.addFutureListener(new EventListener<FutureEvent<PushEndpoint>>() {
                @Override
                public void handleEvent(FutureEvent<PushEndpoint> event) {
                    lookup.cancel(true);
                }
            });
            
            return future;
        }
    }
    
    public static PushEndpoint createPushEndpoint(PushEndpointFactory factory, 
            GUID guid, ValueEntity... entities) throws NoSuchElementException, IOException {
        return createPushEndpoint(factory, guid.bytes(), entities);
    }
    
    public static PushEndpoint createPushEndpoint(PushEndpointFactory factory, 
            byte[] guid, ValueEntity... entities) throws NoSuchElementException, IOException {
        
        for (ValueEntity entity : entities) {
            for (DHTValueEntity values : entity.getEntities()) {
                DHTValue value = values.getValue();
                
                DHTValueType valueType = value.getValueType();
                if (!valueType.equals(PushProxiesValue.PUSH_PROXIES)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Not a PushProxy: " + value);
                    }
                    continue;
                }
                
                PushProxiesValue pushProxies 
                    = new DefaultPushProxiesValue(value);
                if (!Arrays.equals(guid, pushProxies.getGUID())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Not for GUID: " + guid 
                                + ", pushProxies=" + pushProxies);
                    }
                    continue;
                }
                
                Contact creator = values.getCreator();
                InetAddress addr = ((InetSocketAddress)
                        creator.getContactAddress()).getAddress();
                
                Set<? extends IpPort> proxies = pushProxies.getPushProxies();
                byte features = pushProxies.getFeatures();
                int fwtVersion = pushProxies.getFwtVersion();
                IpPort ipp = new IpPortImpl(addr, pushProxies.getPort());
                PushEndpoint pe = factory.createPushEndpoint(
                        guid, proxies, features, fwtVersion, ipp);
                
                return pe;
            }
        }
        
        throw new NoSuchElementException("ValueEntities=" + Arrays.toString(entities));
    }
}
