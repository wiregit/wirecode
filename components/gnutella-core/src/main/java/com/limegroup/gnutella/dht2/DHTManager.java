package com.limegroup.gnutella.dht2;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.lifecycle.Service;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.message.DefaultMessageFactory;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.settings.ContextSettings;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.util.EventUtils;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.mojito2.util.IoUtils;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

@Singleton
public class DHTManager implements ConnectionLifecycleListener, Service, Closeable {

    private static final Log LOG 
        = LogFactory.getLog(DHTManager.class);
    
    public static final Vendor VENDOR = ContextSettings.getVendor();
    
    public static final Version VERSION = ContextSettings.getVersion();
    
    /**
     * Defines the modes of a DHT Node (inactive, active, passive and passive leaf).
     */
    public static enum DHTMode {
        
        /**
         * A DHT Node is in INACTIVE mode if it supports the DHT
         * but is currently not capable of joining it.
         * 
         * @see NodeAssigner.java
         */
        INACTIVE(0x00, new byte[]{ 'I', 'D', 'H', 'T' }),
        
        /**
         * A DHT Node is ACTIVE mode if it's a full participant
         * of the DHT, e.g. a non-firewalled Gnutella leave node
         * with a sufficiently stable connection.
         */
        ACTIVE(0x01, new byte[]{ 'A', 'D', 'H', 'T' }),
        
        /**
         * A DHT Node is in PASSIVE mode if it's connected to
         * the DHT but is not part of the global DHT routing table. 
         * Thus, a passive node never receives requests from the DHT 
         * and does necessarily have an accurate knowledge of the DHT
         * structure. However, it can perform queries and requests stores.
         */
        PASSIVE(0x02, new byte[]{ 'P', 'D', 'H', 'T' }),
        
        /**
         * The PASSIVE_LEAF mode is very similar to PASSIVE mode with
         * two major differences:
         * <pre>
         * 1) A passive leaf has a fixed size LRU Map as its RouteTable.
         *    That means it has almost no knowledge of the global DHT
         *    RouteTable and depends entirely on an another peer (Ultrapeer)
         *    that feeds it continuously with fresh contacts.
         * 
         * 2) A passive leaf node does not perform any Kademlia maintenance
         *    operations!</pre>
         */
        PASSIVE_LEAF(0x03, new byte[]{ 'L', 'D', 'H', 'T' });
        
        public static final byte DHT_MODE_MASK = 0x0F;
        
        private final int mode;
        
        private final byte[] capabilityName;
        
        private DHTMode(int mode, byte[] capabilityName) {
            assert (capabilityName.length == 4);
            this.mode = mode;
            this.capabilityName = capabilityName;
        }
        
        /**
         * Returns the mode as byte.
         */
        public byte byteValue() {
            return (byte)(mode & 0xFF);
        }
        
        /**
         * Returns the VM capability name.
         */
        public byte[] getCapabilityName() {
            return capabilityName.clone();
        }
        
        private static final DHTMode[] MODES;
        
        static {
            DHTMode[] modes = values();
            MODES = new DHTMode[modes.length];
            for (DHTMode m : modes) {
                int index = (m.mode & 0xFF) % MODES.length;
                assert (MODES[index] == null);
                MODES[index] = m;
            }
        }
        
        /**
         * Returns a DHTMode enum for the given mode and null
         * if no such DHTMode exists.
         */
        public static DHTMode valueOf(int mode) {
            int index = (mode & 0xFF) % MODES.length;
            DHTMode s = MODES[index];
            if (s.mode == mode) {
                return s;
            }
            return null;
        }
    }
    
    /**
     * List of event listeners for ConnectionLifeCycleEvents.
     */
    private final List<DHTEventListener> listeners 
        = new CopyOnWriteArrayList<DHTEventListener>();
    
    private final NetworkManager networkManager;
    
    private final Provider<UDPService> udpService;
    
    private final Provider<MessageRouter> messageRouter;

    private final Provider<ConnectionManager> connectionManager;
    
    private final Provider<MACCalculatorRepositoryManager> calculator;
    
    private final Provider<IPFilter> ipFilter;
    
    private final ConnectionServices connectionServices;
    
    private volatile Controller controller = InactiveController.CONTROLLER;
    
    private volatile boolean enabled = true;
    
    private boolean open = true;
    
    @Inject
    public DHTManager(NetworkManager networkManager,
            com.limegroup.gnutella.messages.MessageFactory messageFactory,
            Provider<UDPService> udpService, 
            Provider<MessageRouter> messageRouter, 
            Provider<MACCalculatorRepositoryManager> calculator,
            Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter,
            ConnectionServices connectionServices) {
        
        this.networkManager = networkManager;
        this.udpService = udpService;
        this.messageRouter = messageRouter;
        this.calculator = calculator;
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
        this.connectionServices = connectionServices;
        
        messageFactory.setParser(
                (byte) org.limewire.mojito2.message.Message.F_DHT_MESSAGE, 
                new MojitoMessageParser());
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Mojito DHT");
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
        start(DHTMode.INACTIVE);
    }
    
    
    public synchronized boolean start(DHTMode mode) {
        if (!open) {
            throw new IllegalStateException();
        }
        
        if (controller.isMode(mode)) {
            return true;
        }
        
        stop();
        
        try {
            switch (mode) {
                case INACTIVE:
                    controller = InactiveController.CONTROLLER;
                    break;
                case ACTIVE:
                    controller = createActive();
                    break;
                case PASSIVE:
                    controller = createPassive();
                    break;
                case PASSIVE_LEAF:
                    controller = createLeaf();
                    break;
            }
        
            controller.start();
            return true;
        } catch (IOException err) {
            LOG.error("IOException", err);
            stop();
        }
        
        return false;
    }

    public synchronized void stop() {
        IoUtils.close(controller);
        controller = InactiveController.CONTROLLER;
    }
    
    @Override
    public synchronized void close() {
        open = false;
        stop();
    }
    
    public synchronized Controller getController() {
        return controller;
    }

    public synchronized boolean isRunning() {
        return controller.isRunning();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        if (!DHTSettings.DISABLE_DHT_NETWORK.getValue() 
                && !DHTSettings.DISABLE_DHT_USER.getValue()
                && enabled) {
            return true;
        }
        return false;
    }
    
    public synchronized DHTMode getMode() {
        return controller.getMode();
    }
    
    public synchronized boolean isMode(DHTMode mode) {
        return controller.isMode(mode);
    }
    
    public synchronized void addressChanged() {
        controller.addressChanged();
    }
    
    public synchronized boolean isReady() {
        return controller.isReady();
    }
    
    private Controller createActive() throws IOException {
        MojitoTransport transport = new MojitoTransport(
                udpService, messageRouter);
        
        DefaultMessageFactory messageFactory 
            = new DefaultMessageFactory(calculator.get());
        
        HostFilter hostFilter 
            = new HostFilterDelegate(ipFilter);
        
        return new ActiveController(networkManager, transport, 
                connectionManager, messageFactory, connectionServices, 
                hostFilter);
    }
    
    private Controller createPassive() {
        return new PassiveController();
    }
    
    private Controller createLeaf() {
        return new LeafController();
    }
    
    @Override
    public synchronized void handleConnectionLifecycleEvent(
            ConnectionLifecycleEvent evt) {
        
        if (evt.isDisconnectedEvent() 
                || evt.isNoInternetEvent()) {
            
            if (!DHTSettings.FORCE_DHT_CONNECT.getValue()) {
                stop();
            }
            
        } else {
            
            controller.handleConnectionLifecycleEvent(evt);
        }
    }
    
    public void handleDHTContactsMessage(DHTContactsMessage msg) {
        
    }
    
    public synchronized DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return controller.put(key, value);
    }
    
    public synchronized DHTFuture<ValueEntity> get(EntityKey key) {
        return controller.get(key);
    }
    
    public void addActiveDHTNode(SocketAddress address) {
        
    }
    
    public void addPassiveDHTNode(SocketAddress address) {
        
    }

    public void addEventListener(DHTEventListener listener) {
        listeners.add(Objects.nonNull(listener, "listener"));
    }
    
    public void removeEventListener(DHTEventListener listener) {
        listeners.remove(listener);
    }
    
    void dispatchEvent(final DHTEvent evt) {
        if (!listeners.isEmpty()) {
            Runnable event = new Runnable() {
                @Override
                public void run() {
                    for (DHTEventListener l : listeners) {
                        l.handleDHTEvent(evt);
                    }
                }
            };
            
            EventUtils.fireEvent(event);
        }
    }
}
