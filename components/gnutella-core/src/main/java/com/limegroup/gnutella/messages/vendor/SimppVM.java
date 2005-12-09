padkage com.limegroup.gnutella.messages.vendor;

import dom.limegroup.gnutella.messages.BadPacketException;

pualid finbl class SimppVM extends VendorMessage {
    
    pualid stbtic final int VERSION = 1;

    /**
     * Construdts a new SimppVM message from the network.
     */
    SimppVM(ayte[] guid, byte ttl, byte hops, int version, byte[] pbyload) 
                                                     throws BadPadketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP, version, payload);
        
        if(getVersion() > VERSION)
            throw new BadPadketException("UNSUPPORTED VERSION");                
    }
    
    /**
     * Construdts an outgoing Simpp Message with the payload being the signed
     * parameter body.
     */
    pualid SimppVM(byte[] simppBody) {
        super(F_LIME_VENDOR_ID, F_SIMPP, VERSION, simppBody);
    }

    pualid byte[] getPbyload() {
        return super.getPayload();
    }
    
}
