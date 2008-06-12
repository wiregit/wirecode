package org.limewire.xmpp.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.file.FileContentHandler;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.limewire.lifecycle.Service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPService implements Service {
    
    private static final String LW_SERVICE_NS = "http://www.limewire.org/";

    private static LibraryIQListener libraryIQListener = new LibraryIQListener(null, null);
    
    private final XMPPServiceConfiguration configuration;
    private final org.limewire.xmpp.client.RosterListener listeners;
    protected HashSet<String> limewireClients;
    protected XMPPConnection connection;

    @Inject
    public XMPPService(XMPPServiceConfiguration configuration,
                       org.limewire.xmpp.client.RosterListener listeners) {
        this.configuration = configuration;
        this.listeners = listeners;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public void start() {
        try {
            XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
            connection = new XMPPConnection(getConnectionConfig(configuration));
            connection.connect();
            connection.login(configuration.getUsername(), configuration.getPassword(), "limewire");

            limewireClients = getRoster(connection);
            getLibraries(limewireClients, connection);

        } catch (XMPPException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private ConnectionConfiguration getConnectionConfig(XMPPServiceConfiguration configuration) {
        return new ConnectionConfiguration(configuration.getHost(),
                                           configuration.getPort(),
                                           configuration.getServiceName());
    }
    
    private HashSet<String> getRoster(XMPPConnection connection) throws InterruptedException {
        // TODO hack to wait for roster to be loaded.
        // replace with XMPPConnection level RosterListener
        Thread.sleep(5 * 1000);        

        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);

        Roster roster = connection.getRoster();        

        HashSet<String> limewireClients = new HashSet<String>();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            System.out.println("found: " + rosterEntry.getUser());
            Iterator<Presence> presences = roster.getPresences(rosterEntry.getUser());
            while (presences.hasNext()) {
                Presence presence = presences.next();
                if (presence.getType() == Presence.Type.available) {
                    String name = rosterEntry.getName() != null ? rosterEntry.getName() : rosterEntry.getUser();                    
                    try {
                        if (serviceDiscoveryManager.discoverInfo(presence.getFrom()).containsFeature("http://www.limewire.org/")) {
                            limewireClients.add(presence.getFrom());
                            System.out.println("lw presence: " + presence.getFrom());
                        } else {
                            System.out.println("presence: " + presence.getFrom());
                        }
                    } catch (XMPPException exception) {
                        //exception.printStackTrace();
                    }
                }
            }
        }
       
        roster.addRosterListener(new RosterListenerImpl(connection));
        return limewireClients;          
    }
    
    private void getLibraries(HashSet<String> limewireClients, XMPPConnection connection) {
        for (String limewireClient : limewireClients) {
            System.out.println("get library of " + limewireClient);
            Library query = new Library();
            query.setType(IQ.Type.GET);
            query.setTo(limewireClient);
            query.setPacketID(IQ.nextID());
            connection.sendPacket(query);
        }
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
                ProviderManager.getInstance().addIQProvider("library", "jabber:iq:lw-library", Library.getIQProvider());
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

        public void entriesAdded(Collection<String> strings) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void entriesUpdated(Collection<String> strings) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void entriesDeleted(Collection<String> strings) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void presenceChanged(Presence presence) {
            Roster roster = connection.getRoster();
            if (presence.getType() == Presence.Type.available) {
                RosterEntry entry = roster.getEntry(presence.getFrom());
                String name = entry.getName();
                if (name == null || name.trim().length() == 0) {
                    name = presence.getFrom();
                }
                System.out.println("found: " + name);
                try {
                    if (ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(presence.getFrom()).containsFeature("http://www.limewire.org/")) {
                        limewireClients.add(presence.getFrom());
                        System.out.println("found lw client: " + presence.getFrom());
                    }
                } catch (XMPPException exception) {
                    //exception.printStackTrace();
                }
            } else if (presence.getType() == Presence.Type.unavailable) {
                limewireClients.remove(presence.getFrom());
            }
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

    private void jingleOUT(String to, XMPPConnection connection) throws InterruptedException {
        JingleManager manager = new JingleManager(connection);

        try {
            FileContentHandler fileContentHandler = new FileContentHandler(new java.io.File("C:\\ChocolateEggThings\\IMG_0089.JPG"), true);
            OutgoingJingleSession out = manager.createOutgoingJingleSession(to, fileContentHandler);

            out.start();

            while (out.getJingleMediaSession() == null) {
                Thread.sleep(500);
            }

            //out.terminate();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }
}
