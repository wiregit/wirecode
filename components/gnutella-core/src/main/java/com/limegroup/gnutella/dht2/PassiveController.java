package com.limegroup.gnutella.dht2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IOUtils;
import org.limewire.io.SecureInputStream;
import org.limewire.io.SecureOutputStream;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.DefaultMojitoDHT;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito2.routing.RouteTable.RouteTableListener;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.DatabaseImpl;
import org.limewire.mojito2.util.ContactUtils;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.mojito2.util.IoUtils;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.connection.Connection;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.PingRequestFactory;

public class PassiveController extends AbstractController {

    private static final Log LOG 
        = LogFactory.getLog(PassiveController.class);
    
    private static final String NAME = "PASSIVE";
    
    public static final File PASSIVE_FILE 
        = new File(CommonUtils.getUserSettingsDir(), "passive.mojito");
    
    private final ConnectionServices connectionServices;
    
    private final DHTManager manager;
    
    private final NetworkManager networkManager;
    
    private final Transport transport;
    
    private final PassiveRouteTable routeTable 
        = new PassiveRouteTable();
    
    private final MojitoDHT dht;
    
    private final BootstrapWorker bootstrapWorker;
    
    private final ContactPusher contactPusher;
    
    private volatile Contact[] contacts = null;
    
    @Inject
    public PassiveController(DHTManager manager,
            NetworkManager networkManager,
            Transport transport, 
            Provider<ConnectionManager> connectionManager,
            Provider<HostCatcher> hostCatcher,
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            MessageFactory messageFactory, 
            ConnectionServices connectionServices,
            HostFilter filter,
            Provider<UDPPinger> udpPinger) throws UnknownHostException {
        super(DHTMode.PASSIVE);
        
        this.connectionServices = connectionServices;
        this.manager = manager;
        this.networkManager = networkManager;
        this.transport = transport;
        
        Database database = new DatabaseImpl();
        Context context = new Context(NAME, 
                messageFactory, routeTable, database);
        
        contacts = init(context);
        context.setHostFilter(filter);
        
        dht = new DefaultMojitoDHT(context);
        
        bootstrapWorker = new BootstrapWorker(
                dht, connectionServices, 
                hostCatcher, pingRequestFactory, 
                uniqueHostPinger, udpPinger);
        
        contactPusher = new ContactPusher(connectionManager);
    }
    
    @Override
    public void start() throws IOException {
        dht.bind(transport);
        
        // If we're an Ultrapeer we want to notify our firewalled
        // leafs about every new Contact
        if (connectionServices.isActiveSuperNode()) {
            RouteTable routeTable = dht.getRouteTable();
            routeTable.addRouteTableListener(new RouteTableListener() {
                @Override
                public void handleRouteTableEvent(RouteTableEvent event) {
                    processRouteTableEvent(event);
                }
            });
        }
        
        Contact[] contacts = null;
        synchronized (this) {
            contacts = this.contacts;
            this.contacts = null;
        }
        
        bootstrapWorker.start(contacts);
    }
    
    @Override
    public void close() throws IOException {
        IoUtils.closeAll(contactPusher, 
                bootstrapWorker, 
                dht);
        
        write();
    }
    
    @Override
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    public BootstrapWorker getBootstrapWorker() {
        return bootstrapWorker;
    }
    
    public PassiveRouteTable getPassiveRouteTable() {
        return routeTable;
    }
    
    private Contact[] init(Context context) throws UnknownHostException {
        LocalContact localhost = context.getLocalNode();
        updateLocalhost(localhost);
        localhost.setFirewalled(true);
        
        return read();
    }
    
    private SocketAddress getExternalAddress() 
            throws UnknownHostException {
        InetAddress address = InetAddress.getByAddress(
                networkManager.getAddress());
        int port = networkManager.getPort();
        
        return new InetSocketAddress(address, port);
    }
    
    private void updateLocalhost(LocalContact localhost) 
            throws UnknownHostException {
        localhost.setVendor(DHTManager.VENDOR);
        localhost.setVersion(DHTManager.VERSION);
        localhost.setContactAddress(getExternalAddress());        
        localhost.nextInstanceID();
    }
    
    @Override
    public void addressChanged() {
        try {
            LocalContact contact = (LocalContact)dht.getLocalNode();
            updateLocalhost(contact);
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    private void processRouteTableEvent(RouteTableEvent event) {
        switch (event.getEventType()) {
            case ADD_ACTIVE_CONTACT:
            case ADD_CACHED_CONTACT:
            case UPDATE_CONTACT:
                Contact node = event.getContact();
                if (isLocalhost(node)) {
                    contactPusher.addContact(node);
                }
                break;
        }
    }
    
    @Override
    public boolean isRunning() {
        return dht.isBound();
    }
    
    @Override
    public boolean isReady() {
        return dht.isBound() && dht.isReady();
    }
    
    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        return dht.get(key);
    }

    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return dht.put(key, value);
    }
    
    @Override
    public Contact[] getActiveContacts(int max) {
        RouteTable routeTable = dht.getRouteTable();
        Collection<Contact> contacts = ContactUtils.sort(
                routeTable.getActiveContacts(), max + 1);
        return contacts.toArray(new Contact[0]);
    }

    public void addLeafNode(SocketAddress address) {
        
        if (!isReady()) {
            addActiveNode(address);
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding host to leaf DHT nodes: " + address);
        }
        
        routeTable.addLeafNode(address);
    }
    
    public boolean removeLeafNode(SocketAddress address) {
        SocketAddress removed = routeTable.removeLeafNode(address);
        
        if (LOG.isDebugEnabled() && removed != null) {
            LOG.debug("Removed host from leaf DHT nodes: " + removed);
        }
        
        return removed != null;
    }
    
    @Override
    public void addActiveNode(SocketAddress address) {
        if (isReady()) {
            //contactSink.addActiveNode(address);
        } else {
            bootstrapWorker.addActiveNode(address);
        }
    }
    
    @Override
    public void addPassiveNode(SocketAddress address) {
        if (!isReady()) {
            bootstrapWorker.addActiveNode(address);
        }
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        // Ignore everything that's not a Connection Event
        Connection connection = evt.getConnection();
        if (connection == null) {
            return;
        }
        
        String host = connection.getAddress();
        int port = connection.getPort();
        
        if (evt.isConnectionClosedEvent()) {
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Got a connection closed event for connection: "+ connection);
            }
            
            removeLeafNode(new InetSocketAddress(host, port));
            
        } else if (evt.isConnectionCapabilitiesEvent()){
            
            ConnectionCapabilities capabilities 
                = connection.getConnectionCapabilities();
            
            if (capabilities.remostHostIsActiveDHTNode() > -1) {
                
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connection is active dht node: "+ connection);
                }
                addLeafNode(new InetSocketAddress(host, port));
                
            } else if(capabilities.remostHostIsPassiveDHTNode() > -1) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connection is passive dht node: "+ connection);
                }
                addPassiveNode(new InetSocketAddress(host, port));
                
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is node not connected to the DHT network: "+ connection);
                }
                
                removeLeafNode(new InetSocketAddress(host, port));
            }
        }
    }
    
    private static Contact[] read() {
        List<Contact> contacts = new ArrayList<Contact>();
        
        // Load the small list of MRS Nodes for bootstrap
        if (DHTSettings.PERSIST_PASSIVE_DHT_ROUTETABLE.getValue()
                && PASSIVE_FILE.exists() && PASSIVE_FILE.isFile()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(
                            new BufferedInputStream(
                                new SecureInputStream(
                                    new FileInputStream(PASSIVE_FILE))));
                
                int routeTableVersion = ois.readInt();
                if (routeTableVersion >= DHTSettings.ACTIVE_DHT_ROUTETABLE_VERSION.getValue()) {
                    Contact node = null;
                    while ((node = (Contact)ois.readObject()) != null) {
                        contacts.add(node);
                    }
                }
            } catch (Throwable ignored) {
                LOG.error("Throwable", ignored);
            } finally {
                IOUtils.close(ois);
            }
        }
        
        return contacts.toArray(new Contact[0]);
    }
    
    private void write() {
        if (DHTSettings.PERSIST_PASSIVE_DHT_ROUTETABLE.getValue()) {
            Collection<Contact> contacts = routeTable.getActiveContacts(); 
            if (contacts.size() >= 2) {
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(
                                new BufferedOutputStream(
                                    new SecureOutputStream(
                                        new FileOutputStream(PASSIVE_FILE))));
                    
                    oos.writeInt(DHTSettings.ACTIVE_DHT_ROUTETABLE_VERSION.getValue());
                    
                    // Sort by MRS and save only some Nodes
                    contacts = ContactUtils.sort(contacts, 
                            DHTSettings.MAX_PERSISTED_NODES.getValue());
                    
                    KUID localNodeID = getMojitoDHT().getLocalNodeID();
                    for(Contact node : contacts) {
                        if(!node.getNodeID().equals(localNodeID)) {
                            oos.writeObject(node);
                        }
                    }
                    
                    // EOF Terminator
                    oos.writeObject(null);
                    oos.flush();
                    
                } catch (IOException ignored) {
                } finally {
                    IOUtils.close(oos);
                }
            }
        }
    }
}
