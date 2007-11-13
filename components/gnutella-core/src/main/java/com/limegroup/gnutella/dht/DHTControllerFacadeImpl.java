package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.StorableModel;
import org.limewire.mojito.io.MessageDispatcherFactory;

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
import com.limegroup.gnutella.dht.db.PrivateGroupsModel;
import com.limegroup.gnutella.dht.db.PrivateGroupsValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesModel;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;

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
    private final Provider<PrivateGroupsValueFactory> privateGroupsValueFactory;
    private final Provider<AltLocModel> altLocModel;
    private final Provider<PushProxiesModel> pushProxyModel;
    private final Provider<PrivateGroupsModel> privateGroupsModel;
    private final Provider<MessageDispatcherFactory> messageDispatcherFactory;
    private final DHTBootstrapperFactory dhtBootstrapperFactory;
        
    @Inject
    public DHTControllerFacadeImpl(NetworkManager networkManager,
            Provider<ConnectionManager> connectionManager,
            @Named("ipFilter") Provider<IPFilter> ipFilter, // TODO: maybe hostileFilter here 
            SpamServices spamServices,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            CapabilitiesVMFactory capabilitiesVMFactory,
            ConnectionServices connectionServices,
            Provider<AltLocValueFactory> altLocValueFactory,
            Provider<PushProxiesValueFactory> pushProxyValueFactory,
            Provider<PrivateGroupsValueFactory> privateGroupsValueFactory,
            Provider<AltLocModel> altLocModel,
            Provider<PushProxiesModel> pushProxyModel,
            Provider<PrivateGroupsModel> privateGroupsModel,
            Provider<MessageDispatcherFactory> messageDispatcherFactory,
            DHTBootstrapperFactory dhtBootstrapperFactory) {
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
        this.spamServices = spamServices;
        this.backgroundExecutor = backgroundExecutor;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.connectionServices = connectionServices;
        this.altLocValueFactory = altLocValueFactory;
        this.pushProxyValueFactory = pushProxyValueFactory;
        this.privateGroupsValueFactory = privateGroupsValueFactory;
        this.altLocModel = altLocModel;
        this.pushProxyModel = pushProxyModel;
        this.privateGroupsModel = privateGroupsModel;
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.dhtBootstrapperFactory = dhtBootstrapperFactory;
    }
    
    public boolean allow(SocketAddress addr) {
        return ipFilter.get().allow(addr);
    }
    
    public byte[] getAddress() {
        return networkManager.getAddress();
    }
    
    public StorableModel getAltLocModel() {
        return altLocModel.get();
    }
    
    public DHTValueFactory getAltLocValueFactory() {
        return altLocValueFactory.get();
    }
    
    public List<RoutedConnection> getInitializedClientConnections() {
        return connectionManager.get().getInitializedClientConnections();
    }
    
    public MessageDispatcherFactory getMessageDispatcherFactory() {
        return messageDispatcherFactory.get();
    }
    
    public int getPort() {
        return networkManager.getPort();
    }
    
    public StorableModel getPushProxyModel() {
        return pushProxyModel.get();
    }
    
    public DHTValueFactory getPushProxyValueFactory() {
        return pushProxyValueFactory.get();
    }
    
    public StorableModel getPrivateGroupsModel(){
        return privateGroupsModel.get();
    }
    public DHTValueFactory getPrivateGroupsValueFactory(){
        return privateGroupsValueFactory.get();
    }
    
    public boolean isActiveSupernode() {
        return connectionServices.isActiveSuperNode();
    }
    
    public boolean isConnected() {
        return connectionServices.isConnected();
    }
    
    public void reloadIPFilter() {
        spamServices.reloadIPFilter();
    }
    
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runner, long initialDelay, long delay, TimeUnit milliseconds) {
        return backgroundExecutor.scheduleWithFixedDelay(runner, initialDelay, delay, milliseconds);
    }
    
    public void sendUpdatedCapabilities() {
        connectionManager.get().sendUpdatedCapabilities();        
    }
    
    public void updateCapabilities() {
        capabilitiesVMFactory.updateCapabilities();
    }

    public DHTBootstrapper getDHTBootstrapper(DHTController dhtController) {
        return dhtBootstrapperFactory.createBootstrapper(dhtController);
    }

}
