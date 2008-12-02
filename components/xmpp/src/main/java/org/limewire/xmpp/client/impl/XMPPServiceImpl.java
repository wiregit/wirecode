package org.limewire.xmpp.client.impl;

import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectRequestEvent;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.ConnectRequestSender;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectRequestIQ;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;


@Singleton
public class XMPPServiceImpl implements Service, XMPPService, EventListener<AddressEvent>, ConnectRequestSender {

    private static final Log LOG = LogFactory.getLog(XMPPServiceImpl.class);

    public static final String LW_SERVICE_NS = "http://www.limewire.org/";

    private final Provider<EventBroadcaster<RosterEvent>> rosterBroadcaster;
    private final Provider<EventBroadcaster<FileOfferEvent>> fileOfferBroadcaster;
    private final Provider<EventBroadcaster<LibraryChangedEvent>> libraryChangedBroadcaster;
    private final AddressFactory addressFactory;
    private XMPPErrorListener errorListener;
    private Provider<EventBroadcaster<XMPPConnectionEvent>> connectionBroadcaster;
    private AddressEvent lastAddressEvent;
    private final XMPPAuthenticator authenticator;
    private final ListenerSupport<FriendPresenceEvent> presenceSupport;
    private LinkedList<XMPPConnectionImpl> connections;
    private boolean multipleConnectionsAllowed;

    private final EventBroadcaster<ConnectRequestEvent> connectRequestEventBroadcaster;

    @Inject
    public XMPPServiceImpl(Provider<EventBroadcaster<RosterEvent>> rosterBroadcaster,
            Provider<EventBroadcaster<FileOfferEvent>> fileOfferBroadcaster,
            Provider<EventBroadcaster<LibraryChangedEvent>> libraryChangedBroadcaster,
            Provider<EventBroadcaster<XMPPConnectionEvent>> connectionBroadcaster,
            AddressFactory addressFactory, XMPPAuthenticator authenticator,
            ListenerSupport<FriendPresenceEvent> presenceSupport,
            EventBroadcaster<ConnectRequestEvent> connectRequestEventBroadcaster) {
        this.rosterBroadcaster = rosterBroadcaster;
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        this.libraryChangedBroadcaster = libraryChangedBroadcaster;
        this.connectionBroadcaster = connectionBroadcaster;
        this.addressFactory = addressFactory;
        this.authenticator = authenticator;
        this.presenceSupport = presenceSupport;
        this.connectRequestEventBroadcaster = connectRequestEventBroadcaster;
        connections = new LinkedList<XMPPConnectionImpl>();
        multipleConnectionsAllowed = false;
    }

    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    @Inject
    void register(ListenerSupport<AddressEvent> registry) {
        registry.addListener(this);
    }

    public void setXmppErrorListener(XMPPErrorListener errorListener) {
        this.errorListener = errorListener;
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
    public synchronized void login(XMPPConnectionConfiguration configuration) {
        if(!multipleConnectionsAllowed)
            logout();
        XMPPConnectionImpl connection = new XMPPConnectionImpl(configuration,
                rosterBroadcaster.get(), fileOfferBroadcaster.get(),
                libraryChangedBroadcaster.get(), connectionBroadcaster.get(),
                addressFactory, authenticator, presenceSupport, connectRequestEventBroadcaster);
        connection.initialize();
        // Give the new connection the latest information about our IP address
        // and firewall status
        if(lastAddressEvent != null)
            connection.handleEvent(lastAddressEvent);
        try {
            connection.login();
            connections.add(connection);
        } catch(XMPPException e) {
            LOG.error(e.getMessage(), e);
            errorListener.error(e);
        }
    }

    @Override
    public synchronized void logout() {
        for(XMPPConnection connection : connections) {
            if(connection.isLoggedIn())
                connection.logout();
        }
        connections.clear();
    }

    @Override
    public synchronized XMPPConnection getLoggedInConnection() {
        return connections.peek();
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
    synchronized List<? extends XMPPConnection> getConnections() {
        // Return a copy in case we modify the list while the caller's using it
        return new LinkedList<XMPPConnectionImpl>(connections);
    }

    @Override
    public void send(String userId, Connectable address, GUID clientGuid, int supportedFWTVersion) {
        XMPPConnectionImpl connection = connections.peek();
        if (connection == null) {
            return;
        }
        ConnectRequestIQ connectRequest = new ConnectRequestIQ(address, clientGuid, supportedFWTVersion);
        connectRequest.setTo(userId);
        connectRequest.setFrom(connection.getLocalJid());
        connection.sendPacket(connectRequest);
    }    
}
