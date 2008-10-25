package org.limewire.xmpp.client.impl;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatStateManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.Address;
import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.DebugRunnable;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.LimePresence;
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



class XMPPConnectionImpl implements org.limewire.xmpp.api.client.XMPPConnection, EventListener<AddressEvent> {
    
    private static final Log LOG = LogFactory.getLog(XMPPConnectionImpl.class);
    
    private final XMPPConnectionConfiguration configuration;
    private final EventListener<FileOfferEvent> fileOfferListener;
    private final EventListener<LibraryChangedEvent> libraryChangedEventEventListener;
    private final EventListener<XMPPConnectionEvent> connectionListener;
    private final AddressFactory addressFactory;
    private volatile org.jivesoftware.smack.XMPPConnection connection;
    
    private final EventListenerList<RosterEvent> rosterListeners;
    private final Map<String, UserImpl> users;
    protected volatile AddressIQListener addressIQListener;
    protected FileTransferIQListener fileTransferIQListener;
    protected AuthTokenIQListener authTokenIQListener;
    protected LibraryChangedIQListener libChangedIQListener;
    protected volatile AddressEvent lastEvent;
    private final XMPPAuthenticator authenticator;

    XMPPConnectionImpl(XMPPConnectionConfiguration configuration,
                       EventListener<RosterEvent> rosterListener,
                       EventListener<FileOfferEvent> fileOfferListener,
                       EventListener<LibraryChangedEvent> libraryChangedEventEventListener,
                       EventListener<XMPPConnectionEvent> connectionListener,
                       AddressFactory addressFactory, XMPPAuthenticator authenticator) {
        this.configuration = configuration;
        this.fileOfferListener = fileOfferListener;
        this.libraryChangedEventEventListener = libraryChangedEventEventListener;
        this.connectionListener = connectionListener;
        this.addressFactory = addressFactory;
        this.authenticator = authenticator;
        this.rosterListeners = new EventListenerList<RosterEvent>();
        if(configuration.getRosterListener() != null) {
            this.rosterListeners.addListener(configuration.getRosterListener());
        }
        this.rosterListeners.addListener(rosterListener);
        this.users = new TreeMap<String, UserImpl>(String.CASE_INSENSITIVE_ORDER);
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
                LOG.info("connecting to " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + "...");
                connection.connect();
                LOG.info("connected.");
                if (LOG.isInfoEnabled())
                    LOG.infof("logging in " + configuration.getUsername() + " with resource: " + configuration.getResource());
                connection.login(configuration.getUsername(), configuration.getPassword(), configuration.getResource());
                LOG.info("logged in.");
                connectionListener.handleEvent(new XMPPConnectionEvent(configuration.getUsername(), ConnectionEvent.LOGIN));
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new XMPPException(e);
            }
        }
    }

    public void logout() {
        synchronized (this) {
            if(isLoggedIn()) {
                LOG.info("disconnecting from " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + ".");
                connection.disconnect();
                rosterListeners.removeListener(addressIQListener.getRosterListener());
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
//        this.rosterListeners.add(new org.limewire.xmpp.client.service.RosterListener() {
//            public void register(XMPPService xmppService) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            public void userAdded(User user) {
//                //user.addPresenceListener(new LibraryGetter());
//            }
//            public void userUpdated(User user) {}
//            public void userDeleted(String id) {}
//        });
        org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(final org.jivesoftware.smack.XMPPConnection connection) {
                if(XMPPConnectionImpl.this.connection == connection) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("adding connection listener for "+ connection.toString());
                    }
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
                        addressIQListener = new AddressIQListener(connection, addressFactory, address);     
                    }                                   
                    connection.addPacketListener(addressIQListener, addressIQListener.getPacketFilter());                    
                    XMPPConnectionImpl.this.rosterListeners.addListener(addressIQListener.getRosterListener());

                    fileTransferIQListener = new FileTransferIQListener(fileOfferListener);
                    connection.addPacketListener(fileTransferIQListener, fileTransferIQListener.getPacketFilter());  
                    
                    authTokenIQListener = new AuthTokenIQListener(connection, authenticator);
                    XMPPConnectionImpl.this.rosterListeners.addListener(authTokenIQListener.getRosterListener());
                    connection.addPacketListener(authTokenIQListener, authTokenIQListener.getPacketFilter());

                    libChangedIQListener = new LibraryChangedIQListener(libraryChangedEventEventListener, users);
                    connection.addPacketListener(libChangedIQListener, libChangedIQListener.getPacketFilter());
                }
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
                    UserImpl user = new UserImpl(id, rosterEntry, configuration);
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
                        user = new UserImpl(id, rosterEntry, configuration);
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
                    rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_REMOVED));
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
                            LOG.debug("user " + user + " presence changed to " + presence.getType());
                        }
                        synchronized (user) {
                            if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                                if(!user.getPresences().containsKey(presence.getFrom())) {
                                    addNewPresence(user, presence);
                                } else {
                                    updatePresence(user, presence);
                                }
                            } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                                Presence p = user.getPresence(presence.getFrom());
                                if(p != null) {
                                    if(p instanceof LimePresence) {
                                        user.removePresense(new LimePresenceImpl(presence, connection, user));    
                                    } else {
                                        user.removePresense(new PresenceImpl(presence, connection, user));
                                    }                                    
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
                        LOG.debugf("found user {0} for presence {1}",
                                StringUtils.parseBareAddress(presence.getFrom()), presence.getFrom());    
                    }
                }
                
            }
            return user;
        }

        private void addNewPresence(UserImpl user, org.jivesoftware.smack.packet.Presence presence) {
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
            
            if (discoverInfo.containsFeature(XMPPServiceImpl.LW_SERVICE_NS)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("limewire user " + user + ", presence " + presence.getFrom() + " detected");
                }
                try {
                    LimePresenceImpl limePresense = new LimePresenceImpl(presence, connection, user);
                    limePresense.subscribeAndWaitForAddress();
                    user.addPresense(limePresense);
                } catch (InvalidDataException e) {
                    LOG.debug("could not parse address data", e);
                    user.addPresense(new PresenceImpl(presence, connection, user));
                }
            } else {
                user.addPresense(new PresenceImpl(presence, connection, user));
            }
        }

        private void updatePresence(UserImpl user, org.jivesoftware.smack.packet.Presence presence) {
            Presence currentPresence = user.getPresences().get(presence.getFrom());
            Presence updatedPresence;
            if(currentPresence instanceof LimePresence) {
                updatedPresence = new LimePresenceImpl(presence, connection, (LimePresence)currentPresence);    
            } else {
                updatedPresence = new PresenceImpl(presence, connection, user);
            }
            user.updatePresence(updatedPresence);
        }
    }
    
//    private class LibraryGetter implements PresenceListener {
//        public void presenceChanged(Presence presence) {
//            if(presence.getType().equals(Presence.Type.available)) {
//                if(presence instanceof LimePresence) {
//                    ((LimePresenceImpl) presence).sendGetLibrary();
//                }
//            }
//        }
//    }

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
                addressIQListener.getAddressListener().handleEvent(event);    
            }
        }
    }

    @Override
    public User getUser(String id) {
        synchronized (users) { 
            return users.get(id);
        }
    }
}
