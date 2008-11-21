package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatStateManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.features.FileOfferFeature;
import org.limewire.core.api.friend.feature.features.FileOfferer;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifierFeature;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.io.Address;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventRebroadcaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.DebugRunnable;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListener;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQProvider;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListener;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQ;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQListener;

public class XMPPConnectionImpl implements org.limewire.xmpp.api.client.XMPPConnection, EventListener<AddressEvent> {
    
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
    private volatile FileTransferIQListener fileTransferIQListener;
    private volatile AuthTokenIQListener authTokenIQListener;
    private volatile LibraryChangedIQListener libChangedIQListener;
    private volatile AddressEvent lastEvent;
    private final XMPPAuthenticator authenticator;
    private final ListenerSupport<FriendPresenceEvent> presenceSupport;

    XMPPConnectionImpl(XMPPConnectionConfiguration configuration,
                       EventBroadcaster<RosterEvent> rosterBroadcaster,
                       EventBroadcaster<FileOfferEvent> fileOfferBroadcaster,
                       EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster,
                       EventBroadcaster<XMPPConnectionEvent> connectionBroadcaster,
                       AddressFactory addressFactory, XMPPAuthenticator authenticator,
                       ListenerSupport<FriendPresenceEvent> presenceSupport) {
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
        this.presenceSupport = presenceSupport;
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
        synchronized (this) {
            try {
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
                throw new XMPPException(e);
            }
        }
    }

    public void logout() {
        synchronized (this) {
            if(isLoggedIn()) {
                if (LOG.isInfoEnabled())
                    LOG.info("disconnecting from " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + ".");
                connection.disconnect();
                addressIQListener.dispose();
                authTokenIQListener.dispose();
                addressIQListener = null;
                fileTransferIQListener = null;
                authTokenIQListener = null;
                synchronized (users) {
                    users.clear();
                }                
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
    
    public void initialize() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("adding connection listener for "+ toString());
        }
        
        org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(final org.jivesoftware.smack.XMPPConnection connection) {
                if(XMPPConnectionImpl.this.connection != connection) {
                    return;
                }
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("connection created for "+ connection.toString());
                }                
                connection.addConnectionListener(new SmackConnectionListener());
                
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
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(XMPPServiceImpl.LW_SERVICE_NS);                    
                
                synchronized (XMPPConnectionImpl.this) {
                    Address address = null;
                    if(lastEvent != null) {
                        address = lastEvent.getSource();
                    }
                    addressIQListener = new AddressIQListener(XMPPConnectionImpl.this, addressFactory, address, presenceSupport);     
                }                                   
                connection.addPacketListener(addressIQListener, addressIQListener.getPacketFilter());

                fileTransferIQListener = new FileTransferIQListener(fileOfferBroadcaster);
                connection.addPacketListener(fileTransferIQListener, fileTransferIQListener.getPacketFilter());  
                
                authTokenIQListener = new AuthTokenIQListener(XMPPConnectionImpl.this, authenticator, presenceSupport);
                connection.addPacketListener(authTokenIQListener, authTokenIQListener.getPacketFilter());

                libChangedIQListener = new LibraryChangedIQListener(libraryChangedEventEventBroadcaster, XMPPConnectionImpl.this);
                connection.addPacketListener(libChangedIQListener, libChangedIQListener.getPacketFilter());
            }
        });
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
            final PresenceImpl presenceImpl = new PresenceImpl(presence, user);
            user.addPresense(presenceImpl);
            
            Thread t = ThreadExecutor.newManagedThread(new DebugRunnable(new Runnable() {
                public void run() {
                    if(supportsLimeWireFeature(presence)) {
                        LOG.debugf("limewire user {0}, presence {1} detected", user, presence.getFrom());
                        presenceImpl.addFeature(new LimewireFeature());
                        presenceImpl.addFeature(new LibraryChangedNotifierFeature(getLibraryChangedNotifier(presence.getFrom())));
                        presenceImpl.addFeature(new FileOfferFeature(getFileOfferer(presence.getFrom())));
                    }
                }
            }), "disco-info-" + presence.getFrom());
            t.start();

        }

        private LibraryChangedNotifier getLibraryChangedNotifier(final String from) {
            return new LibraryChangedNotifier() {
                public void sendLibraryRefresh() {
                    final LibraryChangedIQ libraryChangedIQ = new LibraryChangedIQ();
                    libraryChangedIQ.setType(IQ.Type.SET);
                    libraryChangedIQ.setTo(from);
                    libraryChangedIQ.setPacketID(IQ.nextID());
                    connection.sendPacket(libraryChangedIQ);
                }    
            };
        }

        private boolean supportsLimeWireFeature(org.jivesoftware.smack.packet.Presence presence) {
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

            return discoverInfo.containsFeature(XMPPServiceImpl.LW_SERVICE_NS);
        }

        private void updatePresence(UserImpl user, org.jivesoftware.smack.packet.Presence presence) {
            PresenceImpl currentPresence = (PresenceImpl)user.getFriendPresences().get(presence.getFrom());
            user.updatePresence(new PresenceImpl(presence, currentPresence));
        }

        public FileOfferer getFileOfferer(final String jid) {
            return new FileOfferer() {
                public void offerFile(FileMetaData file) {
                    if(LOG.isInfoEnabled()) {
                        LOG.info("offering file " + file.toString() + " to " + jid);
                    }
                    final FileTransferIQ transferIQ = new FileTransferIQ(file, FileTransferIQ.TransferType.OFFER);
                    transferIQ.setType(IQ.Type.GET);
                    transferIQ.setTo(jid);
                    transferIQ.setPacketID(IQ.nextID());
                    connection.sendPacket(transferIQ);
                }
            };
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
    
    private class SmackConnectionListener implements ConnectionListener {
        @Override
        public void connectionClosed() {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.DISCONNECTED));
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.DISCONNECTED));
        }

        @Override
        public void reconnectingIn(int seconds) {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.RECONNECTING));
        }

        @Override
        public void reconnectionFailed(Exception e) {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.RECONNECTING_FAILED));
        }

        @Override
        public void reconnectionSuccessful() {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.CONNECTED));
        }
    }
}
