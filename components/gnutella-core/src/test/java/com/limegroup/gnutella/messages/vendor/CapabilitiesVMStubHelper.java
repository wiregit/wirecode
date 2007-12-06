package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;

import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.Message.Network;

public class CapabilitiesVMStubHelper {
    
    public static CapabilitiesVM.SupportedMessageBlock makeSMB(byte[] capability, int version) {
        return new CapabilitiesVM.SupportedMessageBlock(capability, version);
    }

    public static CapabilitiesVM makeCapVM(int simppNumber) throws Exception {
        //1. prepare the SMB
        CapabilitiesVM.SupportedMessageBlock simppSMB =
            makeSMB(new byte[] { 'I', 'M', 'P', 'P' }, simppNumber);
        //2. make the payload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)1, baos);
        simppSMB.encode(baos);
        byte[] payload = baos.toByteArray();
        byte[] guid = GUID.makeGuid();
        return new CapabilitiesVM(guid, (byte)1, (byte)0, 1, payload, Network.UNKNOWN);
    }
    
    public static CapabilitiesVM makeUpdateVM(int id) throws Exception {
        //1. prepare the SMB
        CapabilitiesVM.SupportedMessageBlock smb =
            makeSMB(new byte[] { 'L', 'M', 'U', 'P' }, id);
            
        //2. make the payload
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteOrder.short2leb((short)1, baos);
        smb.encode(baos);
        byte[] payload = baos.toByteArray();
        byte[] guid = GUID.makeGuid();
        return new CapabilitiesVM(guid, (byte)1, (byte)0, 1, payload, Network.UNKNOWN);        
    }
}
