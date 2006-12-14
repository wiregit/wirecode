/**
 * 
 */
package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Signature;
import java.security.SignatureException;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.auth.ContentResponseData.Authorization;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.GGEPParser;
import com.limegroup.gnutella.messages.SecureMessage;

/**
 * A response of content.
 */
public class ContentResponse extends VendorMessage implements SecureMessage {

    private static final long ONE_WEEK = 1000L*60L*60L*24L*7L;
    
    public static final int VERSION = 1;
    
    /* default */ static ContentResponseSigner SIGNER;
    
    private URN urn;
    
    private Authorization auth;
    
    private String message;
    
    private long timeStamp;
    
    private byte[] signature;
    
    private int secureStatus = SecureMessage.INSECURE;
    
    /**
     * Constructs a new ContentRequest with data from the network.
     */
    public ContentResponse(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
      throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_CONTENT_RESP, version, payload);
        
        if (payload.length < 1) {
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + payload.length);
        }
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(payload, 0);
        
        GGEP ggep = parser.getNormalGGEP();
        if (ggep == null) {
            throw new BadPacketException("GGEP field missing");
        }
            
        try {
            urn = URN.createSHA1UrnFromBytes(ggep.getBytes(GGEP.GGEP_HEADER_SHA1));
        } catch (IOException e) {
        } catch (BadGGEPPropertyException e) {
        }
        
        try {
            auth = Authorization.valueOf(ggep.getInt(GGEP.GGEP_HEADER_SHA1_VALID));
        } catch (BadGGEPPropertyException e) {
        }
        
        if (ggep.hasKey(GGEP.GGEP_HEADER_CONTENT_MESSAGE)) {
            try {
                message = ggep.getString(GGEP.GGEP_HEADER_CONTENT_MESSAGE);
            } catch (BadGGEPPropertyException e) {
            }
        }
        
        if (ggep.hasKey(GGEP.GGEP_HEADER_TIMESTAMP)) {
            try {
                timeStamp = ggep.getLong(GGEP.GGEP_HEADER_TIMESTAMP);
            } catch (BadGGEPPropertyException e) {
            }
        }
        
        GGEP secure = parser.getSecureGGEP();
        if (secure != null) {
            if (secure.hasKey(GGEP.GGEP_HEADER_SIGNATURE)) {
                try {
                    signature = secure.getBytes(GGEP.GGEP_HEADER_SIGNATURE);
                } catch (BadGGEPPropertyException e) {
                }
            }
        }
    }
    
    /**
     * Constructs a new ContentRequest for the given SHA1 URN.
     */
    public ContentResponse(URN urn, Authorization auth, String message) {
        this(urn, auth, message, System.currentTimeMillis());
    }
    
    /**
     * 
     */
    private ContentResponse(URN urn, Authorization auth, String message, long timeStamp) {
        super(F_LIME_VENDOR_ID, F_CONTENT_RESP, VERSION, 
                derivePayload(urn, auth, message, timeStamp));
        
        this.urn = urn;
        this.auth = auth;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    /**
     * Constructs the payload from given SHA1 Urn & okay flag.
     */
    private static byte[] derivePayload(URN urn, Authorization auth, String message, long timeStamp) {
        if (urn == null) {
            throw new NullPointerException("URN is null");
        }
        
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException("URN must be a SHA1");
        }
        
        GGEP ggep =  new GGEP(true);
        
        ggep.put(GGEP.GGEP_HEADER_SHA1, urn.getBytes());
        ggep.put(GGEP.GGEP_HEADER_SHA1_VALID, auth.getValue());
        
        if (message != null && message.length() > 0) {
            try {
                ggep.put(GGEP.GGEP_HEADER_CONTENT_MESSAGE, 
                        message.getBytes(Constants.UTF_8_ENCODING));
            } catch (UnsupportedEncodingException e) {
            }
        }
        
        if (timeStamp > 0L) {
            ggep.put(GGEP.GGEP_HEADER_TIMESTAMP, timeStamp);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ggep.write(out);
        } catch(IOException iox) {
            ErrorService.error(iox); // impossible.
        }
        
        ContentResponseSigner signer = ContentResponse.SIGNER;
        if (signer != null) {
            signer.sign(out, out.toByteArray());
        }
        
        return out.toByteArray();
    }
    
    /**
     * Gets the URN this msg is for.
     */
    public URN getURN() {
        return urn;
    }
    
    /**
     * Returns the reason why content was blocked or
     * null if no reason is provided
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the time stamp when the ContentResponse object
     * was created
     */
    public long getTimeStamp() {
        return timeStamp;
    }
    
    /**
     * Returns true if this ContentResponse object is expired
     */
    public boolean isExpired() {
        return (System.currentTimeMillis() - timeStamp) >= ONE_WEEK;
    }

    public int getSecureStatus() {
        return secureStatus;
    }

    public synchronized void setSecureStatus(int secureStatus) {
        this.secureStatus = secureStatus;
    }

    public synchronized boolean isSecure() {
        return (secureStatus & SecureMessage.SECURE) != 0;
    }
    
    public byte[] getSecureSignature() {
        return signature;
    }
    
    public boolean hasSecureSignature() {
        return signature != null;
    }
    
    public synchronized void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException {
        byte[] payload = getPayload();
        
        GGEPParser parser = new GGEPParser();
        parser.scanForGGEPs(payload, 0);
        
        int start = parser.getSecureStartIndex();
        signature.update(payload, 0, start);
        
        int end = parser.getSecureEndIndex();
        signature.update(payload, end, payload.length-end);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("URN: ").append(getURN()).append("\n");
        buffer.append("Authorization: ").append(auth).append("\n");
        buffer.append("Message: ").append(getMessage()).append("\n");
        buffer.append("TimeStamp: ").append(getTimeStamp()).append("\n");
        buffer.append("Signature: ").append((getSecureSignature() != null ? getSecureSignature().length : "null")).append("\n");
        buffer.append("Length: ").append(getPayload().length).append("\n");
        return buffer.toString();
    }
    
    /**
     * 
     */
    static interface ContentResponseSigner {
        public void sign(OutputStream out, byte[] data);
    }

	public Authorization getAuthorization() {
		return auth;
	}
}
