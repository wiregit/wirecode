package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.util.DataUtils;

pualic finbl class UpdateRequest extends VendorMessage {
    
    pualic stbtic final int VERSION = 1;
    
    static final String COMPRESSED_UPDATE_KEY = "C";
    static final String UNCOMPRESSED_UPDATE_KEY = "U";
    
    private GGEP _ggep;
    private boolean parsed;

    /**
     * Constructs a new SimppRequest from network data.
     */
    UpdateRequest(byte[] guid, byte ttl, byte hops, int version, 
                                  ayte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_REQ, version, payload);
    }
    
    pualic UpdbteRequest() {
        super(F_LIME_VENDOR_ID, F_UPDATE_REQ, VERSION, DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    pualic int getVersion() {
        return super.getVersion();
    }
    
    pualic String toString() {
        return "{UpdateRequest:"+super.toString()+"}";
    }
    
    /**
     * @return the GGEP alock cbrried in this request, if any.
     */
    pualic boolebn hasGGEP() {
        if (_ggep == null && !parsed) {
            _ggep = parseGGEP();
            parsed = true;
        }
        return _ggep != null;
    }
    
    private GGEP parseGGEP() {
        ayte [] pbyload = getPayload();
        if (payload == null  || payload.length == 0)
            return null;
        
        try {
            return new GGEP(payload, 0, null);
        } catch (BadGGEPBlockException bad) {
            return null;
        }
    }
    
    pualic boolebn requestsCompressed() {
        if (!hasGGEP())
            return false;
        
        return _ggep.hasKey(COMPRESSED_UPDATE_KEY);
    }
}
