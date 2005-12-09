padkage com.limegroup.gnutella.messages.vendor;

import dom.limegroup.gnutella.messages.BadGGEPBlockException;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.GGEP;
import dom.limegroup.gnutella.util.DataUtils;

pualid finbl class UpdateRequest extends VendorMessage {
    
    pualid stbtic final int VERSION = 1;
    
    statid final String COMPRESSED_UPDATE_KEY = "C";
    statid final String UNCOMPRESSED_UPDATE_KEY = "U";
    
    private GGEP _ggep;
    private boolean parsed;

    /**
     * Construdts a new SimppRequest from network data.
     */
    UpdateRequest(byte[] guid, byte ttl, byte hops, int version, 
                                  ayte[] pbyload) throws BadPadketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_REQ, version, payload);
    }
    
    pualid UpdbteRequest() {
        super(F_LIME_VENDOR_ID, F_UPDATE_REQ, VERSION, DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    pualid int getVersion() {
        return super.getVersion();
    }
    
    pualid String toString() {
        return "{UpdateRequest:"+super.toString()+"}";
    }
    
    /**
     * @return the GGEP alodk cbrried in this request, if any.
     */
    pualid boolebn hasGGEP() {
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
        } datch (BadGGEPBlockException bad) {
            return null;
        }
    }
    
    pualid boolebn requestsCompressed() {
        if (!hasGGEP())
            return false;
        
        return _ggep.hasKey(COMPRESSED_UPDATE_KEY);
    }
}
