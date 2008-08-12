package org.limewire.xmpp.client.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.listener.EventListener;
import org.limewire.net.address.Address;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.RosterListener;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListener;

//import com.limegroup.gnutella.BrowseHostReplyHandler;

class XMPPConnectionImpl implements org.limewire.xmpp.api.client.XMPPConnection, EventListener<AddressEvent> {
    
    private static final Log LOG = LogFactory.getLog(XMPPConnectionImpl.class);
    
    private final XMPPConnectionConfiguration configuration;
    private final FileOfferHandler fileOfferHandler;
    private final AddressFactory addressFactory;
    private volatile org.jivesoftware.smack.XMPPConnection connection;
    
    private final CopyOnWriteArrayList<RosterListener> rosterListeners;
    private final HashMap<String, UserImpl> users;
    protected volatile AddressIQListener addressIQListener;
    protected FileTransferIQListener fileTransferIQListener;
    protected volatile AddressEvent queuedEvent;

    XMPPConnectionImpl(XMPPConnectionConfiguration configuration,
                       FileOfferHandler fileOfferHandler,
                       AddressFactory addressFactory) {
        this.configuration = configuration;
        this.fileOfferHandler = fileOfferHandler;
        this.addressFactory = addressFactory;
        this.rosterListeners = new CopyOnWriteArrayList<org.limewire.xmpp.api.client.RosterListener>();
        if(configuration.getRosterListener() != null) {
            this.rosterListeners.add(configuration.getRosterListener());
        }
        this.users = new HashMap<String, UserImpl>();
    }
    
    public void addRosterListener(RosterListener rosterListener) {
        rosterListeners.add(rosterListener);
    }

    public void setMode(Presence.Mode mode) {
        connection.sendPacket(getPresenceForMode(mode));
    }

    private Packet getPresenceForMode(Presence.Mode mode) {
        return new org.jivesoftware.smack.packet.Presence(
                org.jivesoftware.smack.packet.Presence.Type.available,
                null, Presence.MIN_PRIORITY, org.jivesoftware.smack.packet.Presence.Mode.valueOf(mode.toString()));
    }

    public XMPPConnectionConfiguration getConfiguration() {
        return configuration;
    }

    public void login() throws XMPPException {
        try {
            org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
            connection = new org.jivesoftware.smack.XMPPConnection(getConnectionConfig(configuration));
            connection.addRosterListener(new RosterListenerImpl(connection));
            LOG.info("connecting to " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + "...");
            connection.connect();
            LOG.info("connected.");
            LOG.info("logging in " + configuration.getUsername() + "...");
            connection.login(configuration.getUsername(), configuration.getPassword(), "limewire");
            LOG.info("logged in.");
        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e);
        }
    }

    public void logout() {
        if(isLoggedIn()) {
            LOG.info("disconnecting from " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + ".");
            connection.disconnect();
            LOG.info("disconnected.");
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
                    ServiceDiscoveryManager.getInstanceFor(connection).addFeature(XMPPServiceImpl.LW_SERVICE_NS);
                    Address address = null;
                    synchronized (XMPPConnectionImpl.this) {
                        if(queuedEvent != null) {
                            address = queuedEvent.getSource();
                        }
                    }
                    addressIQListener = new AddressIQListener(connection, addressFactory, address);
                    queuedEvent = null;
                    XMPPConnectionImpl.this.rosterListeners.add(addressIQListener);
                    connection.addPacketListener(addressIQListener, addressIQListener.getPacketFilter());
                    ProviderManager.getInstance().addIQProvider("address", "jabber:iq:lw-address", new AddressIQProvider(addressFactory));

                    fileTransferIQListener = new FileTransferIQListener(fileOfferHandler);
                    connection.addPacketListener(fileTransferIQListener, fileTransferIQListener.getPacketFilter());
                    ProviderManager.getInstance().addIQProvider("file-transfer", "jabber:iq:lw-file-transfer", FileTransferIQ.getIQProvider());
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
            for(String id : addedIds) {
                Roster roster = connection.getRoster();
                RosterEntry rosterEntry = roster.getEntry(id);
                UserImpl user = new UserImpl(id, rosterEntry.getName());
                if(LOG.isDebugEnabled()) {
                    LOG.debug("user " + user + " added");
                }
                synchronized (users) {
                    users.put(id, user);
                }
                fireUserAdded(user);
            }
        }

        private void fireUserAdded(User user) {
            for(RosterListener rosterListener : rosterListeners) {
                rosterListener.userAdded(user);
            }
        }

        public void entriesUpdated(Collection<String> updatedIds) {
            for(String id : updatedIds) {
                Roster roster = connection.getRoster();
                RosterEntry rosterEntry = roster.getEntry(id);
                UserImpl user = new UserImpl(id, rosterEntry.getName());
                if(LOG.isDebugEnabled()) {
                    LOG.debug("user " + user + " updated");
                }
                synchronized (users) {
                    users.put(id, user);
                }
                fireUserUpdated(user);
            }
        }

        private void fireUserUpdated(UserImpl user) {
            for(RosterListener rosterListener : rosterListeners) {
                rosterListener.userUpdated(user);
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            for(String id : removedIds) {
                User user;
                synchronized (users) {                    
                    user = users.remove(id);
                }
                if(LOG.isDebugEnabled()) {
                    LOG.debug("user " + user + " removed");
                }
                fireUserDeleted(id);
            }
        }
        
        private void fireUserDeleted(String id) {
            for(RosterListener rosterListener : rosterListeners) {
                rosterListener.userDeleted(id);
            }
        }

        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            if(!presence.getFrom().equals(connection.getUser())) {
                Thread t = ThreadExecutor.newManagedThread(new Runnable() {
                    public void run() {
                        UserImpl user;
                        synchronized (users) {
                            user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                        }
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
                                user.removePresense(new PresenceImpl(presence, connection));
                            }
                        }
                    }
                }, "presence-handler-" + presence.getFrom());
                t.start();
            }
        }

        private void addNewPresence(UserImpl user, org.jivesoftware.smack.packet.Presence presence) {
            try {
                ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
                DiscoverInfo discoverInfo = serviceDiscoveryManager.discoverInfo(presence.getFrom());
                if (discoverInfo.containsFeature(XMPPServiceImpl.LW_SERVICE_NS)) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("limewire user " + user + ", presence " + presence.getFrom() + " detected");
                    }
                    LimePresenceImpl limePresense = new LimePresenceImpl(presence, connection);
                    limePresense.subscribeAndWaitForAddress();
                    user.addPresense(limePresense);
                } else {
                    user.addPresense(new PresenceImpl(presence, connection));
                }
            } catch (org.jivesoftware.smack.XMPPException exception) {
                LOG.error(exception.getMessage(), exception);
            }
        }

        private void updatePresence(UserImpl user, org.jivesoftware.smack.packet.Presence presence) {
            Presence currentPresence = user.getPresences().get(presence.getFrom());
            Presence updatedPresence;
            if(currentPresence instanceof LimePresenceImpl) {
                updatedPresence = new LimePresenceImpl(presence, connection);    
            } else {
                updatedPresence = new PresenceImpl(presence, connection);   
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

    public void handleEvent(AddressEvent event) {
        synchronized (this) {
            if(addressIQListener != null) {
                addressIQListener.handleEvent(event);    
            } else {
                queuedEvent = event;
            }
        }
    }
}
