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
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IOUtils;
import org.limewire.io.SecureInputStream;
import org.limewire.io.SecureOutputStream;
import org.limewire.mojito.DefaultMojitoDHT;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.ValueKey;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.Value;
import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.routing.impl.RouteTableImpl;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.HostFilter;
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

/**
 * The {@link ActiveController} is controlling a {@link MojitoDHT}
 * instance that is running in {@link DHTMode#ACTIVE} mode.
 */
public class ActiveController extends AbstractConnectionController {
    
    private static final Log LOG 
        = LogFactory.getLog(ActiveController.class);
    
    private static final String NAME = "ACTIVE";
    
    public static final File ACTIVE_FILE 
        = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
    
    private final AtomicBoolean collision = new AtomicBoolean(false);
    
    private final ContactPusher contactPusher;
    
    private final ContactSink contactSink;
    
    private final DefaultMojitoDHT dht;
    
    private final BootstrapWorker bootstrapWorker;
    
    private Contact[] contacts = null;
    
    @Inject
    public ActiveController(NetworkManager networkManager,
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
        
        DatabaseImpl database = new DatabaseImpl();
        RouteTable routeTable = new RouteTableImpl();
        
        dht = new DefaultMojitoDHT(NAME, 
                messageFactory, routeTable, database);
        
        dht.setHostFilter(filter);
        contacts = init(dht);
        
        // Memorize the localhost's KUID
        Contact localhost = dht.getLocalhost();
        KUID contactId = localhost.getContactId();
        DHTSettings.DHT_NODE_ID.set(contactId.toHexString());
        
        bootstrapWorker = new BootstrapWorker(
                dht, connectionServices, 
                hostCatcher, pingRequestFactory, 
                uniqueHostPinger, udpPinger);
        
        contactSink = new ContactSink(dht);
        contactPusher = new ContactPusher(connectionManager);
        bootstrapWorker.addBootstrapListener(new BootstrapListener() {
            @Override
            public void handleCollision(CollisionException ex) {
                collision.set(true);
                ACTIVE_FILE.delete();
            }

            @Override
            public void handleConnected(boolean success) {
            }

            @Override
            public void handleConnecting() {
            }
        });
    }
    
    @Override
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    /**
     * Returns the {@link BootstrapWorker}.
     */
    public BootstrapWorker getBootstrapWorker() {
        return bootstrapWorker;
    }
    
    private Contact[] init(DefaultMojitoDHT context) throws IOException {
        
        LocalContact contact = context.getLocalhost();
        initLocalhost(context.getLocalhost());
        
        RouteTable existing = read();
        
        if (existing != null) {
            // Take the KUID from the persisted RouteTable and
            // change override our KUID with it.
            Contact other = existing.getLocalNode();
            contact.setNodeID(other.getContactId());
            
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
        return isRunning() && dht.isReady();
    }
    
    @Override
    public boolean isBooting() {
        return isRunning() && dht.isBooting();
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
        IOUtils.close(contactSink, 
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
    public DHTFuture<StoreEntity> put(KUID key, Value value) {
        return dht.put(key, value);
    }
    
    @Override
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value) {
        return dht.enqueue(key, value);
    }

    @Override
    public DHTFuture<ValueEntity> get(ValueKey key) {
        return dht.get(key);
    }
    
    @Override
    public DHTFuture<ValueEntity[]> getAll(ValueKey key) {
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
                in = new MojitoObjectInputStream(
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
                    if (routeTable != null) {
                        long maxElaspedTime 
                            = DHTSettings.MAX_ELAPSED_TIME_SINCE_LAST_CONTACT.getTimeInMillis();
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
