package com.limegroup.gnutella.stubs;

import org.limewire.concurrent.Providers;

import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.ProviderHacks;

public class HackHostCatcher extends HostCatcher {

    public HackHostCatcher() {
        super(ProviderHacks.getBackgroundExecutor(), ProviderHacks
                .getConnectionServices(), Providers.of(ProviderHacks
                .getConnectionManager()), Providers.of(ProviderHacks
                .getUdpService()), Providers.of(ProviderHacks.getDHTManager()),
                Providers.of(ProviderHacks.getQueryUnicaster()), Providers
                        .of(ProviderHacks.getIpFilter()), Providers
                        .of(ProviderHacks.getMulticastService()));
    }

}
