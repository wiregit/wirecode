package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.storage.DHTValueFactory;
import org.limewire.mojito2.storage.StorableModel;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.db.AltLocModel;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;

/**
 * Glues together DHT connection functionality.
 */
@Singleton
public class DHTControllerFacadeImpl implements DHTControllerFacade {
    
    private final NetworkManager networkManager;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<IPFilter> ipFilter;
    private final SpamServices spamServices;
    private final ScheduledExecutorService backgroundExecutor;
    private final CapabilitiesVMFactory capabilitiesVMFactory;
    private final ConnectionServices connectionServices;
    private final Provider<AltLocValueFactory> altLocValueFactory;
    private final Provider<PushProxiesValueFactory> pushProxyValueFactory;
    private final Provider<AltLocModel> altLocModel;
    private final Provider<MessageDispatcherFactory> messageDispatcherFactory;
    private final DHTBootstrapperFactory dhtBootstrapperFactory;
    private final Provider<SecurityToken.TokenProvider> securityTokenProvider;
    private final Provider<MACCalculatorRepositoryManager> MACCalculatorRepositoryManager;
        
    @Inject
    public DHTControllerFacadeImpl(NetworkManager networkManager,
            Provider<ConnectionManager> connectionManager,
             Provider<IPFilter> ipFilter, // TODO: maybe hostileFilter here 
            SpamServices spamServices,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            CapabilitiesVMFactory capabilitiesVMFactory,
            ConnectionServices connectionServices,
            Provider<AltLocValueFactory> altLocValueFactory,
            Provider<PushProxiesValueFactory> pushProxyValueFactory,
            Provider<AltLocModel> altLocModel,
            Provider<MessageDispatcherFactory> messageDispatcherFactory,
            DHTBootstrapperFactory dhtBootstrapperFactory,
            Provider<SecurityToken.TokenProvider> securityTokenProvider,
            Provider<MACCalculatorRepositoryManager> MACCalculatorRepositoryManager) {
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
        this.spamServices = spamServices;
        this.backgroundExecutor = backgroundExecutor;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.connectionServices = connectionServices;
        this.altLocValueFactory = altLocValueFactory;
        this.pushProxyValueFactory = pushProxyValueFactory;
        this.altLocModel = altLocModel;
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.dhtBootstrapperFactory = dhtBootstrapperFactory;
        this.securityTokenProvider = securityTokenProvider;
        this.MACCalculatorRepositoryManager = MACCalculatorRepositoryManager;
    }
    
    @Override
    public boolean allow(SocketAddress addr) {
        return ipFilter.get().allow(addr);
    }
    
    @Override
    public byte[] getAddress() {
        return networkManager.getAddress();
    }
    
    @Override
    public StorableModel getAltLocModel() {
        return altLocModel.get();
    }
    
    @Override
    public DHTValueFactory getAltLocValueFactory() {
        return altLocValueFactory.get();
    }
    
    @Override
    public List<RoutedConnection> getInitializedClientConnections() {
        return connectionManager.get().getInitializedClientConnections();
    }
    
    @Override
    public MessageDispatcherFactory getMessageDispatcherFactory() {
        return messageDispatcherFactory.get();
    }
    
    @Override
    public int getPort() {
        return networkManager.getPort();
    }

    @Override
    public DHTValueFactory getPushProxyValueFactory() {
        return pushProxyValueFactory.get();
    }
    
    @Override
    public boolean isActiveSupernode() {
        return connectionServices.isActiveSuperNode();
    }
    
    @Override
    public boolean isConnected() {
        return connectionServices.isConnected();
    }
    
    @Override
    public void reloadIPFilter() {
        spamServices.reloadIPFilter();
    }
    
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable runner, long initialDelay, 
            long delay, TimeUnit milliseconds) {
        return backgroundExecutor.scheduleWithFixedDelay(
                runner, initialDelay, delay, milliseconds);
    }
    
    @Override
    public void sendUpdatedCapabilities() {
        connectionManager.get().sendUpdatedCapabilities();        
    }
    
    @Override
    public void updateCapabilities() {
        capabilitiesVMFactory.updateCapabilities();
    }

    @Override
    public DHTBootstrapper getDHTBootstrapper(DHTController dhtController) {
        return dhtBootstrapperFactory.createBootstrapper(dhtController);
    }
    
    @Override
    public SecurityToken.TokenProvider getSecurityTokenProvider() {
        return securityTokenProvider.get();
    }
    
    @Override
    public MACCalculatorRepositoryManager getMACCalculatorRespositoryManager() {
        return MACCalculatorRepositoryManager.get();
    }
}
