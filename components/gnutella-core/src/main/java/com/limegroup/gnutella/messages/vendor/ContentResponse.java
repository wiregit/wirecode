/**
 * 
 */
package com.limegroup.gnutella.messages.vendor;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * A response of content.
 */
public class ContentResponse extends VendorMessage {

    public static final int VERSION = 1;
    
    private URN sha1;
    
    private boolean okay;
    
    private String message;
    
    /**
     * Constructs a new ContentRequest with data from the network.
     */
    public ContentResponse(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
      throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_CONTENT_RESP, version, payload);
        
        if (getPayload().length < 1) {
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
        }
        
        try {
            GGEP ggep = new GGEP(getPayload(), 0);
            
            try {
                sha1 = URN.createSHA1UrnFromBytes(ggep.getBytes(GGEP.GGEP_HEADER_SHA1));
            } catch (IOException e) {
            } catch (BadGGEPPropertyException e) {
            }
            
            try {
                okay = ggep.getInt(GGEP.GGEP_HEADER_SHA1_VALID) != 0;
            } catch (BadGGEPPropertyException e) {
            }
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_CONTENT_MESSAGE)) {
                try {
                    message = ggep.getString(GGEP.GGEP_HEADER_CONTENT_MESSAGE);
                } catch (BadGGEPPropertyException e) {
                }
            }
        } catch(BadGGEPBlockException bgbe) {
            throw new BadPacketException(bgbe);
        }
    }
    
    /**
     * Constructs a new ContentRequest for the given SHA1 URN.
     */
    public ContentResponse(URN sha1, boolean okay, String message) {
        super(F_LIME_VENDOR_ID, F_CONTENT_RESP, VERSION, derivePayload(sha1, okay, message));
        
        this.sha1 = sha1;
        this.okay = okay;
        this.message = message;
    }

    /**
     * Constructs the payload from given SHA1 Urn & okay flag.
     */
    private static byte[] derivePayload(URN sha1, boolean okay, String message) {
        if (sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        
        GGEP ggep =  new GGEP(true);
        
        ggep.put(GGEP.GGEP_HEADER_SHA1, sha1.getBytes());
        ggep.put(GGEP.GGEP_HEADER_SHA1_VALID, okay ? 1 : 0);
        
        if (message != null && message.length() > 0) {
            try {
                ggep.put(GGEP.GGEP_HEADER_CONTENT_MESSAGE, 
                        message.getBytes(Constants.UTF_8_ENCODING));
            } catch (UnsupportedEncodingException e) {
            }
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ggep.write(out);
        } catch(IOException iox) {
            ErrorService.error(iox); // impossible.
        }
        return out.toByteArray();
    }
    
    /**
     * Gets the URN this msg is for.
     */
    public URN getURN() {
        return sha1;
    }
    
    /**
     * Gets the 'ok' flag for the URN.
     */
    public boolean getOK() {
        return okay;
    }
    
    /**
     * 
     */
    public String getMessage() {
        return message;
    }
}
