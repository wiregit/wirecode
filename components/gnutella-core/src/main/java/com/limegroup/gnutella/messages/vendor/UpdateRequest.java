package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

public final class UpdateRequest extends VendorMessage {
    
    public static final int VERSION = 1;

    /**
     * Constructs a new SimppRequest from network data.
     */
    UpdateRequest(byte[] guid, byte ttl, byte hops, int version, 
                                  byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_REQ, version, payload);
        
        if(getVersion() > VERSION) //we don't support it
            throw new BadPacketException("UNSUPPORTED VERSION");

        //there is no payload 
    }
    
    public UpdateRequest() {
        super(F_LIME_VENDOR_ID, F_UPDATE_REQ, VERSION, DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    public int getVersion() {
        return super.getVersion();
    }
    
    public String toString() {
        return "{UpdateRequest:"+super.toString()+"}";
    }
}
