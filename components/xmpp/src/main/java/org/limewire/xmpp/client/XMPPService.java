package org.limewire.xmpp.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.limewire.lifecycle.Service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPService implements Service {
    
    private static final String LW_SERVICE_NS = "http://www.limewire.org/";

    private static LibraryIQListener libraryIQListener = new LibraryIQListener(null, null);
    
    private final XMPPServiceConfiguration configuration;
    private final CopyOnWriteArrayList<org.limewire.xmpp.client.RosterListener> rosterListeners;
    private final HashMap<String, UserImpl> users;
    protected XMPPConnection connection;

    @Inject
    public XMPPService(XMPPServiceConfiguration configuration,
                       org.limewire.xmpp.client.RosterListener rosterListener) {
        this.configuration = configuration;
        this.rosterListeners = new CopyOnWriteArrayList<org.limewire.xmpp.client.RosterListener>();
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
            connection.connect();
            connection.login(configuration.getUsername(), configuration.getPassword(), "limewire");
        } catch (XMPPException e) {
            throw new IllegalStateException(e);
        }
    }

    private ConnectionConfiguration getConnectionConfig(XMPPServiceConfiguration configuration) {
        return new ConnectionConfiguration(configuration.getHost(),
                                           configuration.getPort(),
                                           configuration.getServiceName());
    }

    public void stop() {
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
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(LW_SERVICE_NS);
                libraryIQListener.setConnection(connection);
                connection.addPacketListener(libraryIQListener, libraryIQListener.getPacketFilter());
                ProviderManager.getInstance().addIQProvider("library", "jabber:iq:lw-library", LibraryIQ.getIQProvider());
            }
        });
    }

    public String getServiceName() {
        return "XMPP";
    }
    
    private class RosterListenerImpl implements RosterListener {
        private final XMPPConnection connection;

        public RosterListenerImpl(XMPPConnection connection) {
            this.connection = connection;
        }

        public void entriesAdded(Collection<String> addedIds) {
            for(String id : addedIds) {
                Roster roster = connection.getRoster();
                RosterEntry rosterEntry = roster.getEntry(id);
                UserImpl user = new UserImpl(id, rosterEntry.getName(), connection);
                users.put(id, user);
                fireUserAdded(user);
            }
        }

        private void fireUserAdded(User user) {
            for(org.limewire.xmpp.client.RosterListener rosterListener : rosterListeners) {
                rosterListener.userAdded(user);
            }
        }

        public void entriesUpdated(Collection<String> updatedIds) {
            for(String id : updatedIds) {
                Roster roster = connection.getRoster();
                RosterEntry rosterEntry = roster.getEntry(id);
                UserImpl user = new UserImpl(id, rosterEntry.getName(), connection);
                users.put(id, user);
                fireUserUpdated(user);
            }
        }

        private void fireUserUpdated(UserImpl user) {
            for(org.limewire.xmpp.client.RosterListener rosterListener : rosterListeners) {
                rosterListener.userUpdated(user);
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            for(String id : removedIds) {
                users.remove(id);
                fireUserDeleted(id);
            }
        }
        
        private void fireUserDeleted(String id) {
            for(org.limewire.xmpp.client.RosterListener rosterListener : rosterListeners) {
                rosterListener.userDeleted(id);
            }
        }

        public void presenceChanged(final Presence presence) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    UserImpl user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                    if (presence.getType().equals(Presence.Type.available)) {
                        try {
                            if (ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(presence.getFrom()).containsFeature("http://www.limewire.org/")) {
                                user.addPresense(new LimePresenceImpl(presence, connection));
                            } else {
                                user.addPresense(new PresenceImpl(presence, connection));
                            }
                        } catch (XMPPException exception) {
                            exception.printStackTrace();
                        }
                    } else if (presence.getType().equals(Presence.Type.unavailable)) {
                        user.removePresense(new PresenceImpl(presence, connection));
                    }
                }
            });
            t.start();
        }
    }
    
    private void jingleIN(XMPPConnection connection) {
        //"jstun.javawi.de", 3478
        JingleManager manager = new JingleManager(connection);
        manager.addJingleSessionRequestListener(new JingleSessionRequestListener() {
            public void sessionRequested(JingleSessionRequest request) {

                try {
                    // Accept the call
                    IncomingJingleSession session = request.accept();
                    // Start the call
                    session.start();
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class LibraryGetter implements PresenceListener {
        public synchronized void presenceChanged(org.limewire.xmpp.client.Presence presence) {
            if(presence.getType().equals(org.limewire.xmpp.client.Presence.Type.available)) {
                if(presence instanceof LimePresence) {
                    ((LimePresenceImpl) presence).sendGetLibrary();
                }
            }
        }
    }
}
