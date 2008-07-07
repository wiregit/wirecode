package org.limewire.xmpp.client.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.file.FileDescription;
import org.jivesoftware.smackx.jingle.file.FileLocator;
import org.jivesoftware.smackx.jingle.file.UserAcceptor;
import org.jivesoftware.smackx.jingle.file.FileContentHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.xmpp.client.service.*;
import org.limewire.xmpp.client.impl.messages.library.LibraryIQ;
import org.limewire.xmpp.client.impl.messages.library.LibraryIQListener;

class XMPPConnectionImpl implements org.limewire.xmpp.client.service.XMPPConnection {
    
    private static final Log LOG = LogFactory.getLog(XMPPConnectionImpl.class);
    
    private final XMPPConnectionConfiguration configuration;
    private final LibraryProvider libraryProvider;
    private final IncomingFileAcceptor incomingFileAcceptor;
    private final FileTransferProgressListener progressListener;
    private org.jivesoftware.smack.XMPPConnection connection;
    
    private final CopyOnWriteArrayList<org.limewire.xmpp.client.service.RosterListener> rosterListeners;
    private final HashMap<String, UserImpl> users;
    protected LibraryIQListener libraryIQListener;
    
    XMPPConnectionImpl(XMPPConnectionConfiguration configuration, 
                       LibraryProvider libraryProvider,
                       IncomingFileAcceptor incomingFileAcceptor,
                       FileTransferProgressListener progressListener) {
        this.configuration = configuration;
        this.libraryProvider = libraryProvider;
        this.incomingFileAcceptor = incomingFileAcceptor;
        this.progressListener = progressListener;
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
                user.addPresenceListener(new LibraryGetter());
            }
            public void userUpdated(User user) {}
            public void userDeleted(String id) {}
        });
        try {
            Class.forName("org.jivesoftware.smackx.jingle.JingleManager");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
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
                    libraryIQListener = new LibraryIQListener(connection, libraryProvider);
                    libraryIQListener.setConnection(connection);
                    connection.addPacketListener(libraryIQListener, libraryIQListener.getPacketFilter());
                    ProviderManager.getInstance().addIQProvider("library", "jabber:iq:lw-library", LibraryIQ.getIQProvider());
                    FileDescription.setUserAccptor(new UserAcceptor() {
                        public boolean userAccepts(FileDescription.FileContainer file) {
                            return incomingFileAcceptor.accept(new FileMetaDataAdapter(file.getFile()));
                        }
                    });
                    
                    JingleManager manager = new JingleManager(connection);
                    manager.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                        public void sessionRequested(JingleSessionRequest request) {
                            try {
                                if(LOG.isInfoEnabled()) {
                                    LOG.info("incoming jingle request from " + request.getFrom());
                                }
                                IncomingJingleSession session = request.accept();
                                Jingle jingle = session.getInitialSessionRequest().getJingle();
                                if(jingle != null) {
                                    Content content = jingle.getContent();
                                    if(content != null) {
                                        Description description = content.getDescriptions().get(0);
                                        if(description != null) {
                                            if(description instanceof FileDescription) {
                                                ((FileContentHandler)session.getContentHandler()).setFileLocator(new FileLocatorAdapter());
                                                ((FileContentHandler)session.getContentHandler()).setProgressListener(new ProgressListenerAdapter(progressListener, session));
                                                // TODO set UserAcceptor
                                                if(LOG.isInfoEnabled()) {
                                                    LOG.info("starting jingle session");
                                                }
                                                session.start();
                                                return;
                                            }
                                        }
                                    }
                                }
                                if(LOG.isInfoEnabled()) {
                                    LOG.info("rejecting jingle session");
                                }
                                session.terminate();
                            } catch (org.jivesoftware.smack.XMPPException e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                    });
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
            Thread t = ThreadExecutor.newManagedThread(new Runnable() {
                public void run() {
                    UserImpl user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("user " + user + " presence changed to " + presence.getType());
                    }
                    if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                        try {
                            if (ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(presence.getFrom()).containsFeature(XMPPServiceImpl.LW_SERVICE_NS)) {
                                if(LOG.isDebugEnabled()) {
                                    LOG.debug("limwire user " + user + ", presence " + presence.getFrom() + " detected");
                                }
                                user.addPresense(new LimePresenceImpl(presence, connection, libraryIQListener, new FileLocatorAdapter()));
                            } else {
                                user.addPresense(new PresenceImpl(presence, connection));
                            }
                        } catch (org.jivesoftware.smack.XMPPException exception) {
                            LOG.error(exception.getMessage(), exception);
                        }
                    } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                        user.removePresense(new PresenceImpl(presence, connection));
                    }
                }
            }, "presence-handler-" + presence.getFrom());
            t.start();
        }
    }
    
    private class LibraryGetter implements PresenceListener {
        public void presenceChanged(Presence presence) {
            if(presence.getType().equals(Presence.Type.available)) {
                if(presence instanceof LimePresence) {
                    ((LimePresenceImpl) presence).sendGetLibrary();
                }
            }
        }
    }
    
    private class FileLocatorAdapter implements FileLocator {
        public InputStream readFile(StreamInitiation.File file) throws FileNotFoundException {
            return libraryProvider.readFile(new FileMetaDataAdapter(file));
        }

        public OutputStream writeFile(StreamInitiation.File file) throws IOException {
            return libraryProvider.writeFile(new FileMetaDataAdapter(file));
        }    
    }
}
