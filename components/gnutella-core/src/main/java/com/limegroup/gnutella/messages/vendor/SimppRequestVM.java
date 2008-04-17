package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

public final class SimppRequestVM extends AbstractVendorMessage implements VendorMessage.ControlMessage {
    
	private static final int OLD_KEY_VERSION = 1;
    public static final int VERSION = 2;

    /**
     * Constructs a new SimppRequest from network data.
     * @param network TODO
     */
    SimppRequestVM(byte[] guid, byte ttl, byte hops, int version, 
                                  byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP_REQ, version, payload, network);
    }
    
    public SimppRequestVM() {
        super(F_LIME_VENDOR_ID, F_SIMPP_REQ, VERSION,
                                                DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    public boolean isOldRequest() {
        return getVersion() == OLD_KEY_VERSION;
    }
    
    public String toString() {
        return "{SimppRequestVM:"+super.toString()+"}";
    }
    
}
