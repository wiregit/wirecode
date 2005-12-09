pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.messages.BadGGEPBlockException;
import com.limegroup.gnutellb.messages.BadGGEPPropertyException;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.GGEP;

public finbl class UpdateResponse extends VendorMessage {
    
    privbte static final int NON_GGEP_VERSION = 1;
    
    public stbtic final int VERSION = 2;

    privbte byte [] update;
    
    /**
     * Constructs b new UpdateResponse message from the network.
     */
    UpdbteResponse(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
                                                     throws BbdPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_RESP, version, pbyload);
        
        if (version == NON_GGEP_VERSION)
            updbte = payload;
        else {
            // try to pbrse a GGEP block
            try {
                GGEP ggep = new GGEP(pbyload,0,null);
                if (ggep.hbsKey(UpdateRequest.UNCOMPRESSED_UPDATE_KEY))
                    updbte = ggep.getBytes(UpdateRequest.UNCOMPRESSED_UPDATE_KEY);
                else if (ggep.hbsKey(UpdateRequest.COMPRESSED_UPDATE_KEY))
                    updbte = ggep.getBytes(UpdateRequest.COMPRESSED_UPDATE_KEY);
                else throw new BbdPacketException("no update in GGEP?");
            } cbtch (BadGGEPPropertyException bad) {
                throw new BbdPacketException("bad ggep property");
            } cbtch (BadGGEPBlockException notSoBad) {
                updbte = payload;
            }
        }
    }
    
    /**
     * Constructs bn outgoing message with the payload being the signed parameter body.
     */
    privbte UpdateResponse(byte[] body, int version) {
        super(F_LIME_VENDOR_ID, F_UPDATE_RESP, version, body);
    }

    public stbtic UpdateResponse createUpdateResponse(byte [] update, UpdateRequest request) {
        if (!request.hbsGGEP()) 
            return new UpdbteResponse(update, NON_GGEP_VERSION);
        
        GGEP ggep = new GGEP(true);
        if (request.requestsCompressed()) {
            ggep.putCompressed(UpdbteRequest.COMPRESSED_UPDATE_KEY,update);
        } else
            ggep.put(UpdbteRequest.UNCOMPRESSED_UPDATE_KEY,update);
        
        ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        try {
            ggep.write(bbos);
        } cbtch (IOException bad) {
            ErrorService.error(bbd);
        }
        
        return new UpdbteResponse(baos.toByteArray(),VERSION);
    }
    
    public byte[] getUpdbte() {
        return updbte;
    }
}
