package org.limewire.xmpp.client.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatStateManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.io.Address;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventRebroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.DebugRunnable;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.client.impl.features.FileOfferInitializer;
import org.limewire.xmpp.client.impl.features.LibraryChangedNotifierFeatureInitializer;
import org.limewire.xmpp.client.impl.features.LimewireFeatureInitializer;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListener;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQProvider;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListener;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQ;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQListener;

public class XMPPConnectionImpl implements org.limewire.xmpp.api.client.XMPPConnection, EventListener<AddressEvent>, FeatureRegistry {

    private static final Log LOG = LogFactory.getLog(XMPPConnectionImpl.class);

    private final XMPPConnectionConfiguration configuration;
    private final EventBroadcaster<FileOfferEvent> fileOfferBroadcaster;
    private final EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster;
    private final EventBroadcaster<XMPPConnectionEvent> connectionBroadcaster;
    private final EventListenerList<RosterEvent> rosterListeners;
    private final AddressFactory addressFactory;
    private volatile org.jivesoftware.smack.XMPPConnection connection;

    private final Map<String, UserImpl> users;
    private volatile AddressIQListener addressIQListener;
    private volatile AddressEvent lastEvent;
    private final XMPPAuthenticator authenticator;
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);
    private final EventMulticaster<FeatureEvent> featureSupport;
    private final SmackConnectionListener smackConnectionListener;
    private final Map<URI, FeatureInitializer> featureInitializerMap;

    XMPPConnectionImpl(XMPPConnectionConfiguration configuration,
                       EventBroadcaster<RosterEvent> rosterBroadcaster,
                       EventBroadcaster<FileOfferEvent> fileOfferBroadcaster,
                       EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster,
                       EventBroadcaster<XMPPConnectionEvent> connectionBroadcaster,
                       AddressFactory addressFactory, XMPPAuthenticator authenticator,
                       EventMulticaster<FeatureEvent> featureSupport) {
        this.configuration = configuration;
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        this.libraryChangedEventEventBroadcaster = libraryChangedEventEventBroadcaster;
        this.connectionBroadcaster = connectionBroadcaster;
        this.addressFactory = addressFactory;
        this.authenticator = authenticator;
        this.rosterListeners = new EventListenerList<RosterEvent>();
        if(configuration.getRosterListener() != null) {
            this.rosterListeners.addListener(configuration.getRosterListener());
        }
        this.rosterListeners.addListener(new EventRebroadcaster<RosterEvent>(rosterBroadcaster));
        this.users = new TreeMap<String, UserImpl>(String.CASE_INSENSITIVE_ORDER);
        this.featureSupport = featureSupport;
        this.featureInitializerMap = new ConcurrentHashMap<URI, FeatureInitializer>();
        smackConnectionListener = new SmackConnectionListener();
    }

    public void setMode(Presence.Mode mode) {
        connection.sendPacket(getPresenceForMode(mode));
    }

    private Packet getPresenceForMode(Presence.Mode mode) {
        org.jivesoftware.smack.packet.Presence presence = new org.jivesoftware.smack.packet.Presence(
                org.jivesoftware.smack.packet.Presence.Type.available);
        presence.setMode(org.jivesoftware.smack.packet.Presence.Mode.valueOf(mode.name()));
        return presence;
    }

    public XMPPConnectionConfiguration getConfiguration() {
        return configuration;
    }

    public void login() throws XMPPException {
        connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTING));
        loggingIn.set(true);
        synchronized (this) {
            try {
                org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(smackConnectionListener);
                org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
                connection = new org.jivesoftware.smack.XMPPConnection(getConnectionConfig(configuration));
                connection.addRosterListener(new RosterListenerImpl(connection));
                if (LOG.isInfoEnabled())
                    LOG.info("connecting to " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + "...");
                connection.connect();
                LOG.info("connected.");
                if (LOG.isInfoEnabled())
                    LOG.infof("logging in " + configuration.getUsername() + " with resource: " + configuration.getResource());
                connection.login(configuration.getUsername(), configuration.getPassword(), configuration.getResource());
                LOG.info("logged in.");
                connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTED));
            } catch (org.jivesoftware.smack.XMPPException e) {
                connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECT_FAILED, e));
                if(connection != null && connection.isConnected()) {
                    connection.disconnect();
                }
                org.jivesoftware.smack.XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                throw new XMPPException(e);
            } finally {
                loggingIn.set(false);
            }
        }
    }

    @Override
    public boolean isLoggingIn() {
        return loggingIn.get();
    }

    public void logout() {
        synchronized (this) {
            if(connection != null && connection.isAuthenticated()) {
                LOG.infof("disconnecting from {0} at {1}:{2} ...", configuration.getServiceName(), configuration.getHost(), configuration.getPort());
                connection.disconnect();
                synchronized (users) {
                    users.clear();
                }
                XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                connection = null;
                LOG.info("disconnected.");
            }
        }
    }

    public boolean isLoggedIn() {
        return connection != null && connection.isAuthenticated();
    }

    private ConnectionConfiguration getConnectionConfig(XMPPConnectionConfiguration configuration) {
        return new ConnectionConfiguration(configuration.getHost(),
                                           configuration.getPort(),
                                           configuration.getServiceName());
    }

    @Override
    public void add(URI uri, FeatureInitializer featureInitializer) {
        featureInitializerMap.put(uri, featureInitializer);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(uri.toASCIIString());
    }

    private class RosterListenerImpl implements org.jivesoftware.smack.RosterListener {
        private final org.jivesoftware.smack.XMPPConnection connection;

        public RosterListenerImpl(org.jivesoftware.smack.XMPPConnection connection) {
            this.connection = connection;
        }

        public void entriesAdded(Collection<String> addedIds) {
            synchronized (users) {
                Roster roster = connection.getRoster();
                for(String id : addedIds) {
                    RosterEntry rosterEntry = roster.getEntry(id);
                    UserImpl user = new UserImpl(id, rosterEntry, configuration, connection);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("user " + user + " added");
                    }
                    users.put(id, user);
                    rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_ADDED));
                }
                users.notifyAll();
            }
        }

        public void entriesUpdated(Collection<String> updatedIds) {
            synchronized (users) {
                for(String id : updatedIds) {
                    Roster roster = connection.getRoster();
                    RosterEntry rosterEntry = roster.getEntry(id);
                    UserImpl user = users.get(id);
                    if(user == null) {
                        // should never happen ?
                        user = new UserImpl(id, rosterEntry, configuration, connection);
                        users.put(id, user);
                    } else {
                        user.setRosterEntry(rosterEntry);
                    }
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("user " + user + " updated");
                    }
                    rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_UPDATED));
                }
                users.notifyAll();
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            synchronized (users) {
                for(String id : removedIds) {
                    User user;
                    user = users.remove(id);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("user " + user + " removed");
                    }
                    rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_DELETED));
                }
                users.notifyAll();
            }
        }

        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            if(!presence.getFrom().equals(connection.getUser())) {
                Thread t = ThreadExecutor.newManagedThread(new DebugRunnable(new Runnable() {
                    public void run() {
                        UserImpl user = getUser(presence);
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("presence.from " + presence.getFrom() + " changed to " + presence.getType());
                        }
                        synchronized (user) {
                            if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                                if(!user.getFriendPresences().containsKey(presence.getFrom())) {
                                    addNewPresence(user, presence);
                                } else {
                                    updatePresence(user, presence);
                                }
                            } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                                PresenceImpl p = (PresenceImpl)user.getPresence(presence.getFrom());
                                if(p != null) {
                                    user.removePresense(new PresenceImpl(presence, p));
                                }
                            }
                        }
                    }
                }), "presence-thread-" + presence.getFrom());
                t.start();
            }
        }

        private UserImpl getUser(org.jivesoftware.smack.packet.Presence presence) {
            UserImpl user;
            synchronized (users) {
                user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                while(user == null) {
                    try {
                        LOG.debugf("presence {0} waiting for roster entry for user {1} ...",
                                presence.getFrom(), StringUtils.parseBareAddress(presence.getFrom()));
                        users.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                    if(user != null) {
                        if (LOG.isDebugEnabled())
                            LOG.debugf("found user {0} for presence {1}",
                                StringUtils.parseBareAddress(presence.getFrom()), presence.getFrom());
                    }
                }

            }
            return user;
        }

        private void addNewPresence(final UserImpl user, final org.jivesoftware.smack.packet.Presence presence) {
            final PresenceImpl presenceImpl = new PresenceImpl(presence, user, featureSupport);
            user.addPresense(presenceImpl);
            
            Thread t = ThreadExecutor.newManagedThread(new DebugRunnable(new Runnable() {
                public void run() {
                    DiscoverInfo discoverInfo = getDiscoInfo(presence);
                    //LOG.debugf("limewire user {0}, presence {1} detected", user, presence.getFrom());
                    for(URI uri : featureInitializerMap.keySet()) {
                        if(discoverInfo.containsFeature(uri.toASCIIString())) {
                            featureInitializerMap.get(uri).initializeFeature(presenceImpl);
                        }
                    }

                }
            }), "disco-info-" + presence.getFrom());
            t.start();

        }

        private DiscoverInfo getDiscoInfo(org.jivesoftware.smack.packet.Presence presence) {
            DiscoverInfo discoverInfo;
            try {
                ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
                discoverInfo = serviceDiscoveryManager.discoverInfo(presence.getFrom());
            } catch (org.jivesoftware.smack.XMPPException exception) {
                discoverInfo = new DiscoverInfo();
                if(exception.getXMPPError() != null && exception.getXMPPError().getCode() != 501) {
                    LOG.info(exception.getMessage(), exception);
                }
            }

            return discoverInfo;
        }

        private void updatePresence(UserImpl user, org.jivesoftware.smack.packet.Presence presence) {
            PresenceImpl currentPresence = (PresenceImpl)user.getFriendPresences().get(presence.getFrom());
            user.updatePresence(new PresenceImpl(presence, currentPresence));
        }
    }
    

    public void addUser(String id, String name) throws XMPPException {
        Roster roster = connection.getRoster();
        if(roster != null) {
            // TODO smack enhancement
            // TODO to support notifications when 
            // TODO the Roster is created

            try {
                roster.createEntry(id, name, null);
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new XMPPException(e);
            }
        }
    }
    
    public void removeUser(String id) throws XMPPException {
        Roster roster = connection.getRoster();
        if(roster != null) {
            // TODO smack enhancement
            // TODO to support notifications when 
            // TODO the Roster is created

            try {
                RosterEntry entry = roster.getEntry(id);
                if(entry!= null) {
                    roster.removeEntry(entry);    
                }                
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new XMPPException(e);
            }
        }
    }

    public void handleEvent(AddressEvent event) {
        LOG.debugf("handling address event: {0}", event.getSource());
        synchronized (this) {
            lastEvent = event;
            if(addressIQListener != null) {
                addressIQListener.handleEvent(event);    
            }
        }
    }

    @Override
    public User getUser(String id) {
        synchronized (users) { 
            return users.get(id);
        }
    }

    @Override
    public Collection<User> getUsers() {
        synchronized (users) { 
            return new ArrayList<User>(users.values());
        }
    }

    public void sendPacket(Packet packet) {
        synchronized (this) {
            if (connection.isConnected()) {
                connection.sendPacket(packet);
            }
        }
    }

    public String getLocalJid() {
        return connection.getUser();
    }
    
    private class SmackConnectionListener implements ConnectionListener, ConnectionCreationListener {

        @Override
        public void connectionCreated(XMPPConnection connection) {
            if(XMPPConnectionImpl.this.connection != connection) {
                return;
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("connection created for "+ connection.toString());
            }
            connection.addConnectionListener(this);

            synchronized (ProviderManager.getInstance()) {
                if(ProviderManager.getInstance().getIQProvider("address", "jabber:iq:lw-address") == null) {
                    ProviderManager.getInstance().addIQProvider("address", "jabber:iq:lw-address", new AddressIQProvider(addressFactory));
                }
                if(ProviderManager.getInstance().getIQProvider("file-transfer", "jabber:iq:lw-file-transfer") == null) {
                    ProviderManager.getInstance().addIQProvider("file-transfer", "jabber:iq:lw-file-transfer", FileTransferIQ.getIQProvider());
                }
                if(ProviderManager.getInstance().getIQProvider("auth-token", "jabber:iq:lw-auth-token") == null) {
                    ProviderManager.getInstance().addIQProvider("auth-token", "jabber:iq:lw-auth-token", new AuthTokenIQProvider());
                }
                if(ProviderManager.getInstance().getIQProvider("library-changed", "jabber:iq:lw-lib-change") == null) {
                    ProviderManager.getInstance().addIQProvider("library-changed", "jabber:iq:lw-lib-change", LibraryChangedIQ.getIQProvider());
                }
            }

            ChatStateManager.getInstance(connection);
            new FileOfferInitializer(connection).register(XMPPConnectionImpl.this);
            new LibraryChangedNotifierFeatureInitializer(connection).register(XMPPConnectionImpl.this);
            new LimewireFeatureInitializer().register(XMPPConnectionImpl.this);

            synchronized (XMPPConnectionImpl.this) {
                Address address = null;
                if(lastEvent != null) {
                    address = lastEvent.getSource();
                }
                addressIQListener = new AddressIQListener(XMPPConnectionImpl.this, addressFactory, address, XMPPConnectionImpl.this);
            }
            connection.addPacketListener(addressIQListener, addressIQListener.getPacketFilter());

            FileTransferIQListener fileTransferIQListener = new FileTransferIQListener(fileOfferBroadcaster);
            connection.addPacketListener(fileTransferIQListener, fileTransferIQListener.getPacketFilter());

            AuthTokenIQListener authTokenIQListener = new AuthTokenIQListener(XMPPConnectionImpl.this, authenticator, XMPPConnectionImpl.this);
            connection.addPacketListener(authTokenIQListener, authTokenIQListener.getPacketFilter());

            LibraryChangedIQListener libChangedIQListener = new LibraryChangedIQListener(libraryChangedEventEventBroadcaster, XMPPConnectionImpl.this);
            connection.addPacketListener(libChangedIQListener, libChangedIQListener.getPacketFilter());
        }

        @Override
        public void connectionClosed() {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.DISCONNECTED));
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.DISCONNECTED, e));
        }

        @Override
        public void reconnectingIn(int seconds) {
        }

        @Override
        public void reconnectionFailed(Exception e) {
        }

        @Override
        public void reconnectionSuccessful() {
        }
    }
}