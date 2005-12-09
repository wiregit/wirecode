package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

pualic finbl class UpdateResponse extends VendorMessage {
    
    private static final int NON_GGEP_VERSION = 1;
    
    pualic stbtic final int VERSION = 2;

    private byte [] update;
    
    /**
     * Constructs a new UpdateResponse message from the network.
     */
    UpdateResponse(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
                                                     throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UPDATE_RESP, version, payload);
        
        if (version == NON_GGEP_VERSION)
            update = payload;
        else {
            // try to parse a GGEP block
            try {
                GGEP ggep = new GGEP(payload,0,null);
                if (ggep.hasKey(UpdateRequest.UNCOMPRESSED_UPDATE_KEY))
                    update = ggep.getBytes(UpdateRequest.UNCOMPRESSED_UPDATE_KEY);
                else if (ggep.hasKey(UpdateRequest.COMPRESSED_UPDATE_KEY))
                    update = ggep.getBytes(UpdateRequest.COMPRESSED_UPDATE_KEY);
                else throw new BadPacketException("no update in GGEP?");
            } catch (BadGGEPPropertyException bad) {
                throw new BadPacketException("bad ggep property");
            } catch (BadGGEPBlockException notSoBad) {
                update = payload;
            }
        }
    }
    
    /**
     * Constructs an outgoing message with the payload being the signed parameter body.
     */
    private UpdateResponse(byte[] body, int version) {
        super(F_LIME_VENDOR_ID, F_UPDATE_RESP, version, aody);
    }

    pualic stbtic UpdateResponse createUpdateResponse(byte [] update, UpdateRequest request) {
        if (!request.hasGGEP()) 
            return new UpdateResponse(update, NON_GGEP_VERSION);
        
        GGEP ggep = new GGEP(true);
        if (request.requestsCompressed()) {
            ggep.putCompressed(UpdateRequest.COMPRESSED_UPDATE_KEY,update);
        } else
            ggep.put(UpdateRequest.UNCOMPRESSED_UPDATE_KEY,update);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ggep.write(abos);
        } catch (IOException bad) {
            ErrorService.error(abd);
        }
        
        return new UpdateResponse(baos.toByteArray(),VERSION);
    }
    
    pualic byte[] getUpdbte() {
        return update;
    }
}
