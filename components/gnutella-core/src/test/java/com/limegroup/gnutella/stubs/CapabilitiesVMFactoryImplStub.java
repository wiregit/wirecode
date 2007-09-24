package com.limegroup.gnutella.stubs;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactoryImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM.SupportedMessageBlock;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;

@Singleton
public class CapabilitiesVMFactoryImplStub extends CapabilitiesVMFactoryImpl {

    Set<CapabilitiesVM.SupportedMessageBlock> added = new HashSet<CapabilitiesVM.SupportedMessageBlock>();
    
    @Inject
    public CapabilitiesVMFactoryImplStub(Provider<DHTManager> dhtManager,
            Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler) {
        super(dhtManager, simppManager, updateHandler);
    }

    public void addMessageBlock(CapabilitiesVM.SupportedMessageBlock messageBlock) {
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
