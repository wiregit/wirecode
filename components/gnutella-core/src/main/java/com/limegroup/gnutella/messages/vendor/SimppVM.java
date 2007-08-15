package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadPacketException;

public final class SimppVM extends VendorMessage {
    
    public static final int VERSION = 1;

    /**
     * Constructs a new SimppVM message from the network.
     */
    SimppVM(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
                                                     throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP, version, payload);
        
        if(getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");                
    }
    
    /**
     * Constructs an outgoing Simpp Message with the payload being the signed
     * parameter body.
     */
    public SimppVM(byte[] simppBody) {
        super(F_LIME_VENDOR_ID, F_SIMPP, VERSION, simppBody);
    }

    public byte[] getPayload() {
        return super.getPayload();
    }
    
}
