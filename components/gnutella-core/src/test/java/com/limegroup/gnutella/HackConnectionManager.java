package com.limegroup.gnutella;

import org.limewire.concurrent.Providers;

public class HackConnectionManager extends ConnectionManager {

    public HackConnectionManager() {
        super(ProviderHacks.getNetworkManager(), Providers.of(ProviderHacks
                .getHostCatcher()), Providers.of(ProviderHacks
                .getConnectionDispatcher()), ProviderHacks
                .getBackgroundExecutor(), Providers.of(ProviderHacks
                .getSimppManager()), ProviderHacks.getCapabilitiesVMFactory(),
                ProviderHacks.getManagedConnectionFactory(), Providers
                        .of(ProviderHacks.getMessageRouter()), Providers
                        .of(ProviderHacks.getQueryUnicaster()), ProviderHacks
                        .getSocketsManager(), ProviderHacks
                        .getConnectionServices(), Providers.of(ProviderHacks
                        .getNodeAssigner()), Providers.of(ProviderHacks
                        .getIpFilter()), ProviderHacks
                        .getConnectionCheckerManager(),
                        ProviderHacks.getPingRequestFactory());

    }

}
