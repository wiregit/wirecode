package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.io.*;

public class CapabilitiesVMStubHelper {
    
    public static Object makeSMB(byte[] capability, int version) {
        return new 
        CapabilitiesVM.SupportedMessageBlock(capability, version);
    }

    public static CapabilitiesVM makeCapVM(int simppNumber) throws Exception {
        //1. prepare the SMB
        CapabilitiesVM.SupportedMessageBlock simppSMB = 
        (CapabilitiesVM.SupportedMessageBlock) makeSMB(
                             CapabilitiesVM.SIMPP_CAPABILITY_BYTES,simppNumber);
        //2. make the payload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)1, baos);
        simppSMB.encode(baos);
        byte[] payload = baos.toByteArray();
        byte[] guid = GUID.makeGuid();
        return new CapabilitiesVM(guid, (byte)1, (byte)0, 1, payload);
    }
    
}
