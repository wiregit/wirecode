package com.limegroup.gnutella;

import org.limewire.lws.server.LWSNetworkAdapter;

import com.google.inject.Inject;

public class LWSNetworkAdapterImpl implements LWSNetworkAdapter {

    private final NetworkManager networkManager;
    
    @Inject
    public LWSNetworkAdapterImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    @Override
    public byte[] getExternalAddress() {
        return networkManager.getExternalAddress();
    }
}
