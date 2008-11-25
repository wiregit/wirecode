package org.limewire.xmpp.client.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;


@Singleton
public class XMPPServiceImpl implements Service, XMPPService, EventListener<AddressEvent> {

    private static final Log LOG = LogFactory.getLog(XMPPServiceImpl.class);

    public static final String LW_SERVICE_NS = "http://www.limewire.org/";

    private final Provider<EventBroadcaster<RosterEvent>> rosterBroadcaster;
    private final Provider<EventBroadcaster<FileOfferEvent>> fileOfferBroadcaster;
    private final Provider<EventBroadcaster<LibraryChangedEvent>> libraryChangedBroadcaster;
    private final AddressFactory addressFactory;
    private final Provider<EventMulticaster<XMPPConnectionEvent>> connectionBroadcaster;
    private final XMPPAuthenticator authenticator;
    private final EventMulticaster<FeatureEvent> featureSupport;
    private final List<XMPPConnectionImpl> connections;
    
    private AddressEvent lastAddressEvent;
    private boolean multipleConnectionsAllowed;

    @Inject
    public XMPPServiceImpl(Provider<EventBroadcaster<RosterEvent>> rosterBroadcaster,
            Provider<EventBroadcaster<FileOfferEvent>> fileOfferBroadcaster,
            Provider<EventBroadcaster<LibraryChangedEvent>> libraryChangedBroadcaster,
            Provider<EventMulticaster<XMPPConnectionEvent>> connectionBroadcaster,
            AddressFactory addressFactory, XMPPAuthenticator authenticator,
            EventMulticaster<FeatureEvent> featureSupport) {
        this.rosterBroadcaster = rosterBroadcaster;
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        this.libraryChangedBroadcaster = libraryChangedBroadcaster;
        this.connectionBroadcaster = connectionBroadcaster;
        this.addressFactory = addressFactory;
        this.authenticator = authenticator;
        this.featureSupport = featureSupport;
        this.connections = new CopyOnWriteArrayList<XMPPConnectionImpl>();
        this.multipleConnectionsAllowed = false;
        this.connectionBroadcaster.get().addListener(new ReconnectionManager());
    }

    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    @Inject
    void register(ListenerSupport<AddressEvent> registry) {
        registry.addListener(this);
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
    }

    /**
     * Logs out all existing connections.
     */
    @Asynchronous
    @Override
    public void stop() {
        logout();
    }

    @Override
    public String getServiceName() {
        return "XMPP";
    }

    @Override
    public XMPPConnection login(XMPPConnectionConfiguration configuration) throws XMPPException {
        return login(configuration, false);
    }

    public XMPPConnection login(XMPPConnectionConfiguration configuration, boolean isReconnect) throws XMPPException {
        synchronized (this) {
            if(!multipleConnectionsAllowed) {
                XMPPConnection activeConnection = getActiveConnection();
                if(isReconnect) {
                    if(activeConnection != null) {
                        return activeConnection;
                    }
                } else {
                    if(activeConnection != null && activeConnection.getConfiguration().equals(configuration)) {
                        return activeConnection;
                    } else {
                        logout();
                    }    
                }                
            }
            
            XMPPConnectionImpl connection = new XMPPConnectionImpl(configuration,
                    rosterBroadcaster.get(), fileOfferBroadcaster.get(),
                    libraryChangedBroadcaster.get(), connectionBroadcaster.get(),
                    addressFactory, authenticator, featureSupport);
            if(lastAddressEvent != null) {
                connection.handleEvent(lastAddressEvent);
            }
            try {            
                connection.login();
                connections.add(connection);
                return connection;
            } catch(XMPPException e) {
                LOG.debug(e.getMessage(), e);
                throw new XMPPException(e);
            }
        }
    }
    
    @Override
    public boolean isLoggedIn() {
        for(XMPPConnection connection : connections) {
            if(connection.isLoggedIn()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean isLoggingIn() {
        for(XMPPConnection connection : connections) {
            if(connection.isLoggingIn()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void logout() {
        for(XMPPConnection connection : connections) {
            if(connection.isLoggedIn()) {
                connection.logout();
            }
        }
        connections.clear();
    }

    @Override
    public XMPPConnection getActiveConnection() {
        if(connections.isEmpty()) {
            return null;
        } else {
            try {
                return connections.get(0);
            } catch(IndexOutOfBoundsException ioobe) {
                return null; // possible because connections is CoW
            }
        }
    }

    @Override
    public void handleEvent(AddressEvent event) {
        LOG.debugf("handling address event: {0}", event.getSource().toString());
        synchronized(this) {
            for(XMPPConnectionImpl connection : connections)
                connection.handleEvent(event);
            lastAddressEvent = event;
        }
    }

    // Only for testing
    void setMultipleConnectionsAllowed(boolean allowed) {
        multipleConnectionsAllowed = allowed;
    }

    // Only for testing
    List<? extends XMPPConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    } 
    
    private class ReconnectionManager implements EventListener<XMPPConnectionEvent> {
        @Override
        @BlockingEvent
        public void handleEvent(XMPPConnectionEvent event) {
            if(event.getType() == XMPPConnectionEvent.Type.DISCONNECTED && event.getData() != null) {
                XMPPConnection connection = event.getSource();
                XMPPConnectionConfiguration configuration = connection.getConfiguration();
                synchronized (XMPPServiceImpl.this) {
                    connections.remove(connection);
                }
                connection = null;
                long sleepTime = 10000;
                while(connection == null) {
                    try {
                        LOG.debugf("attempting to reconnect to {0} ..." + configuration.getServiceName());
                        connection = login(configuration, true);
                    } catch (XMPPException e) {
                    }
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        
                    }
//                  if(sleepTime < (Long.MAX_VALUE / 2)) {
//                      sleepTime *= 2;
//                  }
                }
            }
        }
    }
}
