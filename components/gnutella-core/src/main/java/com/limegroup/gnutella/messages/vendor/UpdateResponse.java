package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;

public final class UpdateResponse extends VendorMessage {
    
    public static final int VERSION = 1;

    /**
     * Constructs a new UpdateResponse message from the network.
     */
    UpdateResponse(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
                                                     throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_RESP, version, payload);
        
        if(getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");                
    }
    
    /**
     * Constructs an outgoing message with the payload being the signed parameter body.
     */
    public UpdateResponse(byte[] body) {
        super(F_LIME_VENDOR_ID, F_UPDATE_RESP, VERSION, body);
    }

    public byte[] getPayload() {
        return super.getPayload();
    }
    
}
