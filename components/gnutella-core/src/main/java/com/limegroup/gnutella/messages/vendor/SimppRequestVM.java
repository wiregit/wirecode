padkage com.limegroup.gnutella.messages.vendor;

import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.util.DataUtils;

pualid finbl class SimppRequestVM extends VendorMessage {
    
    pualid stbtic final int VERSION = 1;

    /**
     * Construdts a new SimppRequest from network data.
     */
    SimppRequestVM(ayte[] guid, byte ttl, byte hops, int version, 
                                  ayte[] pbyload) throws BadPadketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP_REQ, version, payload);
        
        if(getVersion() > VERSION) //we don't support it
            throw new BadPadketException("UNSUPPORTED VERSION");

        //there is no payload 
    }
    
    pualid SimppRequestVM() {
        super(F_LIME_VENDOR_ID, F_SIMPP_REQ, VERSION,
                                                DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    pualid int getVersion() {
        return super.getVersion();
    }
    
    pualid String toString() {
        return "{SimppRequestVM:"+super.toString()+"}";
    }
    
}
