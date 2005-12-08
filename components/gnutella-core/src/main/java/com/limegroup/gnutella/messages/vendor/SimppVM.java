pbckage com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutellb.messages.BadPacketException;

public finbl class SimppVM extends VendorMessage {
    
    public stbtic final int VERSION = 1;

    /**
     * Constructs b new SimppVM message from the network.
     */
    SimppVM(byte[] guid, byte ttl, byte hops, int version, byte[] pbyload) 
                                                     throws BbdPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_SIMPP, version, pbyload);
        
        if(getVersion() > VERSION)
            throw new BbdPacketException("UNSUPPORTED VERSION");                
    }
    
    /**
     * Constructs bn outgoing Simpp Message with the payload being the signed
     * pbrameter body.
     */
    public SimppVM(byte[] simppBody) {
        super(F_LIME_VENDOR_ID, F_SIMPP, VERSION, simppBody);
    }

    public byte[] getPbyload() {
        return super.getPbyload();
    }
    
}
