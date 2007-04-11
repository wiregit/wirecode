package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * Message requesting inspection for specified values.
 * 
 * Note this is very LimeWire-specific, so other vendors will
 * almost certainly have no use for supporting this message.
 */
public class InspectionRequest extends VendorMessage {
    
    private static final int VERSION = 1;
    static final String INSPECTION_KEY = "I";

    private final String[] requested;
    
    public InspectionRequest(byte[] guid, byte ttl, byte hops, 
            int version, byte[] payload, int network)
            throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_INSPECTION_REQ, version, payload, network);
        String requested;
        try {
            GGEP ggep = new GGEP(payload,0,null);
            requested = ggep.getString(INSPECTION_KEY);
        } catch (BadGGEPBlockException bad) {
            throw new BadPacketException();
        } catch (BadGGEPPropertyException bad) {
            throw new BadPacketException();
        }
        
        this.requested = requested.split(";"); 
    }
    
    /**
     * @param requested requested fields for inspection.  
     * See <tt>InspectionUtils</tt> for description of the format.
     */
    public InspectionRequest(String... requested) {
        super(F_LIME_VENDOR_ID, F_INSPECTION_REQ, VERSION,
                derivePayload(requested));
        this.requested = requested;
    }

    public String[] getRequestedFields() {
        return requested;
    }
    
    private static byte [] derivePayload(String... requested) {
        /*
         * The selected fields are catenated and put in a compressed
         * ggep entry.
         */
        StringBuilder b = new StringBuilder();
        for (String r : requested)
            b.append(r).append(";");
        String ret;
        if (b.charAt(b.length() - 1) == ';')
            ret = b.substring(0, b.length() - 1);
        else
            ret = b.toString();
        
        GGEP g = new GGEP();
        g.putCompressed(INSPECTION_KEY, ret.getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            g.write(baos);
        } catch (IOException impossible) {
            ErrorService.error(impossible);
        }
        return baos.toByteArray();
    }
}
