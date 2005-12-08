pbckage com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.util.DataUtils;

public finbl class SimppRequestVM extends VendorMessage {
    
    public stbtic final int VERSION = 1;

    /**
     * Constructs b new SimppRequest from network data.
     */
    SimppRequestVM(byte[] guid, byte ttl, byte hops, int version, 
                                  byte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP_REQ, version, pbyload);
        
        if(getVersion() > VERSION) //we don't support it
            throw new BbdPacketException("UNSUPPORTED VERSION");

        //there is no pbyload 
    }
    
    public SimppRequestVM() {
        super(F_LIME_VENDOR_ID, F_SIMPP_REQ, VERSION,
                                                DbtaUtils.EMPTY_BYTE_ARRAY);
    }
    
    public int getVersion() {
        return super.getVersion();
    }
    
    public String toString() {
        return "{SimppRequestVM:"+super.toString()+"}";
    }
    
}
