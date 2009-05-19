package org.limewire.xmpp.client.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
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
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FileOfferEvent;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.client.FriendRequestEvent;
import org.limewire.core.api.friend.client.LibraryChangedEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.core.api.friend.impl.LimewireFeatureInitializer;
import org.limewire.io.Address;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventRebroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequestedEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.client.impl.features.FileOfferInitializer;
import org.limewire.xmpp.client.impl.features.LibraryChangedNotifierFeatureInitializer;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListener;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListenerFactory;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQProvider;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQ;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQListener;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQProvider;
import org.limewire.xmpp.client.impl.messages.discoinfo.DiscoInfoListener;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListener;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQ;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQListener;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class XMPPFriendConnectionImpl implements FriendConnection {

    private static final Log LOG = LogFactory.getLog(XMPPFriendConnectionImpl.class);

    private final FriendConnectionConfiguration configuration;
    private final EventBroadcaster<FileOfferEvent> fileOfferBroadcaster;
    private final EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster;
    private final EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster;
    private final EventBroadcaster<FriendConnectionEvent> connectionBroadcaster;
    private final AddressFactory addressFactory;
    private final EventMulticaster<FeatureEvent> featureSupport;
    private final EventBroadcaster<ConnectBackRequestedEvent> connectRequestEventBroadcaster;
    private final ListeningExecutorService executorService;
    private final List<ConnectionConfigurationFactory> connectionConfigurationFactories;
    private final AddressIQListenerFactory addressIQListenerFactory;
    private final AuthTokenIQListenerFactory authTokenIQListenerFactory;
    private final FeatureRegistry featureRegistry;
    private volatile AddressIQListener addressIQListener;
    private volatile AuthTokenIQListener authTokenIQListener;

    private final EventListenerList<RosterEvent> rosterListeners;
    private final Map<String, XMPPFriendImpl> users;
    private final SmackConnectionListener smackConnectionListener;
    private final AtomicBoolean loggedIn = new AtomicBoolean(false);
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);

    private volatile org.jivesoftware.smack.XMPPConnection connection;
    private volatile DiscoInfoListener discoInfoListener;

    @AssistedInject
    public XMPPFriendConnectionImpl(@Assisted FriendConnectionConfiguration configuration,
                       @Assisted ListeningExecutorService executorService,
                       EventBroadcaster<RosterEvent> rosterBroadcaster,
                       EventBroadcaster<FileOfferEvent> fileOfferBroadcaster,
                       EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster,
                       EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster,
                       EventBroadcaster<FriendConnectionEvent> connectionBroadcaster,
                       AddressFactory addressFactory,
                       EventMulticaster<FeatureEvent> featureSupport,
                       EventBroadcaster<ConnectBackRequestedEvent> connectRequestEventBroadcaster,                     
                       List<ConnectionConfigurationFactory> connectionConfigurationFactories,
                       AddressIQListenerFactory addressIQListenerFactory,
                       AuthTokenIQListenerFactory authTokenIQListenerFactory,
                       FeatureRegistry featureRegistry) {
        this.configuration = configuration;
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        this.friendRequestBroadcaster = friendRequestBroadcaster;
        this.libraryChangedEventEventBroadcaster = libraryChangedEventEventBroadcaster;
        this.connectionBroadcaster = connectionBroadcaster;
        this.addressFactory = addressFactory;
        this.featureSupport = featureSupport;
        this.connectRequestEventBroadcaster = connectRequestEventBroadcaster;
        this.executorService = executorService;
        this.connectionConfigurationFactories = connectionConfigurationFactories;
        this.addressIQListenerFactory = addressIQListenerFactory;
        this.authTokenIQListenerFactory = authTokenIQListenerFactory;
        this.featureRegistry = featureRegistry;
        rosterListeners = new EventListenerList<RosterEvent>();
        // FIXME: this is only used by tests
        if(configuration.getRosterListener() != null) {
            rosterListeners.addListener(configuration.getRosterListener());
        }
        rosterListeners.addListener(new EventRebroadcaster<RosterEvent>(rosterBroadcaster));
        users = new TreeMap<String, XMPPFriendImpl>(String.CASE_INSENSITIVE_ORDER);
        
        smackConnectionListener = new SmackConnectionListener();
    }
    
    @Override
    public String toString() {
        return org.limewire.util.StringUtils.toString(this, configuration, connection);
    }
    
    public ListeningFuture<Void> setMode(final FriendPresence.Mode mode) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                setModeImpl(mode);
                return null;
            }
        });   
    }    

    void setModeImpl(FriendPresence.Mode mode) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                connection.sendPacket(getPresenceForMode(mode));
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            } 
        }
    }

    private Packet getPresenceForMode(FriendPresence.Mode mode) {
        org.jivesoftware.smack.packet.Presence presence = new org.jivesoftware.smack.packet.Presence(
                org.jivesoftware.smack.packet.Presence.Type.available);
        presence.setMode(org.jivesoftware.smack.packet.Presence.Mode.valueOf(mode.name()));
        return presence;
    }

    public FriendConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ListeningFuture<Void> login() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                loginImpl();
                return null;
            }
        });   
    }

    void loginImpl() throws FriendException {
        synchronized (this) {
            try {
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTING));        
                loggingIn.set(true);
                org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(smackConnectionListener);
                org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
                connect();
                LOG.infof("connected.");
                LOG.infof("logging in {0} with resource: {1} ...", configuration.getUserInputLocalID(), configuration.getResource());
                connection.login(configuration.getUserInputLocalID(), configuration.getPassword(), configuration.getResource());
                LOG.infof("logged in.");
                loggedIn.set(true);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTED));
            } catch (org.jivesoftware.smack.XMPPException e) {
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                if(connection != null && connection.isConnected()) {
                    connection.disconnect();
                }
                org.jivesoftware.smack.XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                connection = null;
                throw new FriendException(e);
            } finally {
                loggingIn.set(false);
            }
        }
    }

    private void connect() throws org.jivesoftware.smack.XMPPException {
        for(ConnectionConfigurationFactory factory : connectionConfigurationFactories) {
            try {
                connectUsingFactory(factory);
                return;
            } catch (FriendException e) {
                LOG.debug(e.getMessage(), e);
            }
        }
    }

    private void connectUsingFactory(ConnectionConfigurationFactory factory) throws FriendException {
        ConnectionConfigurationFactory.RequestContext requestContext = new ConnectionConfigurationFactory.RequestContext();
        while(factory.hasMore(configuration, requestContext)) {
            ConnectionConfiguration connectionConfig = factory.getConnectionConfiguration(configuration, requestContext);
            connection = new XMPPConnection(connectionConfig);
            connection.addRosterListener(new RosterListenerImpl());
            LOG.infof("connecting to {0} at {1}:{2} ...", connectionConfig.getServiceName(), connectionConfig.getHost(), connectionConfig.getPort());
            try {
                connection.connect();
                return;
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debug(e.getMessage(), e);
                requestContext.incrementRequests();
            }            
        }
        throw new FriendException("couldn't connect using " + factory);
    }

    @Override
    public boolean isLoggingIn() {
        return loggingIn.get();
    }

    @Override
    public ListeningFuture<Void> logout() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logoutImpl();
                return null;
            }
        }); 
    }

    void logoutImpl() {
        synchronized (this) {
            if(isLoggedIn()) {
                loggedIn.set(false);
                LOG.infof("disconnecting from {0} at {1}:{2} ...", connection.getServiceName(), connection.getHost(), connection.getPort());
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
        return loggedIn.get();
    }
    
    private void checkLoggedIn() throws FriendException {
        synchronized (this) {
            if(!isLoggedIn()) {
                throw new FriendException("not connected");
            }
        }
    }

    private class RosterListenerImpl implements org.jivesoftware.smack.RosterListener {

        public void entriesAdded(Collection<String> addedIds) {
            try {
                synchronized (XMPPFriendConnectionImpl.this) {
                    checkLoggedIn();
                    synchronized (users) {
                        Roster roster = connection.getRoster();
                        if(roster != null) {
                            for(String id : addedIds) {
                                RosterEntry rosterEntry = roster.getEntry(id);
                                XMPPFriendImpl user = new XMPPFriendImpl(id, rosterEntry, configuration, connection, featureRegistry);
                                LOG.debugf("user {0} added", user);
                                users.put(id, user);
                                rosterListeners.broadcast(new RosterEvent(user, RosterEvent.Type.USER_ADDED));
                            }
                        }
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debugf(e, "error getting roster");    
            } catch (FriendException e) {
                LOG.debugf(e, "error getting roster");    
            }
        }

        public void entriesUpdated(Collection<String> updatedIds) {
            try {
                synchronized (XMPPFriendConnectionImpl.this) {
                    checkLoggedIn();
                    synchronized (users) {                 
                        Roster roster = connection.getRoster();
                        if(roster != null) {
                            for(String id : updatedIds) {
                                RosterEntry rosterEntry = roster.getEntry(id);
                                XMPPFriendImpl user = users.get(id);
                                if(user == null) {
                                    // should never happen ?
                                    user = new XMPPFriendImpl(id, rosterEntry, configuration, connection, featureRegistry);
                                    users.put(id, user);
                                } else {
                                    user.setRosterEntry(rosterEntry);
                                }
                                LOG.debugf("user {0} updated", user);
                                rosterListeners.broadcast(new RosterEvent(user, RosterEvent.Type.USER_UPDATED));
                            }
                        }
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debugf(e, "error getting roster");    
            } catch (FriendException e) {
                LOG.debugf(e, "error getting roster");    
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            synchronized (users) {
                for(String id : removedIds) {
                    XMPPFriendImpl friend = users.remove(id);
                    if(friend != null) {
                        LOG.debugf("user {0} removed", friend);
                        rosterListeners.broadcast(new RosterEvent(friend, RosterEvent.Type.USER_DELETED));
                    }
                }
            }
        }

        @Override
        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            String localJID;
            try {
                localJID = getLocalJid();
            } catch (FriendException e) {
                localJID = null;
            }
            if(!presence.getFrom().equals(localJID)) {
                XMPPFriendImpl user = getUser(presence);
                if(user != null) {
                    LOG.debugf("presence from {0} changed to {1}", presence.getFrom(), presence.getType());
                    synchronized (user) {
                        if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                            if(!user.getPresences().containsKey(presence.getFrom())) {
                                addNewPresence(user, presence);
                            } else {
                                updatePresence(user, presence);
                            }
                        } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                            PresenceImpl p = (PresenceImpl)user.getPresence(presence.getFrom());
                            if(p != null) {
                                p.update(presence);
                                user.removePresense(p);
                            }
                        }
                    }
                } else {
                    LOG.debugf("no user for presence {0}", presence.getFrom());    
                }
            }
        }

        private XMPPFriendImpl getUser(org.jivesoftware.smack.packet.Presence presence) {
            XMPPFriendImpl user;
            synchronized (users) {
                user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
            }
            return user;
        }

        private void addNewPresence(final XMPPFriendImpl user, final org.jivesoftware.smack.packet.Presence presence) {
            final PresenceImpl presenceImpl = new PresenceImpl(presence, user, featureSupport);
            presenceImpl.addTransport(Address.class, addressIQListener);
            presenceImpl.addTransport(AuthToken.class, authTokenIQListener);
            user.addPresense(presenceImpl);
        }

        private void updatePresence(XMPPFriendImpl user, org.jivesoftware.smack.packet.Presence presence) {
            PresenceImpl currentPresence = (PresenceImpl)user.getPresences().get(presence.getFrom());
            currentPresence.update(presence);
            user.updatePresence(currentPresence);
        }
    }


    @Override
    public ListeningFuture<Void> addNewFriend(final String id, final String name) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                addUserImpl(id, name);
                return null;
            }
        }); 
    }

    void addUserImpl(String id, String name) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                Roster roster = connection.getRoster();
                if(roster != null) {
                    // TODO smack enhancement
                    // TODO to support notifications when
                    // TODO the Roster is created
                    roster.createEntry(id, name, null);
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            } 
        }
    }

    @Override
    public ListeningFuture<Void> removeFriend(final String id) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                removeUserImpl(id);
                return null;
            }
        }); 
    }

    private void removeUserImpl(String id) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                Roster roster = connection.getRoster();
                if(roster != null) {
                    // TODO smack enhancement
                    // TODO to support notifications when
                    // TODO the Roster is created
    
                    RosterEntry entry = roster.getEntry(id);
                    if(entry!= null) {
                        roster.removeEntry(entry);    
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            }
        }
    }

    @Override
    public Friend getFriend(String id) {
        id = org.jivesoftware.smack.util.StringUtils.parseBareAddress(id);
        synchronized (users) { 
            return users.get(id);
        }
    }

    @Override
    public Collection<Friend> getFriends() {
        synchronized (users) { 
            return new ArrayList<Friend>(users.values());
        }
    }

    public void sendPacket(Packet packet) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                connection.sendPacket(packet);
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            } 
        }
    }

    public String getLocalJid() throws FriendException {
        synchronized (this) {
            checkLoggedIn();
            return connection.getUser();
        }
    }
    
    private class SmackConnectionListener implements ConnectionListener, ConnectionCreationListener {
        @Override
        public void connectionCreated(XMPPConnection connection) {
            if(XMPPFriendConnectionImpl.this.connection != connection) {
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
                if (ProviderManager.getInstance().getIQProvider(ConnectBackRequestIQ.ELEMENT_NAME, ConnectBackRequestIQ.NAME_SPACE) == null) {
                    ProviderManager.getInstance().addIQProvider(ConnectBackRequestIQ.ELEMENT_NAME, ConnectBackRequestIQ.NAME_SPACE, new ConnectBackRequestIQProvider());
                }
            }

            ChatStateManager.getInstance(connection);

            discoInfoListener = new DiscoInfoListener(XMPPFriendConnectionImpl.this, connection, featureRegistry);
            rosterListeners.addListener(discoInfoListener.getRosterListener());
            connection.addPacketListener(discoInfoListener, discoInfoListener.getPacketFilter());

            addressIQListener = addressIQListenerFactory.create(XMPPFriendConnectionImpl.this, addressFactory);
            connection.addPacketListener(addressIQListener, addressIQListener.getPacketFilter());

            FileTransferIQListener fileTransferIQListener = new FileTransferIQListener(fileOfferBroadcaster);
            connection.addPacketListener(fileTransferIQListener, fileTransferIQListener.getPacketFilter());

            authTokenIQListener = authTokenIQListenerFactory.create(XMPPFriendConnectionImpl.this);
            connection.addPacketListener(authTokenIQListener, authTokenIQListener.getPacketFilter());

            LibraryChangedIQListener libChangedIQListener = new LibraryChangedIQListener(libraryChangedEventEventBroadcaster, XMPPFriendConnectionImpl.this);
            connection.addPacketListener(libChangedIQListener, libChangedIQListener.getPacketFilter());

            ConnectBackRequestIQListener connectRequestIQListener = new ConnectBackRequestIQListener(connectRequestEventBroadcaster, featureRegistry);
            connection.addPacketListener(connectRequestIQListener, connectRequestIQListener.getPacketFilter());
            
            new FileOfferInitializer(connection).register(featureRegistry);
            new LibraryChangedNotifierFeatureInitializer(connection).register(featureRegistry);
            new LimewireFeatureInitializer().register(featureRegistry);
            
            SubscriptionListener sub = new SubscriptionListener(connection,
                                                friendRequestBroadcaster);
            connection.addPacketListener(sub, sub);
            
            for(URI feature : featureRegistry) {
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(feature.toASCIIString());
            }
        }

        @Override
        public void connectionClosed() {
            connectionBroadcaster.broadcast(new FriendConnectionEvent(XMPPFriendConnectionImpl.this, FriendConnectionEvent.Type.DISCONNECTED));
            cleanup();
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            connectionBroadcaster.broadcast(new FriendConnectionEvent(XMPPFriendConnectionImpl.this, FriendConnectionEvent.Type.DISCONNECTED, e));
            cleanup();
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
        
        void cleanup() {
            ChatStateManager.remove(connection);
            if(discoInfoListener != null) {
                rosterListeners.removeListener(discoInfoListener.getRosterListener());
            }
        }
    }
}
