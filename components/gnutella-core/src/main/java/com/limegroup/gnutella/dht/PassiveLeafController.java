package com.limegroup.gnutella.dht;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.DefaultDHT;
import org.limewire.mojito.DefaultMojitoDHT;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.ValueKey;
import org.limewire.mojito.BootstrapManager.State;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTValueFuture;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.BootstrapConfig;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.storage.Database;
import org.limewire.mojito.storage.DatabaseImpl;
import org.limewire.mojito.storage.Value;
import org.limewire.mojito.util.HostFilter;

import com.google.inject.Inject;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

/**
 * The {@link PassiveController} is controlling a {@link MojitoDHT}
 * instance that is running in {@link DHTMode#PASSIVE_LEAF} mode.
 */
public class PassiveLeafController extends AbstractController {

    private static final String NAME = "LeafDHT";
    
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
        
        Database database = new DatabaseImpl();
        
        dht = new LeafContext(NAME, 
                messageFactory, routeTable, database);
        dht.setHostFilter(hostFilter);
        init(dht);
    }
    
    private void init(DefaultDHT context) 
            throws UnknownHostException {
        LocalContact localhost = context.getLocalhost();
        initLocalhost(localhost);
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
        return isRunning() && dht.isReady();
    }
    
    @Override
    public boolean isBooting() {
        return isRunning() && dht.isBooting();
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }
    
    @Override
    public DHTFuture<ValueEntity> get(ValueKey key) {
        return dht.get(key);
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
    public DHTFuture<ValueEntity[]> getAll(ValueKey key) {
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
            
            // Passive-Leafs are out of the box ready!
            getBootstrapManager().setCustomState(State.READY);
        }
        
        @Override
        protected DHTFuture<BootstrapEntity> bootstrap(
                BootstrapConfig config, long timeout, TimeUnit unit) {
            return new DHTValueFuture<BootstrapEntity>(
                    new UnsupportedOperationException());
        }
    }
}
