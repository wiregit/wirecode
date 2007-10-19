package com.limegroup.gnutella.stubs;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.inject.Providers;
import org.limewire.io.Connectable;
import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.ManagedConnectionFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HandshakeStatus;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.util.SocketsManager;

/** A (incomplete!) stub for ConnectionManager. */
@Singleton
public class ConnectionManagerStub extends ConnectionManager {

    private boolean enableRemove = false;

    private Boolean connected;

    private Set<? extends Connectable> pushProxies;

    private Boolean fullyConnected;

    private List<ManagedConnection> initializedConnections;

    private Integer preferredConnectionCount;

    @Inject
    public ConnectionManagerStub(NetworkManager networkManager, Provider<HostCatcher> hostCatcher,
            @Named("global")
            Provider<ConnectionDispatcher> connectionDispatcher, @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor, Provider<SimppManager> simppManager,
            CapabilitiesVMFactory capabilitiesVMFactory,
            ManagedConnectionFactory managedConnectionFactory,
            Provider<MessageRouter> messageRouter, Provider<QueryUnicaster> queryUnicaster,
            SocketsManager socketsManager, ConnectionServices connectionServices,
            Provider<NodeAssigner> nodeAssigner, 
            @Named("ipFilter") Provider<IPFilter> ipFilter,
            ConnectionCheckerManager connectionCheckerManager, PingRequestFactory pingRequestFactory) {
        super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor, simppManager,
                capabilitiesVMFactory, managedConnectionFactory, messageRouter, queryUnicaster,
                socketsManager, connectionServices, nodeAssigner, ipFilter,
                connectionCheckerManager, pingRequestFactory);
    }

    @Deprecated
    public ConnectionManagerStub() {
        super(ProviderHacks.getNetworkManager(), Providers.of(ProviderHacks.getHostCatcher()),
                Providers.of(ProviderHacks.getConnectionDispatcher()), ProviderHacks
                        .getBackgroundExecutor(), Providers.of(ProviderHacks.getSimppManager()),
                ProviderHacks.getCapabilitiesVMFactory(), ProviderHacks
                        .getManagedConnectionFactory(), Providers.of(ProviderHacks
                        .getMessageRouter()), Providers.of(ProviderHacks.getQueryUnicaster()),
                ProviderHacks.getSocketsManager(), ProviderHacks.getConnectionServices(), Providers
                        .of(ProviderHacks.getNodeAssigner()), Providers.of(ProviderHacks
                        .getIpFilter()), ProviderHacks.getConnectionCheckerManager(), ProviderHacks
                        .getPingRequestFactory());

    }

    /** Calls c.close iff enableRemove */
    @Override
    public void remove(ManagedConnection c) {
        if (enableRemove)
            c.close();
    }

    @Override
    public boolean isConnected() {
        if(connected != null)
            return connected;
        else
            return super.isConnected();
    }

    @Override
    public boolean isSupernode() {
        return true;
    }

    @Override
    public HandshakeStatus allowConnection(HandshakeResponse hr) {
        return HandshakeStatus.OK;
    }

    @Override
    public String toString() {
        return "ConnectionManagerStub";
    }

    public void setEnableRemove(boolean enableRemove) {
        this.enableRemove = enableRemove;
    }

    public void setConnected(boolean connected) {
        this.connected = Boolean.valueOf(connected);
    }

    public void setPushProxies(Set<? extends Connectable> pushProxies) {
        this.pushProxies = pushProxies;
    }

    @Override
    public Set<? extends Connectable> getPushProxies() {
        if (pushProxies != null)
            return pushProxies;
        else
            return super.getPushProxies();
    }

    public void setFullyConnected(boolean fullyConnected) {
        this.fullyConnected = Boolean.valueOf(fullyConnected);
    }
    

    public void setInitializedConnections(List<ManagedConnection> initializedConnections) {
        this.initializedConnections = initializedConnections;
    }

    public void setPreferredConnectionCount(int i) {
        this.preferredConnectionCount = Integer.valueOf(i);
    }


    @Override
    public List<ManagedConnection> getInitializedConnections() {
        if(initializedConnections != null)
            return initializedConnections;
        else
            return super.getInitializedConnections();
    }

    @Override
    public int getPreferredConnectionCount() {
        if(preferredConnectionCount != null)
            return preferredConnectionCount;
        else
            return super.getPreferredConnectionCount();
    }

    @Override
    public boolean isFullyConnected() {
        if(fullyConnected != null)
            return fullyConnected;
        else
            return super.isFullyConnected();
    }
    
}
