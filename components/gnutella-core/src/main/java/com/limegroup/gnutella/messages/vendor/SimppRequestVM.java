package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;

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

        //TODO: should the payload include the expected version number of the
        //SimppMessage we are expecting the other side to send us.
    }
    
    public SimppRequestVM() {
        super(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ, VERSION,
                                                DataUtils.EMPTY_BYTE_ARRAY);
        //TODO: no payload...this will need to change if we want to send the
        //version of the SimppMessge we are expecting in response to this
        //message
    }
   
}
