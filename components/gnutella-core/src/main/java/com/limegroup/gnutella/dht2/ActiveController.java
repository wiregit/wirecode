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
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IOUtils;
import org.limewire.io.SecureInputStream;
import org.limewire.io.SecureOutputStream;
import org.limewire.mojito2.Context;
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
import org.limewire.mojito2.routing.RouteTableImpl;
import org.limewire.mojito2.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito2.routing.RouteTable.RouteTableListener;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.DatabaseImpl;
import org.limewire.mojito2.util.ContactUtils;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.mojito2.util.IoUtils;
import org.limewire.util.CommonUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.connection.Connection;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.PingRequestFactory;

class ActiveController extends AbstractController {
    
    private static final Log LOG 
        = LogFactory.getLog(ActiveController.class);
    
    private static final String NAME = "MojitoDHT";
    
    private static final File ACTIVE_FILE 
        = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
    
    private final DHTManager manager;
    
    private final NetworkManager networkManager;
    
    private final Transport transport;
    
    private final ContactPusher contactPusher;
    
    private final ContactPinger contactPinger;
    
    private final MojitoDHT dht;
    
    private final BootstrapManager bootstrapManager;
    
    private Contact[] contacts = null;
    
    public ActiveController(DHTManager manager,
            NetworkManager networkManager,
            Transport transport, 
            Provider<ConnectionManager> connectionManager,
            Provider<HostCatcher> hostCatcher,
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            MessageFactory messageFactory, 
            ConnectionServices connectionServices,
            HostFilter filter) throws IOException {
        super(DHTMode.ACTIVE, connectionServices);
        
        this.manager = manager;
        this.networkManager = networkManager;
        this.transport = transport;
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        Context context = new Context(NAME, 
                messageFactory, routeTable, database);
        
        contacts = init(context);
        
        context.setHostFilter(filter);
        
        // Memorize the localhost's KUID
        Contact localhost = context.getLocalNode();
        KUID contactId = localhost.getNodeID();
        DHTSettings.DHT_NODE_ID.set(contactId.toHexString());
        
        dht = new MojitoDHT(context);
        
        bootstrapManager = new BootstrapManager(
                dht, connectionServices, hostCatcher, 
                pingRequestFactory, uniqueHostPinger);
        
        contactPinger = new ContactPinger(dht);
        contactPusher = new ContactPusher(connectionManager);
    }
    
    @Override
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    private Contact[] init(Context context) throws IOException {
        LocalContact contact = context.getLocalNode();
        contact.setVendor(DHTManager.VENDOR);
        contact.setVersion(DHTManager.VERSION);
        
        contact.setContactAddress(getExternalAddress());
        
        Handle handle = read();
        
        if (handle != null && handle.routeTable != null) {
            // Take the KUID from the persisted RouteTable and
            // change override our KUID with it.
            Contact other = handle.routeTable.getLocalNode();
            contact.setNodeID(other.getNodeID());
            
            // Take all Contacts and use 'em for bootstrapping
            Collection<Contact> contacts 
                = handle.routeTable.getContacts();
            contacts.remove(other);
            
            return contacts.toArray(new Contact[0]);
        }
        
        return null;
    }
    
    @Override
    public Contact[] getActiveContacts(int max) {
        RouteTable routeTable = dht.getRouteTable();
        Collection<Contact> contacts = ContactUtils.sort(
                routeTable.getActiveContacts(), max + 1);
        return contacts.toArray(new Contact[0]);
    }

    private SocketAddress getExternalAddress() 
            throws UnknownHostException {
        InetAddress address = InetAddress.getByAddress(
                networkManager.getAddress());
        int port = networkManager.getPort();
        
        return new InetSocketAddress(address, port);
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
        
        bootstrapManager.start(contacts);
    }
    
    @Override
    public void close() {
        IoUtils.close(contactPinger);
        IoUtils.close(contactPusher);
        IoUtils.close(bootstrapManager);
        IoUtils.close(dht);
        
        write();
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

    private boolean isLocalhost(Contact contact) {
        Context context = dht.getContext();
        return context.isLocalNode(contact);
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        
        // Ignore everything that's not a Connection Event
        Connection connection = evt.getConnection();
        if (connection == null) {
            return;
        }
        
        if (!evt.isConnectionCapabilitiesEvent()) {
            return;
        }
        
        ConnectionCapabilities capabilities 
            = connection.getConnectionCapabilities();
        String host = connection.getAddress();
        int port = connection.getPort();

        if (capabilities.remostHostIsPassiveDHTNode() > -1) {
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Connection is passive dht node: " + connection);
            }
            
            addPassiveNode(new InetSocketAddress(host, port));
            
        } else if (capabilities.remostHostIsActiveDHTNode() > -1) {
            
            if (DHTSettings.EXCLUDE_ULTRAPEERS.getValue()) {
                return;
            } 
            
            addActiveNode(new InetSocketAddress(host, port));
        } 
    }
    
    private void addActiveNode(SocketAddress address) {
        if (dht.isReady()) {
            contactPinger.addSocketAddress(address);
        } else {
            bootstrapManager.addSocketAddress(address);
        }
    }
    
    private void addPassiveNode(SocketAddress address) {
        
    }
    
    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return dht.put(key, value);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        return dht.get(key);
    }
    
    private void write() {
        if (!DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.getValue()) {
            return;
        }
        
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(
                    new BufferedOutputStream(
                        new SecureOutputStream(
                            new FileOutputStream(ACTIVE_FILE))));
    
            out.writeInt(DHTSettings.ACTIVE_DHT_ROUTETABLE_VERSION.getValue());
            synchronized (dht) {
                out.writeObject(dht.getRouteTable());
                
                Database database = null;
                if (DHTSettings.PERSIST_DHT_DATABASE.getValue()) {
                    database = dht.getDatabase();
                }                    
                out.writeObject(database);
            }
            out.flush();
        } catch (IOException ignored) {
        } finally {
            IOUtils.close(out);
        }
    }
    
    private static Handle read() {
        if (DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.getValue() 
                && ACTIVE_FILE.exists() && ACTIVE_FILE.isFile()) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(
                        new BufferedInputStream(
                            new SecureInputStream(
                                new FileInputStream(ACTIVE_FILE))));
                
                // Pre-condition: The Database depends on the RouteTable
                // If there's no persisted RouteTable or it's corrupt
                // then there's no point in reading or setting the 
                // Database!
                
                int routeTableVersion = in.readInt();
                if (routeTableVersion >= DHTSettings.ACTIVE_DHT_ROUTETABLE_VERSION.getValue()) {
                    RouteTable routeTable = (RouteTable)in.readObject();

                    Database database = null;
                    try {
                        if (DHTSettings.PERSIST_DHT_DATABASE.getValue()) {
                            database = (Database)in.readObject();                                                        
                        }
                    } catch (Throwable ignored) {
                        LOG.error("Throwable", ignored);
                    }
                    
                    // The Database depends on the RouteTable!
                    if (routeTable != null) {
                        long maxElaspedTime 
                            = DHTSettings.MAX_ELAPSED_TIME_SINCE_LAST_CONTACT.getValue();
                        if (maxElaspedTime < Long.MAX_VALUE) {
                            routeTable.purge(maxElaspedTime);
                        }
                    }
                    
                    return new Handle(routeTableVersion, routeTable, database);
                }
            } catch (Throwable ignored) {
                LOG.error("Throwable", ignored);
            } finally {
                IOUtils.close(in);
            }
        }
        
        return null;
    }
    
    private static class Handle {
        
        private final int version;
        
        private final RouteTable routeTable;
        
        private final Database database;
        
        public Handle(int version, RouteTable routeTable, Database database) {
            this.version = version;
            this.routeTable = routeTable;
            this.database = database;
        }
    }
}
