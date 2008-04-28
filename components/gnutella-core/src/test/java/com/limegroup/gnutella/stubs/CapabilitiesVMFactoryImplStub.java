package com.limegroup.gnutella.stubs;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactoryImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMImpl.SupportedMessageBlock;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;

@Singleton
public class CapabilitiesVMFactoryImplStub extends CapabilitiesVMFactoryImpl {

    Set<CapabilitiesVMImpl.SupportedMessageBlock> added = new HashSet<CapabilitiesVMImpl.SupportedMessageBlock>();
    
    @Inject
    public CapabilitiesVMFactoryImplStub(Provider<DHTManager> dhtManager,
            Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
            Provider<NetworkManager> networkManager) {
        super(dhtManager, simppManager, updateHandler, networkManager);
    }

    public void addMessageBlock(CapabilitiesVMImpl.SupportedMessageBlock messageBlock) {
        added.add(messageBlock);
        updateCapabilities();
    }
    
    @Override
    protected Set<SupportedMessageBlock> getSupportedMessages() {
        Set<SupportedMessageBlock> methods = super.getSupportedMessages();
        methods.addAll(added);
        return methods;
    }
    
}
