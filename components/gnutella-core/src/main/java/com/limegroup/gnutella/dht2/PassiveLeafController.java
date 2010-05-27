package com.limegroup.gnutella.dht2;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.DefaultDHT;
import org.limewire.mojito2.DefaultMojitoDHT;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.BootstrapConfig;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.DatabaseImpl;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.mojito2.util.IoUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

public class PassiveLeafController extends AbstractController {

    private static final String NAME = "LeafDHT";
    
    private final Transport transport;
    
    private final RouteTable routeTable = new PassiveLeafRouteTable(
            DHTManager.VENDOR, DHTManager.VERSION);
    
    private final DefaultMojitoDHT dht;
    
    @Inject
    public PassiveLeafController(Transport transport, 
            MessageFactory messageFactory,
            NetworkManager networkManager,
            HostFilter hostFilter) throws UnknownHostException {
        super(DHTMode.PASSIVE_LEAF, 
                transport, 
                networkManager);
        
        this.transport = transport;
        
        Database database = new DatabaseImpl();
        
        dht = new LeafContext(NAME, 
                messageFactory, routeTable, database);
        dht.setHostFilter(hostFilter);
        init(dht);
    }
    
    private void init(DefaultDHT context) 
            throws UnknownHostException {
        LocalContact localhost = context.getLocalNode();
        initLocalhost(localhost);
    }
    
    @Override
    public void start() throws IOException {
        super.start();
    }

    @Override
    public void close() throws IOException {
        IoUtils.closeAll(dht);
    }
    
    @Override
    public MojitoDHT getMojitoDHT() {
        return dht;
    }
    
    @Override
    public boolean isRunning() {
        return dht.isBound();
    }
    
    @Override
    public boolean isReady() {
        return dht.isReady();
    }
    
    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
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
    public void handleContactsMessage(DHTContactsMessage msg) {
        for (Contact contact : msg.getContacts()) {
            routeTable.add(contact);
        }
    }
    
    private static class LeafContext extends DefaultMojitoDHT {

        public LeafContext(String name, MessageFactory messageFactory, 
                RouteTable routeTable, Database database) {
            super(name, messageFactory, routeTable, database);
        }
        
        @Override
        public boolean isBooting() {
            return false;
        }
        
        @Override
        public boolean isReady() {
            return isBound();
        }

        @Override
        protected DHTFuture<BootstrapEntity> bootstrap(
                BootstrapConfig config, long timeout, TimeUnit unit) {
            return new DHTValueFuture<BootstrapEntity>(
                    new UnsupportedOperationException());
        }
    }
}
