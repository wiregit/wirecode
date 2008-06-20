package org.limewire.xmpp.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.file.*;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.packet.Content;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.file.FileDescription;
import org.limewire.lifecycle.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPService implements Service {

    private static final Log LOG = LogFactory.getLog(XMPPService.class);
    
    private static final String LW_SERVICE_NS = "http://www.limewire.org/";
    
    private final XMPPServiceConfiguration configuration;
    private final LibrarySource librarySource;
    private final IncomingFileAcceptor incomingFileAcceptor;
    private final FileTransferProgressListener progressListener;
    private final CopyOnWriteArrayList<RosterListener> rosterListeners;
    private final HashMap<String, UserImpl> users;
    protected XMPPConnection connection;
    protected LibraryIQListener libraryIQListener;

    @Inject
    public XMPPService(XMPPServiceConfiguration configuration,
                       RosterListener rosterListener,
                       LibrarySource librarySource,
                       IncomingFileAcceptor incomingFileAcceptor,
                       FileTransferProgressListener progressListener) {
        this.configuration = configuration;
        this.librarySource = librarySource;
        this.incomingFileAcceptor = incomingFileAcceptor;
        this.progressListener = progressListener;
        this.rosterListeners = new CopyOnWriteArrayList<RosterListener>();
        this.rosterListeners.add(rosterListener);
        this.users = new HashMap<String, UserImpl>();
        this.rosterListeners.add(new org.limewire.xmpp.client.RosterListener() {
            public void userAdded(User user) {
                user.addPresenceListener(new LibraryGetter());
            }

            public void userUpdated(User user) {}

            public void userDeleted(String id) {}
        });
        
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public void start() {
        try {
            XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
            connection = new XMPPConnection(getConnectionConfig(configuration));
            connection.addRosterListener(new RosterListenerImpl(connection));
            LOG.info("connecting to " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + "...");
            connection.connect();
            LOG.info("connected.");
            LOG.info("logging in " + configuration.getUsername() + "...");
            connection.login(configuration.getUsername(), configuration.getPassword(), "limewire");
            LOG.info("logged in.");
        } catch (XMPPException e) {
            LOG.error(e.getMessage(), e);
            // TODO fireListenerMethod
        }
    }

    private ConnectionConfiguration getConnectionConfig(XMPPServiceConfiguration configuration) {
        return new ConnectionConfiguration(configuration.getHost(),
                                           configuration.getPort(),
                                           configuration.getServiceName());
    }

    public void stop() {
        LOG.info("disconnecting from " + configuration.getServiceName() + " at " + configuration.getHost() + ":" + configuration.getPort() + ".");
        connection.disconnect();
    }

    public void initialize() {
        try {
            Class.forName("org.jivesoftware.smackx.jingle.JingleManager");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(final XMPPConnection connection) {
                if(XMPPService.this.connection == connection) {
                    LOG.debug("adding connection listener for "+ connection.toString());
                    if(!ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(LW_SERVICE_NS)) {
                        // TODO conncurrency control
                        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(LW_SERVICE_NS);
                    }
                    libraryIQListener = new LibraryIQListener(connection,  librarySource);
                    libraryIQListener.setConnection(connection);
                    connection.addPacketListener(libraryIQListener, libraryIQListener.getPacketFilter());
                    ProviderManager.getInstance().addIQProvider("library", "jabber:iq:lw-library", LibraryIQ.getIQProvider());
                    FileDescription.setUserAccptor(new UserAcceptor() {
                        public boolean userAccepts(FileDescription.FileContainer file) {
                            return incomingFileAcceptor.accept(new File(file.getFile().getHash(), file.getFile().getName()));
                        }
                    });
                    
                    JingleManager manager = new JingleManager(connection);
                    manager.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                        public void sessionRequested(JingleSessionRequest request) {
                            try {
                                // Accept the call
                                LOG.info("incoming jingle request from " + request.getFrom());
                                IncomingJingleSession session = request.accept();
                                Jingle jingle = session.getInitialSessionRequest().getJingle();
                                if(jingle != null) {
                                    Content content = jingle.getContent();
                                    if(content != null) {
                                        Description description = content.getDescriptions().get(0);
                                        if(description != null) {
                                            if(description instanceof FileDescription) {
                                                ((FileContentHandler)session.getContentHandler()).setSaveDir(librarySource.getSaveDirectory(""));
                                                ((FileContentHandler)session.getContentHandler()).setProgressListener(new FileTransferProgressListenerAdapter(progressListener));
                                                // TODO set UserAcceptor
                                                LOG.info("starting jingle session");
                                                session.start();
                                                return;
                                            }
                                        }
                                    }
                                }
                                LOG.info("rejecting jingle session");
                                session.terminate();
                            } catch (XMPPException e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                    });
                }
            }
        });
    }

    public String getServiceName() {
        return "XMPP";
    }
    
    private class RosterListenerImpl implements org.jivesoftware.smack.RosterListener {
        private final XMPPConnection connection;

        public RosterListenerImpl(XMPPConnection connection) {
            this.connection = connection;
        }

        public void entriesAdded(Collection<String> addedIds) {
            for(String id : addedIds) {
                Roster roster = connection.getRoster();
                RosterEntry rosterEntry = roster.getEntry(id);
                UserImpl user = new UserImpl(id, rosterEntry.getName(), connection);
                LOG.debug("user " + user + " added");
                users.put(id, user);
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
                UserImpl user = new UserImpl(id, rosterEntry.getName(), connection);
                LOG.debug("user " + user + " updated");
                users.put(id, user);
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
                User user = users.remove(id);
                LOG.debug("user " + user + " removed");
                fireUserDeleted(id);
            }
        }
        
        private void fireUserDeleted(String id) {
            for(RosterListener rosterListener : rosterListeners) {
                rosterListener.userDeleted(id);
            }
        }

        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    UserImpl user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                    LOG.debug("user " + user + " presence changed to " + presence.getType());
                    if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                        try {
                            if (ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(presence.getFrom()).containsFeature("http://www.limewire.org/")) {
                                LOG.debug("limwire user " + user + ", presence " + presence.getFrom() + " detected");
                                user.addPresense(new LimePresenceImpl(presence, connection, libraryIQListener, librarySource.getSaveDirectory("")));
                            } else {
                                user.addPresense(new PresenceImpl(presence, connection));
                            }
                        } catch (XMPPException exception) {
                            LOG.error(exception.getMessage(), exception);
                        }
                    } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                        user.removePresense(new PresenceImpl(presence, connection));
                    }
                }
            });
            t.start();
        }
    }
    
    private class LibraryGetter implements PresenceListener {
        public void presenceChanged(org.limewire.xmpp.client.Presence presence) {
            if(presence.getType().equals(Presence.Type.available)) {
                if(presence instanceof LimePresence) {
                    ((LimePresenceImpl) presence).sendGetLibrary();
                }
            }
        }
    }
}
