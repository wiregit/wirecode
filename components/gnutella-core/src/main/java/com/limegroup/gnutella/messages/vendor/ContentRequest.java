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
 * A request for content.
 */
public class ContentRequest extends VendorMessage {

    public static final int VERSION = 1;

    private byte[] sha1;
    
    private byte[] filename;
    
    private byte[] metaData;
    
    private long size;
    
    private int length;
    
    /**
     * Constructs a new ContentRequest with data from the network.
     */
    public ContentRequest(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
      throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_CONTENT_REQ, version, payload);
        
        if (getPayload().length < 1) {
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
        }
        
        try {
            GGEP ggep = new GGEP(getPayload(), 0);
            
            sha1 = ggep.get(GGEP.GGEP_HEADER_SHA1);
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_FILENAME)) {
                filename = ggep.get(GGEP.GGEP_HEADER_FILENAME);
            }
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_METADATA)) {
                metaData = ggep.get(GGEP.GGEP_HEADER_METADATA);
            }
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_FILESIZE)) {
                try {
                    size = ggep.getLong(GGEP.GGEP_HEADER_FILESIZE);
                } catch (BadGGEPPropertyException e) {
                }
            }
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_RUNLENGTH)) {
                try {
                    length = ggep.getInt(GGEP.GGEP_HEADER_RUNLENGTH);
                } catch (BadGGEPPropertyException e) {
                }
            }
        } catch (BadGGEPBlockException e) {
            throw new BadPacketException(e);
        }
    }
    
    public ContentRequest(URN sha1) {
        this(sha1, null, null, 0L, 0);
    }
    
    /**
     * Constructs a new ContentRequest for the given SHA1 URN.
     */
    public ContentRequest(URN sha1, String filename, String metaData, long size, int length) {
        super(F_LIME_VENDOR_ID, F_CONTENT_REQ, VERSION, derivePayload(sha1, filename, metaData, size, length));
        
        this.sha1 = sha1.getBytes();
        
        if (filename != null && filename.length() > 0) {
            try {
                this.filename = filename.getBytes(Constants.UTF_8_ENCODING);
            } catch (UnsupportedEncodingException e) {
            }
        }
        
        if (metaData != null && metaData.length() > 0) {
            try {
                this.metaData = metaData.getBytes(Constants.UTF_8_ENCODING);
            } catch (UnsupportedEncodingException e) {
            }
        }
        
        this.size = size;
        this.length = length;
    }

    /**
     * Constructs the payload from given SHA1 Urn.
     */
    private static byte[] derivePayload(URN sha1, String filename, String metaData, long size, int length) {
        if(sha1 == null) {
            throw new NullPointerException("null sha1");
        }
        
        GGEP ggep =  new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_SHA1, sha1.getBytes());
        
        if (filename != null && filename.length() > 0) {
            try {
                ggep.put(GGEP.GGEP_HEADER_FILENAME, 
                        filename.getBytes(Constants.UTF_8_ENCODING));
            } catch (UnsupportedEncodingException e) {
            }
        }
        
        if (metaData != null && metaData.length() > 0) {
            try {
                ggep.put(GGEP.GGEP_HEADER_METADATA, 
                        metaData.getBytes(Constants.UTF_8_ENCODING));
            } catch (UnsupportedEncodingException e) {
            }
        }
        
        if (size > 0L) {
            ggep.put(GGEP.GGEP_HEADER_FILESIZE, size);
        }
        
        if (length > 0) {
            ggep.put(GGEP.GGEP_HEADER_RUNLENGTH, length);
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
     * 
     */
    public URN getURN() {
        try {
            return URN.createSHA1UrnFromBytes(sha1);
        } catch (IOException err) {
        }
        return null;
    }
    
    /**
     * 
     */
    public byte[] getSHA1() {
        return sha1;
    }
    
    /**
     * 
     */
    public byte[] getFilename() {
        return filename;
    }
    
    /**
     * 
     */
    public byte[] getMetaData() {
        return metaData;
    }
    
    /**
     * 
     */
    public long getSize() {
        return size;
    }
    
    /**
     * 
     */
    public int getLength() {
        return length;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        byte[] filename = getFilename();
        buffer.append("Name: ").append(filename != null ? new String(filename) : "null").append("\n");
        
        buffer.append("URN: ").append(getURN()).append("\n");
        
        byte[] metaData = getMetaData();
        buffer.append("Meta: ").append(metaData != null ? new String(metaData) : "null").append("\n");
        
        buffer.append("Size: ").append(getSize()).append("\n");
        buffer.append("Length: ").append(getLength()).append("\n");
        return buffer.toString();
    }
}
