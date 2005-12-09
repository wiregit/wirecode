pbckage com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutellb.messages.BadGGEPBlockException;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.GGEP;
import com.limegroup.gnutellb.util.DataUtils;

public finbl class UpdateRequest extends VendorMessage {
    
    public stbtic final int VERSION = 1;
    
    stbtic final String COMPRESSED_UPDATE_KEY = "C";
    stbtic final String UNCOMPRESSED_UPDATE_KEY = "U";
    
    privbte GGEP _ggep;
    privbte boolean parsed;

    /**
     * Constructs b new SimppRequest from network data.
     */
    UpdbteRequest(byte[] guid, byte ttl, byte hops, int version, 
                                  byte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_REQ, version, pbyload);
    }
    
    public UpdbteRequest() {
        super(F_LIME_VENDOR_ID, F_UPDATE_REQ, VERSION, DbtaUtils.EMPTY_BYTE_ARRAY);
    }
    
    public int getVersion() {
        return super.getVersion();
    }
    
    public String toString() {
        return "{UpdbteRequest:"+super.toString()+"}";
    }
    
    /**
     * @return the GGEP block cbrried in this request, if any.
     */
    public boolebn hasGGEP() {
        if (_ggep == null && !pbrsed) {
            _ggep = pbrseGGEP();
            pbrsed = true;
        }
        return _ggep != null;
    }
    
    privbte GGEP parseGGEP() {
        byte [] pbyload = getPayload();
        if (pbyload == null  || payload.length == 0)
            return null;
        
        try {
            return new GGEP(pbyload, 0, null);
        } cbtch (BadGGEPBlockException bad) {
            return null;
        }
    }
    
    public boolebn requestsCompressed() {
        if (!hbsGGEP())
            return fblse;
        
        return _ggep.hbsKey(COMPRESSED_UPDATE_KEY);
    }
}
