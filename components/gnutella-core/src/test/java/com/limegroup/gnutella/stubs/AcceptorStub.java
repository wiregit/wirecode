package com.limegroup.gnutella.stubs;

import org.limewire.inject.Providers;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ProviderHacks;

/** An acceptor that doesn't accept incoming connections. */
public class AcceptorStub extends Acceptor {
    public AcceptorStub() {
        super(ProviderHacks.getNetworkManager(),
                Providers.of(ProviderHacks
                .getUdpService()), Providers.of(ProviderHacks
                .getMulticastService()), Providers.of(ProviderHacks
                .getConnectionDispatcher()), ProviderHacks
                .getBackgroundExecutor(), Providers.of(ProviderHacks
                .getActivityCallback()), Providers.of(ProviderHacks
                .getConnectionManager()), Providers.of(ProviderHacks
                .getIpFilter()), ProviderHacks.getConnectionServices(),
                Providers.of(ProviderHacks.getUPnPManager()));
    }

    public boolean acceptedIncoming=true;
    public boolean acceptedIncoming() {
        return acceptedIncoming;
    }
}
