/**
 * 
 */
package com.limegroup.gnutella.messages.vendor;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * A request for content.
 */
public class ContentRequest extends VendorMessage {

    public static final int VERSION = 1;

    /**
     * Constructs a new ContentRequest with data from the network.
     */
    public ContentRequest(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
      throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_CONTENT_REQ, version, payload);
        if (getPayload().length < 1)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
    }
    
    /**
     * Constructs a new ContentRequest for the given SHA1 URN.
     */
    public ContentRequest(URN sha1) {
        super(F_LIME_VENDOR_ID, F_CONTENT_REQ, VERSION, derivePayload(sha1));
    }

    /**
     * Constructs the payload from given SHA1 Urn.
     */
    private static byte[] derivePayload(URN sha1) {
        if(sha1 == null)
            throw new NullPointerException("null sha1");
        
        GGEP ggep =  new GGEP(true);
        // TODO use bytes instead of String, or pack into GUID.
        ggep.put(GGEP.GGEP_HEADER_SHA1, sha1.httpStringValue());        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ggep.write(out);
        } catch(IOException iox) {
            ErrorService.error(iox); // impossible.
        }
        return out.toByteArray();
    }
}
