package com.limegroup.gnutella.stubs;

import org.limewire.inject.Providers;

import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.ProviderHacks;

@Deprecated
public class HackHostCatcher extends HostCatcher {

    public HackHostCatcher() {
        super(ProviderHacks.getBackgroundExecutor(), ProviderHacks
                .getConnectionServices(), Providers.of(ProviderHacks
                .getConnectionManager()), Providers.of(ProviderHacks
                .getUdpService()), Providers.of(ProviderHacks.getDHTManager()),
                Providers.of(ProviderHacks.getQueryUnicaster()), Providers
                        .of(ProviderHacks.getIpFilter()), Providers
                        .of(ProviderHacks.getMulticastService()),
                        ProviderHacks.getUniqueHostPinger(),
                        ProviderHacks.getUDPHostCacheFactory(),
                        ProviderHacks.getPingRequestFactory());
    }

}
