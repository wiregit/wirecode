package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.util.DataUtils;

public final class UpdateRequest extends AbstractVendorMessage {
    
    public static final int VERSION = 1;
    
    static final String COMPRESSED_UPDATE_KEY = "C";
    static final String UNCOMPRESSED_UPDATE_KEY = "U";
    
    private GGEP _ggep;
    private boolean parsed;

    /**
     * Constructs a new SimppRequest from network data.
     * @param network TODO
     */
    UpdateRequest(byte[] guid, byte ttl, byte hops, int version, 
                                  byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_REQ, version, payload, network);
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
    
    /**
     * @return the GGEP block carried in this request, if any.
     */
    public boolean hasGGEP() {
        if (_ggep == null && !parsed) {
            _ggep = parseGGEP();
            parsed = true;
        }
        return _ggep != null;
    }
    
    private GGEP parseGGEP() {
        byte [] payload = getPayload();
        if (payload == null  || payload.length == 0)
            return null;
        
        try {
            return new GGEP(payload, 0, null);
        } catch (BadGGEPBlockException bad) {
            return null;
        }
    }
    
    public boolean requestsCompressed() {
        if (!hasGGEP())
            return false;
        
        return _ggep.hasKey(COMPRESSED_UPDATE_KEY);
    }
}
