package com.limegroup.gnutella.dht.db;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventListenerList.EventListenerListContext;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;

import com.google.inject.Inject;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

/**
 * Searches for {@link PushEndpoint push endpoints} in the DHT.
 */
public class DHTAddressFinderImpl implements AddressFinder {

    private static final Log LOG = LogFactory.getLog(DHTAddressFinderImpl.class);
    
    private final PushEndpointFactory pushEndpointFactory;
    private final DHTManager dhtManager;

    @Inject
    public DHTAddressFinderImpl(DHTManager dhtManager, PushEndpointFactory pushEndpointFactory) {
        this.dhtManager = dhtManager;
        this.pushEndpointFactory = pushEndpointFactory;
    }
    
    
    @Override
    public ListeningFuture<Address> search(GUID guid) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("starting dht lookup for guid: " + guid);
        }
        
        KUID key = KUIDUtils.toKUID(guid);
        EntityKey lookupKey = EntityKey.createEntityKey(key, AbstractPushProxiesValue.PUSH_PROXIES);        
        
        DHTFuture<FindValueResult> future = dhtManager.get(lookupKey);
        if(future != null) {                        
            return new DelegatingListeningFuture(guid, future);
        } else {
            return new SimpleFuture<Address>(new IOException("no dht currently"));
        }               
    }

    private class DelegatingListeningFuture implements ListeningFuture<Address> {

        private final DHTFuture<FindValueResult> delegate;
        private final EventListenerListContext listenerContext = new EventListenerListContext();
        private final GUID guid;

        public DelegatingListeningFuture(GUID guid, DHTFuture<FindValueResult> delegate) {
            this.guid = guid;
            this.delegate = delegate;
        }

        @Override
        public void addFutureListener(final EventListener<FutureEvent<Address>> listener) {
            if (isDone()) {
                EventListenerList.dispatch(listener, FutureEvent.createEvent(this), listenerContext);
            } else {
                delegate.addDHTFutureListener(new DHTFutureListener<FindValueResult> () {
                    public void handleCancellationException(java.util.concurrent.CancellationException e) {
                        EventListenerList.dispatch(listener, FutureEvent.createEvent(DelegatingListeningFuture.this), listenerContext);
                    };
                    public void handleExecutionException(ExecutionException e) {
                        EventListenerList.dispatch(listener, FutureEvent.createEvent(DelegatingListeningFuture.this), listenerContext);
                    };
                    public void handleFutureSuccess(FindValueResult result) {
                        EventListenerList.dispatch(listener, FutureEvent.createEvent(DelegatingListeningFuture.this), listenerContext);
                    };
                    public void handleInterruptedException(InterruptedException e) {
                        EventListenerList.dispatch(listener, FutureEvent.createEvent(DelegatingListeningFuture.this), listenerContext);
                    };
                });
            }
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public Address get() throws InterruptedException, ExecutionException {
            FindValueResult result = delegate.get();
            return convertResult(result);
        }

        @Override
        public Address get(long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            return convertResult(delegate.get(timeout, unit));
        }

        private Address handleDHTValueEntity(DHTValueEntity entity) {
            DHTValue value = entity.getValue();
            if (!(value instanceof PushProxiesValue)) {
                return null;
            }
            Contact creator = entity.getCreator();
            InetAddress addr = ((InetSocketAddress)creator.getContactAddress()).getAddress();
            
            PushProxiesValue pushProxies = (PushProxiesValue)value;
            LOG.debugf("received push proxies value {0}", pushProxies);
            
            if (!guid.equals(new GUID(pushProxies.getGUID()))) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("The AltLoc and PushProxy GUIDs do not match!");
                }
                return null;
            }
            
            Set<? extends IpPort> proxies = pushProxies.getPushProxies();
            byte features = pushProxies.getFeatures();
            int fwtVersion = pushProxies.getFwtVersion();
            IpPort externalAddress = new IpPortImpl(addr, pushProxies.getPort());
            // if the external address is the same as the push proxy, it's a non-firewalled source
            if (proxies.size() == 1 && proxies.contains(externalAddress)) {
                LOG.debugf("peer is not firewalled: {0}", pushProxies);
                return new ConnectableImpl(externalAddress, false);
            } else {
                PushEndpoint pushEndpoint = pushEndpointFactory.createPushEndpoint(guid.bytes(), proxies, features, fwtVersion, externalAddress);
                // TODO fberger does this make sense?
                pushEndpoint.updateProxies(true);
                LOG.debugf("push endpoint found: {0}", pushEndpoint);
                return pushEndpoint;
            }
        }
        
        private Address convertResult(FindValueResult result) throws ExecutionException {
            for (DHTValueEntity entity : result.getEntities()) {
                Address address = handleDHTValueEntity(entity);
                if (address != null) {
                    return address;
                }
            }
            for (EntityKey entityKey : result.getEntityKeys()) {
                if (entityKey.getDHTValueType().equals(AbstractPushProxiesValue.PUSH_PROXIES)) {
                    DHTFuture<FindValueResult> future = dhtManager.get(entityKey);
                    if(future != null) {
                        try {                        
                            // TODO make this a non-blocking call
                            FindValueResult resultFromKey = future.get();
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("result from second lookup: " + resultFromKey);
                            }
                            if (resultFromKey.isSuccess()) {
                                for (DHTValueEntity entity : resultFromKey.getEntities()) {
                                    Address address = handleDHTValueEntity(entity);
                                    if (address != null) {
                                        return address;
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
            }
            throw new ExecutionException(new IOException("no valid results"));
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }
    }
}
