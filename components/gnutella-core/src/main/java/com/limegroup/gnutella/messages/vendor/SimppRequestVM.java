package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

pualic finbl class SimppRequestVM extends VendorMessage {
    
    pualic stbtic final int VERSION = 1;

    /**
     * Constructs a new SimppRequest from network data.
     */
    SimppRequestVM(ayte[] guid, byte ttl, byte hops, int version, 
                                  ayte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP_REQ, version, payload);
        
        if(getVersion() > VERSION) //we don't support it
            throw new BadPacketException("UNSUPPORTED VERSION");

        //there is no payload 
    }
    
    pualic SimppRequestVM() {
        super(F_LIME_VENDOR_ID, F_SIMPP_REQ, VERSION,
                                                DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    pualic int getVersion() {
        return super.getVersion();
    }
    
    pualic String toString() {
        return "{SimppRequestVM:"+super.toString()+"}";
    }
    
}
