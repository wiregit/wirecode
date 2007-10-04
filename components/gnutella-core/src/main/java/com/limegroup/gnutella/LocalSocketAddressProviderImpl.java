package com.limegroup.gnutella;

import org.limewire.io.LocalSocketAddressProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SSLSettings;

@Singleton
public class LocalSocketAddressProviderImpl implements LocalSocketAddressProvider {
    
    private final NetworkManager networkManager;
    
    @Inject
    public LocalSocketAddressProviderImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    public byte[] getLocalAddress() {
        return networkManager.getAddress();
    }

    public int getLocalPort() {
        return networkManager.getPort();
    }

    public boolean isLocalAddressPrivate() {
        return ConnectionSettings.LOCAL_IS_PRIVATE.getValue();
    }
    
    public boolean isTLSCapable() {
        return SSLSettings.isIncomingTLSEnabled();
    }

}
