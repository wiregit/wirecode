package com.limegroup.gnutella.dht2;

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
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTableImpl;
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
import com.limegroup.gnutella.dht2.BootstrapWorker.BootstrapListener;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.PingRequestFactory;

public class ActiveController extends SimpleController {
    
    private static final Log LOG 
        = LogFactory.getLog(ActiveController.class);
    
    private static final String NAME = "ACTIVE";
    
    public static final File ACTIVE_FILE 
        = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
    
    private final AtomicBoolean collision = new AtomicBoolean(false);
    
    private final DHTManager manager;
    
    private final ContactPusher contactPusher;
    
    private final ContactSink contactSink;
    
    private final MojitoDHT dht;
    
    private final BootstrapWorker bootstrapWorker;
    
    private Contact[] contacts = null;
    
    @Inject
    public ActiveController(DHTManager manager,
            NetworkManager networkManager,
            Transport transport, 
            Provider<ConnectionManager> connectionManager,
            Provider<HostCatcher> hostCatcher,
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            MessageFactory messageFactory, 
            ConnectionServices connectionServices,
            HostFilter filter,
            Provider<UDPPinger> udpPinger) throws IOException {
        super(DHTMode.ACTIVE, 
                transport, 
                networkManager, 
                connectionServices);
        
        this.manager = manager;
        
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
        
        dht = new DefaultMojitoDHT(context);
        
        bootstrapWorker = new BootstrapWorker(
                dht, connectionServices, 
                hostCatcher, pingRequestFactory, 
                uniqueHostPinger, udpPinger);
        
        contactSink = new ContactSink(dht);
        contactPusher = new ContactPusher(connectionManager);
        bootstrapWorker.addBootstrapListener(new BootstrapListener() {
            @Override
            public void handleReady() {
            }
            
            @Override
            public void handleCollision(CollisionException ex) {
                collision.set(true);
                ACTIVE_FILE.delete();
            }
        });
    }
    
    @Override
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    public BootstrapWorker getBootstrapWorker() {
        return bootstrapWorker;
    }
    
    private Contact[] init(Context context) throws IOException {
        
        LocalContact contact = context.getLocalNode();
        initLocalhost(context.getLocalNode());
        
        RouteTable existing = read();
        
        if (existing != null) {
            // Take the KUID from the persisted RouteTable and
            // change override our KUID with it.
            Contact other = existing.getLocalNode();
            contact.setNodeID(other.getNodeID());
            
            // Take all Contacts and use 'em for bootstrapping
            Collection<Contact> contacts 
                = existing.getContacts();
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
        IoUtils.closeAll(contactSink, 
                contactPusher, 
                bootstrapWorker);
        
        super.close();
        
        if (!collision.get()) {
            write();
        }
    }
    
    @Override
    protected void addContact(Contact contact) {
        contactPusher.addContact(contact);
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
    
    @Override
    public void addActiveNode(SocketAddress address) {
        if (isReady()) {
            contactSink.addActiveNode(address);
        } else {
            bootstrapWorker.addActiveNode(address);
        }
    }
    
    @Override
    public void addPassiveNode(SocketAddress address) {
        if (!dht.isReady()) {
            bootstrapWorker.addPassiveNode(address);
        }
    }
    
    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return dht.put(key, value);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        return dht.get(key);
    }
    
    @Override
    public DHTFuture<ValueEntity[]> getAll(EntityKey key) {
        return dht.getAll(key);
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
    
    private static RouteTable read() {
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
                    
                    return routeTable;
                }
            } catch (Throwable ignored) {
                LOG.error("Throwable", ignored);
            } finally {
                IOUtils.close(in);
            }
        }
        
        return null;
    }
}
