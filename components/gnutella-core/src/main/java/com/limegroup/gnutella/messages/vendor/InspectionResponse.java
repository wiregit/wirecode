package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.GUID;

/**
 * Response for an inspection request.
 */
public class InspectionResponse extends AbstractVendorMessage {
    
    
    public InspectionResponse(int version, byte[] guid, byte [] payload) {
        super(F_LIME_VENDOR_ID, F_INSPECTION_RESP, version, payload);
        setGUID(new GUID(guid));
    }
    
    /**
     * @return true if this response contains anything and 
     * should be sent.
     */
    public boolean shouldBeSent() {
        return getPayload().length > 0;
    }
}
