package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * A request of content.
 */
public class ContentRequest extends VendorMessage {

    public static final int VERSION = 1;

    protected byte[] sha1;
    
    protected byte[] fileName;
    
    private int extension = -1;
    
    protected byte[] metaData;
    
    private long fileSize;
    
    private int length;
    
    /**
     * Constructs a new ContentRequest with data from the network.
     */
    public ContentRequest(byte[] guid, byte ttl, byte hops, int version, byte[] payload) 
            throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_CONTENT_REQ, version, payload);
        
        GGEP ggep = null;
        try {
            ggep = new GGEP(payload, 0);
        } catch (BadGGEPBlockException e) {
            throw new BadPacketException(e);
        }
        
        sha1 = ggep.get(GGEP.GGEP_HEADER_SHA1);

        if (sha1 == null || sha1.length != 20) {
            throw new BadPacketException();
        }
        
        if (ggep.hasKey(GGEP.GGEP_HEADER_FILENAME)) {
            fileName = ggep.get(GGEP.GGEP_HEADER_FILENAME);
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_FILE_EXTENSION_INDEX)) {
                try {
                    extension = ggep.getInt(GGEP.GGEP_HEADER_FILE_EXTENSION_INDEX);
                    
                    /*if (extension < 0 || extension >= fileName.length) {
                        throw new BadPacketException();
                    }*/
                } catch (BadGGEPPropertyException e) {
                }
            }
        }
            
        if (ggep.hasKey(GGEP.GGEP_HEADER_METADATA)) {
            metaData = ggep.get(GGEP.GGEP_HEADER_METADATA);
        }
        
        if (ggep.hasKey(GGEP.GGEP_HEADER_FILE_SIZE)) {
            try {
                fileSize = ggep.getLong(GGEP.GGEP_HEADER_FILE_SIZE);
            } catch (BadGGEPPropertyException e) {
            }
        }
        
        if (ggep.hasKey(GGEP.GGEP_HEADER_RUNLENGTH)) {
            try {
                length = ggep.getInt(GGEP.GGEP_HEADER_RUNLENGTH);
            } catch (BadGGEPPropertyException e) {
            }
        }
    }
    
    /**
     * Constructs a new ContentRequest for the given file details.
     */
    public ContentRequest(FileDetails fileDetails) {
        super(F_LIME_VENDOR_ID, F_CONTENT_REQ, VERSION, derivePayload(fileDetails));
        
        this.sha1 = fileDetails.getSHA1Urn().getBytes();
        this.fileName = getBytes(fileDetails.getFileName());
        this.extension = FileUtils.indexOfExtension(fileDetails.getFileName());
        this.metaData = getBytes(getMetaData(fileDetails));
        this.fileSize = fileDetails.getFileSize();
        this.length = getLength(fileDetails);
    }
    
    /**
     * Returns the URN of the SHA1
     */
    public URN getURN() {
        try {
            return URN.createSHA1UrnFromBytes(sha1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns the file name
     */
    public String getFileName() {
        return toString(fileName);
    }
    
    /**
     * Returns the start index where the file extension starts
     */
    public int getExtensionIndex() {
        return extension;
    }
    
    /**
     * Returns the meta data
     */
    public String getMetaData() {
        return toString(metaData);
    }
    
    /**
     * Returns the file size
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * Returns the run length (in seconds)
     */
    public int getLength() {
        return length;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("URN: ").append(getURN()).append("\n");
        buffer.append("FileName: ").append(getFileName()).append("\n");
        buffer.append("ExtensionIndex: ").append(getExtensionIndex()).append("\n");
        buffer.append("MetaData: ").append(getMetaData()).append("\n");
        buffer.append("FileSize: ").append(getFileSize()).append("\n");
        buffer.append("Length: ").append(getLength()).append("\n");
        return buffer.toString();
    }
    
    private static String getMetaData(FileDetails fileDetails) {
        String fileName = fileDetails.getFileName();
        if (LimeXMLUtils.isSupportedAudioFormat(fileName)) {
            LimeXMLDocument doc = fileDetails.getXMLDocument();
            if (doc != null) {
                return doc.getValue(LimeXMLNames.AUDIO_TITLE);
            }
            
        } else if (LimeXMLUtils.isSupportedVideoFormat(fileName)) {
            LimeXMLDocument doc = fileDetails.getXMLDocument();
            if (doc != null) {
                return doc.getValue(LimeXMLNames.VIDEO_TITLE);
            }
        }
        return null;
    }
    
    private static int getLength(FileDetails fileDetails) {
        String fileName = fileDetails.getFileName();
        String length = null;
        if (LimeXMLUtils.isSupportedAudioFormat(fileName)) {
            LimeXMLDocument doc = fileDetails.getXMLDocument();
            if (doc != null) {
                length = doc.getValue(LimeXMLNames.AUDIO_SECONDS);
            }
        } else if (LimeXMLUtils.isSupportedVideoFormat(fileName)) {
            LimeXMLDocument doc = fileDetails.getXMLDocument();
            if (doc != null) {
                length = doc.getValue(LimeXMLNames.VIDEO_LENGTH);
            }
        }
        
        if (length != null) {
            try {
                return Integer.parseInt(length);
            } catch (NumberFormatException ignore) {}
        }
        
        return -1;
    }
    
    /**
     * Constructs the payload from given file details.
     */
    private static byte[] derivePayload(FileDetails fileDetails) {
        URN urn = fileDetails.getSHA1Urn();
        if (urn == null) {
            throw new NullPointerException("URN must not be null");
        }
        
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException("URN must be a SHA1");
        }
        
        GGEP ggep =  new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_SHA1, urn.getBytes());

        String fileName = fileDetails.getFileName();
        if (fileName != null && fileName.length() > 0) {
            try { 
                ggep.put(GGEP.GGEP_HEADER_FILE_NAME,
                        toLowerCase(fileName).getBytes(Constants.UTF_8_ENCODING));
            } catch (UnsupportedEncodingException uee) {
            }
            
            int index = FileUtils.indexOfExtension(fileName);
            if (index >= 0) {
                // TODO use only an unsigned byte?
                ggep.put(GGEP.GGEP_HEADER_FILE_EXTENSION_INDEX, index);
            }
        }
        
        String metaData = getMetaData(fileDetails);
        if (metaData != null && metaData.length() > 0) {
            try {
                ggep.put(GGEP.GGEP_HEADER_METADATA, 
                        toLowerCase(metaData).getBytes(Constants.UTF_8_ENCODING));
            } catch (UnsupportedEncodingException e) {
            }
        }
        
        long fileSize = fileDetails.getFileSize();
        if (fileSize > 0L) {
            ggep.put(GGEP.GGEP_HEADER_FILE_SIZE, fileSize);
        }
        
        int length = getLength(fileDetails);
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
    
    private static String toLowerCase(String str) {
        if (str != null) {
            str = str.toLowerCase(Locale.US);
        }
        return str;
    }
    
    private static String toString(byte[] str) {
        if (str == null) {
            return null;
        }
        
        try {
            return new String(str, Constants.UTF_8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static byte[] getBytes(String str) {
        if (str == null) {
            return null;
        }
        
        try {
            return str.getBytes(Constants.UTF_8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
