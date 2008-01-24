package com.limegroup.gnutella.stubs;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.AcceptorImpl;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.MulticastService;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UPnPManager;
import com.limegroup.gnutella.filters.IPFilter;

@Singleton
public class AcceptorStub extends AcceptorImpl {

    @Inject
    public AcceptorStub(NetworkManager networkManager,
            Provider<UDPService> udpService,
            Provider<MulticastService> multicastService,
            @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<ActivityCallback> activityCallback,
            Provider<ConnectionManager> connectionManager,
             Provider<IPFilter> ipFilter, ConnectionServices connectionServices,
            Provider<UPnPManager> upnpManager) {
        super(networkManager, udpService, multicastService, connectionDispatcher, backgroundExecutor,
                activityCallback, connectionManager, ipFilter, connectionServices, upnpManager);
    }

    @Override
    public void setAcceptedIncoming(boolean incoming) {
        super.setAcceptedIncoming(incoming);
    }
    
}
