package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

public final class SimppRequestVM extends VendorMessage {
    
    public static final int VERSION = 1;

    /**
     * Constructs a new SimppRequest from network data.
     */
    SimppRequestVM(byte[] guid, byte ttl, byte hops, int version, 
                                  byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP_REQ, version, payload);
        
        if(getVersion() > VERSION) //we don't support it
            throw new BadPacketException("UNSUPPORTED VERSION");

        //there is no payload 
    }
    
    public SimppRequestVM() {
        super(F_LIME_VENDOR_ID, F_SIMPP_REQ, VERSION,
                                                DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    public int getVersion() {
        return super.getVersion();
    }
    
    public String toString() {
        return "{SimppRequestVM:"+super.toString()+"}";
    }
    
}
