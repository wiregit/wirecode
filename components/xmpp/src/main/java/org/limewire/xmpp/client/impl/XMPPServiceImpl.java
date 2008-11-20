package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
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
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class XMPPServiceImpl implements Service, XMPPService, EventListener<AddressEvent> {

    private static final Log LOG = LogFactory.getLog(XMPPServiceImpl.class);
    
    public static final String LW_SERVICE_NS = "http://www.limewire.org/";
    
    private final CopyOnWriteArrayList<XMPPConnectionImpl> connections;
    private final Provider<EventBroadcaster<RosterEvent>> rosterBroadcaster;
    private final Provider<EventBroadcaster<FileOfferEvent>> fileOfferBroadcaster;
    private final Provider<EventBroadcaster<LibraryChangedEvent>> libraryChangedBroadcaster;
    private final ListenerSupport<FriendPresenceEvent> presenceSupport;
    private final AddressFactory addressFactory;
    private XMPPErrorListener errorListener;
    private Provider<EventBroadcaster<XMPPConnectionEvent>> connectionBroadcaster;
    private AddressEvent lastEvent;
    private final XMPPAuthenticator authenticator;

    @Inject
    XMPPServiceImpl(Provider<List<XMPPConnectionConfiguration>> configurations,
                    Provider<EventBroadcaster<RosterEvent>> rosterBroadcaster,
                    Provider<EventBroadcaster<FileOfferEvent>> fileOfferBroadcaster,
                    Provider<EventBroadcaster<LibraryChangedEvent>> libraryChangedBroadcaster,
                    Provider<EventBroadcaster<XMPPConnectionEvent>> connectionBroadcaster,
                    AddressFactory addressFactory, XMPPAuthenticator authenticator,
                    ListenerSupport<FriendPresenceEvent> presenceSupport) {
        this.rosterBroadcaster = rosterBroadcaster;
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        this.libraryChangedBroadcaster = libraryChangedBroadcaster;
        this.connectionBroadcaster = connectionBroadcaster;
        this.presenceSupport = presenceSupport;
        this.addressFactory = addressFactory;
        this.authenticator = authenticator;
        this.connections = new CopyOnWriteArrayList<XMPPConnectionImpl>();
        for(XMPPConnectionConfiguration configuration : configurations.get()) {
            addConnectionConfiguration(configuration);
        }
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Inject
    void register(ListenerSupport<AddressEvent> registry) {
        registry.addListener(this);
    }

//    public void register(RosterListener rosterListener) {
//        this.rosterListener = rosterListener;
//        for(XMPPConnectionImpl connection : connections) {
//            connection.addRosterListener(rosterListener);
//        }
//    }

    public void setXmppErrorListener(XMPPErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void initialize() {
        initializeConfigurations();
    }

    /**
     * Logs into the xmpp service specified in the <code>XMPPConfiguration</code>
     */
    @Asynchronous 
    public void start() {
        for(XMPPConnection connection : connections) {
            if(connection.getConfiguration().isAutoLogin()) {
                login(connection);
            }
        }        
    }

    private void login(XMPPConnection connection) {
        try {
            // TODO async
            connection.login();
        } catch (XMPPException e) {
            LOG.error(e.getMessage(), e);
            // TODO connection.getConfiguration().getErrorListener()
            errorListener.error(e);
        }
    }

    /**
     * Disconnects from the xmpp server
     */
    @Asynchronous
    public void stop() {
        for(XMPPConnection connection : connections) {
            // TODO async
            connection.logout();
        }
    }

    public String getServiceName() {
        return "XMPP";
    }

    public List<XMPPConnection> getConnections() {
        List<XMPPConnection> copy = new ArrayList<XMPPConnection>();
        copy.addAll(connections);
        return Collections.unmodifiableList(copy);
    }

    private void addConnectionConfiguration(XMPPConnectionConfiguration configuration) {
        synchronized (this) {
            XMPPConnectionImpl connection = new XMPPConnectionImpl(configuration, rosterBroadcaster
                    .get(), fileOfferBroadcaster.get(), libraryChangedBroadcaster.get(),
                    connectionBroadcaster.get(), addressFactory, authenticator, presenceSupport);
            connections.add(connection);
        }
    }
    
    void initializeConfigurations() {
        synchronized(this) {
            for(XMPPConnection connection : connections) {
                XMPPConnectionImpl impl = (XMPPConnectionImpl)connection;
                impl.initialize();
                if(lastEvent != null) {
                    impl.handleEvent(lastEvent);
                }
            }
        }
    }

    public void handleEvent(AddressEvent event) {
        LOG.debugf("handling address event: {0}", event.getSource().toString());
        synchronized (this) {
            for(XMPPConnectionImpl connection : connections) {
                connection.handleEvent(event);
            }
            lastEvent = event;
        }
    }
}
