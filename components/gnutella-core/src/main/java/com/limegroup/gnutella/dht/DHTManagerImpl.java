package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IpPort;
import org.limewire.lifecycle.Service;
import org.limewire.mojito.ValueKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.message.DefaultMessageFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.storage.Value;
import org.limewire.mojito.util.HostFilter;
import org.limewire.mojito.util.IoUtils;
import org.limewire.security.MACCalculatorRepositoryManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.BootstrapWorker.BootstrapListener;
import com.limegroup.gnutella.dht.db.AltLocPublisher;
import com.limegroup.gnutella.dht.db.PushProxiesPublisher;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.tigertree.HashTreeCache;

@Singleton
public class DHTManagerImpl extends AbstractDHTManager implements Service {

    private static final Log LOG 
        = LogFactory.getLog(DHTManagerImpl.class);
    
    private final NetworkManager networkManager;
    
    private final Provider<UDPService> udpService;
    
    private final Provider<MessageRouter> messageRouter;

    private final Provider<ConnectionManager> connectionManager;
    
    private final Provider<MACCalculatorRepositoryManager> calculator;
    
    private final ConnectionServices connectionServices;
    
    private final Provider<HostCatcher> hostCatcher;
    
    private final PingRequestFactory pingRequestFactory;
    
    private final Provider<UniqueHostPinger> uniqueHostPinger;
    
    private final Provider<UDPPinger> udpPinger;
    
    private final Provider<CapabilitiesVMFactory> capabilitiesVMFactory;
    
    private final HostFilter hostFilter;
    
    private final AltLocPublisher altLocPublisher;
    
    private final PushProxiesPublisher pushProxiesPublisher;
    
    private Controller controller = InactiveController.CONTROLLER;
    
    private volatile boolean enabled = true;
    
    private boolean open = true;
    
    @Inject
    public DHTManagerImpl(NetworkManager networkManager,
            com.limegroup.gnutella.messages.MessageFactory messageFactory,
            Provider<UDPService> udpService, 
            Provider<MessageRouter> messageRouter, 
            Provider<MACCalculatorRepositoryManager> calculator,
            Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter,
            ConnectionServices connectionServices,
            Provider<HostCatcher> hostCatcher, 
            PingRequestFactory pingRequestFactory,
            Provider<UniqueHostPinger> uniqueHostPinger,
            Provider<UDPPinger> udpPinger,
            Provider<CapabilitiesVMFactory> capabilitiesVMFactory,
            ApplicationServices applicationServices,
            PushEndpointFactory pushEndpointFactory,
            @GnutellaFiles FileView gnutellaFileView, 
            Provider<HashTreeCache> tigerTreeCache) {
        
        this.networkManager = networkManager;
        this.udpService = udpService;
        this.messageRouter = messageRouter;
        this.calculator = calculator;
        this.connectionManager = connectionManager;
        this.connectionServices = connectionServices;
        this.hostCatcher = hostCatcher;
        this.pingRequestFactory = pingRequestFactory;
        this.uniqueHostPinger = uniqueHostPinger;
        this.udpPinger = udpPinger;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        
        this.hostFilter = new DefaultHostFilter(ipFilter);
        
        this.pushProxiesPublisher = new PushProxiesPublisher(
                this, networkManager, applicationServices, pushEndpointFactory);
        this.altLocPublisher = new AltLocPublisher(this, networkManager, 
                applicationServices, gnutellaFileView, tigerTreeCache);
        
        addEventListener(new DHTEventListener() {
            @Override
            public void handleDHTEvent(DHTEvent evt) {
                switch (evt.getType()) {
                    case CONNECTED:
                    case STOPPED:
                        DHTManagerImpl.this.capabilitiesVMFactory.get().updateCapabilities();
                        DHTManagerImpl.this.connectionManager.get().sendUpdatedCapabilities();
                        break;
                }
            }
        });
        
        messageFactory.setParser(
                (byte) org.limewire.mojito.message.Message.F_DHT_MESSAGE, 
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
    
    @Override
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
            fireStarting();
            
            return true;
        } catch (IOException err) {
            LOG.error("IOException", err);
            stop();
        }
        
        return false;
    }
    
    @Override
    public synchronized void stop() {
        altLocPublisher.stop();
        pushProxiesPublisher.stop();
        
        IoUtils.close(controller);
        controller = InactiveController.CONTROLLER;
        fireStopped();
    }
    
    @Override
    public synchronized void close() {
        open = false;
        stop();
        
        IoUtils.closeAll(
                altLocPublisher, 
                pushProxiesPublisher);
    }
    
    @Override
    public Vendor getVendor() {
        return VENDOR;
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public synchronized Controller getController() {
        return controller;
    }
    
    @Override
    public synchronized MojitoDHT getMojitoDHT() {
        return controller.getMojitoDHT();
    }

    @Override
    public synchronized boolean isRunning() {
        return controller.isRunning();
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isEnabled() {
        if (!DHTSettings.DISABLE_DHT_NETWORK.getValue() 
                && !DHTSettings.DISABLE_DHT_USER.getValue()
                && enabled) {
            return true;
        }
        return false;
    }
    
    @Override
    public synchronized DHTMode getMode() {
        return controller.getMode();
    }
    
    @Override
    public synchronized boolean isMode(DHTMode mode) {
        return controller.isMode(mode);
    }
    
    @Override
    public synchronized boolean isReady() {
        return controller.isReady();
    }
    
    /**
     * Returns the {@link AltLocPublisher}
     */
    public AltLocPublisher getAltLocPublisher() {
        return altLocPublisher;
    }
    
    /**
     * Returns the {@link PushProxiesPublisher}
     */
    public PushProxiesPublisher getPushProxiesPublisher() {
        return pushProxiesPublisher;
    }
    
    /**
     * 
     */
    private synchronized void onReady() {
        altLocPublisher.start();
        pushProxiesPublisher.start();
        
        fireConnected();
    }
    
    private synchronized void onCollision(DHTMode mode) {
        stop();
        start(mode);
    }
    
    private Controller createActive() throws IOException {
        
        MojitoTransport transport = new MojitoTransport(
                udpService, messageRouter);
        
        DefaultMessageFactory messageFactory 
            = new DefaultMessageFactory(calculator.get());
        
        ActiveController controller = new ActiveController( 
                networkManager, transport, connectionManager, hostCatcher, 
                pingRequestFactory, uniqueHostPinger, messageFactory, 
                connectionServices, hostFilter, udpPinger);
        
        BootstrapWorker worker 
            = controller.getBootstrapWorker();
        worker.addBootstrapListener(new BootstrapListener() {
            @Override
            public void handleReady() {
                onReady();
            }
            
            @Override
            public void handleCollision(CollisionException ex) {
                onCollision(DHTMode.ACTIVE);
            }
        });
        
        return controller;
    }
    
    private Controller createPassive() throws UnknownHostException {
        MojitoTransport transport = new MojitoTransport(
                udpService, messageRouter);
        
        DefaultMessageFactory messageFactory 
            = new DefaultMessageFactory(calculator.get());
        
        PassiveController controller = new PassiveController(networkManager, 
                transport, connectionManager, hostCatcher, 
                pingRequestFactory, uniqueHostPinger, messageFactory, 
                connectionServices, hostFilter, udpPinger);
        
        BootstrapWorker worker 
            = controller.getBootstrapWorker();
        worker.addBootstrapListener(new BootstrapListener() {
            @Override
            public void handleReady() {
                onReady();
            }
            
            @Override
            public void handleCollision(CollisionException ex) {
                onCollision(DHTMode.PASSIVE);
            }
        });
        
        return controller;
    }
    
    private Controller createLeaf() throws UnknownHostException {
        MojitoTransport transport = new MojitoTransport(
                udpService, messageRouter);
        
        DefaultMessageFactory messageFactory 
            = new DefaultMessageFactory(calculator.get());
        
        return new PassiveLeafController(transport, messageFactory, 
                networkManager, hostFilter);
    }
    
    @Override
    public synchronized Contact[] getActiveContacts(int max) {
        return controller.getActiveContacts(max);
    }
    
    @Override
    public synchronized IpPort[] getActiveIpPort(int max) {
        List<IpPort> ipp = new ArrayList<IpPort>();
        
        for (Contact contact : getActiveContacts(max)) {
            ipp.add(new IpPortContact(contact));
        }
        
        return ipp.toArray(new IpPort[0]);
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
    
    @Override
    public synchronized void handleContactsMessage(DHTContactsMessage msg) {
        controller.handleContactsMessage(msg);
    }
    
    @Override
    public synchronized DHTFuture<StoreEntity> put(KUID key, Value value) {
        return controller.put(key, value);
    }
    
    @Override
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value) {
        return controller.enqueue(key, value);
    }

    @Override
    public synchronized DHTFuture<ValueEntity> get(ValueKey key) {
        return controller.get(key);
    }
    
    @Override
    public synchronized DHTFuture<ValueEntity[]> getAll(ValueKey key) {
        return controller.getAll(key);
    }

    @Override
    public synchronized void addressChanged() {
        controller.addressChanged();
    }
    
    @Override
    public synchronized void addActiveNode(SocketAddress address) {
        controller.addActiveNode(address);
    }
    
    @Override
    public synchronized void addPassiveNode(SocketAddress address) {
        controller.addPassiveNode(address);
    }
}
