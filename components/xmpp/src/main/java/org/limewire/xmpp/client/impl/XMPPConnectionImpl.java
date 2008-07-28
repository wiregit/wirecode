package org.limewire.xmpp.client.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.listener.EventListener;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListener;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.User;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//import com.limegroup.gnutella.BrowseHostReplyHandler;

class XMPPConnectionImpl implements org.limewire.xmpp.client.service.XMPPConnection, EventListener<AddressEvent> {
    
    private static final Log LOG = LogFactory.getLog(XMPPConnectionImpl.class);
    
    private final XMPPConnectionConfiguration configuration;
    private final FileOfferHandler fileOfferHandler;
    private final AddressFactory addressFactory;
    private org.jivesoftware.smack.XMPPConnection connection;
    
    private final CopyOnWriteArrayList<org.limewire.xmpp.client.service.RosterListener> rosterListeners;
    private final HashMap<String, UserImpl> users;
    protected AddressIQListener addressIQListener;
    protected FileTransferIQListener fileTransferIQListener;

    XMPPConnectionImpl(XMPPConnectionConfiguration configuration,
                       FileOfferHandler fileOfferHandler,
                       AddressFactory addressFactory) {
        this.configuration = configuration;
        this.fileOfferHandler = fileOfferHandler;
        this.addressFactory = addressFactory;
        this.rosterListeners = new CopyOnWriteArrayList<org.limewire.xmpp.client.service.RosterListener>();
        this.rosterListeners.add(configuration.getRosterListener());
        this.users = new HashMap<String, UserImpl>();
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
        LOG.info("disconnecting from " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + ".");
        connection.disconnect();
        LOG.info("disconnected.");
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
        this.rosterListeners.add(new org.limewire.xmpp.client.service.RosterListener() {
            public void userAdded(User user) {
                //user.addPresenceListener(new LibraryGetter());
            }
            public void userUpdated(User user) {}
            public void userDeleted(String id) {}
        });
        org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(final org.jivesoftware.smack.XMPPConnection connection) {
                if(XMPPConnectionImpl.this.connection == connection) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("adding connection listener for "+ connection.toString());
                    }
                    if(!ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(XMPPServiceImpl.LW_SERVICE_NS)) {
                        // TODO conncurrency control
                        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(XMPPServiceImpl.LW_SERVICE_NS);
                    }
                    addressIQListener = new AddressIQListener(connection, addressFactory);
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
                users.put(id, user);
                fireUserAdded(user);
            }
        }

        private void fireUserAdded(User user) {
            for(org.limewire.xmpp.client.service.RosterListener rosterListener : rosterListeners) {
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
                users.put(id, user);
                fireUserUpdated(user);
            }
        }

        private void fireUserUpdated(UserImpl user) {
            for(org.limewire.xmpp.client.service.RosterListener rosterListener : rosterListeners) {
                rosterListener.userUpdated(user);
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            for(String id : removedIds) {
                User user = users.remove(id);
                if(LOG.isDebugEnabled()) {
                    LOG.debug("user " + user + " removed");
                }
                fireUserDeleted(id);
            }
        }
        
        private void fireUserDeleted(String id) {
            for(org.limewire.xmpp.client.service.RosterListener rosterListener : rosterListeners) {
                rosterListener.userDeleted(id);
            }
        }

        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            if(!presence.getFrom().equals(connection.getUser())) {
                Thread t = ThreadExecutor.newManagedThread(new Runnable() {
                    public void run() {
                        UserImpl user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("user " + user + " presence changed to " + presence.getType());
                        }
                        if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                            if(!user.getPresences().containsKey(presence.getFrom())) {
                                try {
                                    if (ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(presence.getFrom()).containsFeature(XMPPServiceImpl.LW_SERVICE_NS)) {
                                        if(LOG.isDebugEnabled()) {
                                            LOG.debug("limewire user " + user + ", presence " + presence.getFrom() + " detected");
                                        }
                                        LimePresenceImpl limePresense = new LimePresenceImpl(presence, connection);
                                        limePresense.sendGetAddress();
                                        user.addPresense(limePresense);
                                    } else {
                                        user.addPresense(new PresenceImpl(presence, connection));
                                    }
                                } catch (org.jivesoftware.smack.XMPPException exception) {
                                    LOG.error(exception.getMessage(), exception);
                                }
                            } else {
                                // TODO update presence
                            }
                        } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                            user.removePresense(new PresenceImpl(presence, connection));
                        }
                    }
                }, "presence-handler-" + presence.getFrom());
                t.start();
            }
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
        addressIQListener.handleEvent(event);
    }
}
