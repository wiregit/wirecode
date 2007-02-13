package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

public class OOBProxyControlVendorMessage extends VendorMessage {

    public static final int VERSION = 3;
    
    public static enum Control {
        DISABLE_FOR_ALL_VERSIONS(-1),
        ENABLE_FOR_ALL_VERSIONS(0),
        DISABLE_VERSION_1(1),
        DISABLE_VERSION_2(2),
        DISABLE_VERSION_3(3);
        
        private int version;
        
        private Control(int version) {
            if (version > 255) {
                throw new IllegalArgumentException("version must be smaller than 256");
            }
            this.version = version;
        }
        
        public int getVersion() {
            return version;
        }
    }
    
    
    public OOBProxyControlVendorMessage(byte[] guid, byte ttl, byte hops, 
            int version, byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_OOB_PROXYING_CONTROL, version, payload);
        if (getVersion() != VERSION || payload.length > 1) {
            throw new BadPacketException("Unsupported version or payload length");
        }
    }
    
    public OOBProxyControlVendorMessage(Control control) {
        super(F_LIME_VENDOR_ID, F_OOB_PROXYING_CONTROL, VERSION, derivePayload(control));
    }

    private static byte[] derivePayload(Control control) {
        if (control == Control.DISABLE_FOR_ALL_VERSIONS) {
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
        else {
            return new byte[] { (byte)control.getVersion() };
        }
    }
    
    public static OOBProxyControlVendorMessage createDoNotProxyMessage() {
        return new OOBProxyControlVendorMessage(Control.DISABLE_FOR_ALL_VERSIONS);
    }
    
    public static OOBProxyControlVendorMessage createDoProxyMessage() {
        return new OOBProxyControlVendorMessage(Control.ENABLE_FOR_ALL_VERSIONS);
    }

    public int getMaximumDisabledVersion() {
        byte[] payload = getPayload();
        if (payload.length == 0) {
            return Integer.MAX_VALUE;
        }
        else {
            return payload[0] & 0xFF;            
        }
    }
    
}
