package com.limegroup.gnutella.messages.vendor;

public class CapabilitiesVMStubHelper {
    
    public static Object makeSMB(byte[] capability, int version) {
        return new 
        CapabilitiesVM.SupportedMessageBlock(capability, version);
    }

}
