package com.limegroup.gnutella.dht;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.limewire.mojito.DefaultMojitoDHT;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.LocalContact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.storage.DHTValue;
import org.limewire.mojito.storage.Database;
import org.limewire.mojito.storage.DatabaseImpl;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.HostFilter;
import org.limewire.mojito.util.IoUtils;
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
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.PingRequestFactory;

public class PassiveController extends SimpleController {

    private static final Log LOG 
        = LogFactory.getLog(PassiveController.class);
    
    private static final String NAME = "PASSIVE";
    
    public static final File PASSIVE_FILE 
        = new File(CommonUtils.getUserSettingsDir(), "passive.mojito");
    
    private final DHTManager manager;
    
    private final PassiveRouteTable routeTable 
        = new PassiveRouteTable();
    
    private final DefaultMojitoDHT dht;
    
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
        super(DHTMode.PASSIVE, 
                transport, 
                networkManager, 
                connectionServices);
        
        this.manager = manager;
        
        Database database = new DatabaseImpl();
        
        dht = new DefaultMojitoDHT(NAME, 
                messageFactory, routeTable, database);
        dht.setHostFilter(filter);
        
        contacts = init(dht);
        
        bootstrapWorker = new BootstrapWorker(
                dht, connectionServices, 
                hostCatcher, pingRequestFactory, 
                uniqueHostPinger, udpPinger);
        
        contactPusher = new ContactPusher(connectionManager);
    }
    
    @Override
    public void start() throws IOException {
        super.start();
        
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
                bootstrapWorker);
        
        super.close();
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
    
    private Contact[] init(DefaultMojitoDHT context) throws UnknownHostException {
        LocalContact localhost = context.getLocalNode();
        initLocalhost(localhost);
        
        return read();
    }
    
    @Override
    protected void addContact(Contact contact) {
        contactPusher.addContact(contact);
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
    public DHTFuture<ValueEntity[]> getAll(EntityKey key) {
        return dht.getAll(key);
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
