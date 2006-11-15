/**
 * 
 */
package com.limegroup.gnutella.messages.vendor;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

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
        
        if (payload.length < 1) {
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: " + getPayload().length);
        }
        
        try {
            GGEP ggep = new GGEP(payload, 0);
            
            sha1 = ggep.get(GGEP.GGEP_HEADER_SHA1);
            
            if (ggep.hasKey(GGEP.GGEP_HEADER_FILENAME)) {
                filename = ggep.get(GGEP.GGEP_HEADER_FILENAME);
            }
            
            // TODO implement
            /*if (ggep.hasKey(GGEP.GGEP_HEADER_FILE_EXTENSION_INDEX)) {
                try {
                    index = ggep.getInt(GGEP.GGEP_HEADER_FILE_EXTENSION_INDEX);
                } catch (BadGGEPPropertyException e) {
                }
            }*/
            
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
    
    /**
     * Constructs a new ContentRequest for the given file details.
     */

    public ContentRequest(FileDetails details) {
    	super(F_LIME_VENDOR_ID, F_CONTENT_REQ, VERSION, derivePayload(details));
        
        this.sha1 = details.getSHA1Urn().getBytes();
        
        String filename = details.getFileName();
        if (filename != null && filename.length() > 0) {
            try {
                this.filename = filename.getBytes(Constants.UTF_8_ENCODING);
            } catch (UnsupportedEncodingException e) {
            }
        }

        // TODO fberger
//        if (metaData != null && metaData.length() > 0) {
//            try {
//                this.metaData = metaData.getBytes(Constants.UTF_8_ENCODING);
//            } catch (UnsupportedEncodingException e) {
//            }
//        }
        
        this.size = details.getFileSize();
        // TODO fberger add length
    }

	/**
     * Constructs the payload from given SHA1 Urn.
     */

    private static byte[] derivePayload(FileDetails details) {
    	URN urn = details.getSHA1Urn();
    	if (urn == null) {
            throw new NullPointerException("URN must not be null");
        }
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException("URN must be a SHA1");
        }
        
        GGEP ggep =  new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_SHA1, urn.getBytes());

        String fileName = details.getFileName();
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
        
        long fileSize = details.getFileSize();
        if (fileSize > 0L) {
        	ggep.put(GGEP.GGEP_HEADER_FILE_SIZE, fileSize);
        }
        
        String metaData = null;
        String length = null;
        
        if (LimeXMLUtils.isSupportedAudioFormat(fileName)) {
            LimeXMLDocument doc = details.getXMLDocument();
            length = doc.getValue(LimeXMLNames.AUDIO_SECONDS);
            metaData = doc.getValue(LimeXMLNames.AUDIO_TITLE);
            
        } else if (LimeXMLUtils.isSupportedVideoFormat(fileName)) {
            LimeXMLDocument doc = details.getXMLDocument();
            length = doc.getValue(LimeXMLNames.VIDEO_LENGTH);
            metaData = doc.getValue(LimeXMLNames.VIDEO_TITLE);
        }

        if (length != null) {
            try {
                int l = Integer.parseInt(length);
                if (l > 0) {
                  ggep.put(GGEP.GGEP_HEADER_RUNLENGTH, length);
                }
            } catch (NumberFormatException e) {}
        }
        
        if (metaData != null && metaData.length() > 0) {
        	try {
        		ggep.put(GGEP.GGEP_HEADER_METADATA, 
        				toLowerCase(metaData).getBytes(Constants.UTF_8_ENCODING));
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
    
    /** Gets the URN -- this will inefficiently parse the GGEP each time it's called. */


    private static String toLowerCase(String str) {
        if (str != null) {
            str = str.toLowerCase(Locale.US);
        }
        return str;
    }
    
    /**
     * Returns the URN of the SHA1
     */
    public URN getURN() {
    	try { 
    		return URN.createSHA1UrnFromBytes(sha1);
    	} catch (IOException err) {
    	}
    	return null;
    }
        
    public FileDetails getFileDetails() {
    	try {
    		GGEP ggep = new GGEP(getPayload(), 0);
    		return new ContentRequestFileDetails(ggep);
    	} catch (BadGGEPBlockException e) {
    	}
        return null;
    }
    
    /**
     * Returns the SHA1
     */
    public byte[] getSHA1() {
        return sha1;
    }
    
    /**
     * Returns the file name
     */
    public byte[] getFilename() {
        return filename;
    }
    
    /**
     * Returns the meta data
     */
    public byte[] getMetaData() {
        return metaData;
    }
    
    /**
     * Returns the file size
     */
    public long getSize() {
        return size;
    }
    
    /**
     * The run length
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
    
    /**
     * Class that provides the values lazily. Not thread safe.
     */
    private static class ContentRequestFileDetails implements FileDetails {

    	private final GGEP ggep;
    	private URN urn;
    	private String fileName;
    	private long fileSize = -1L;
    	
		public ContentRequestFileDetails(GGEP ggep) {
			this.ggep = ggep;
		}

		public File getFile() {
			return null;
		}

		public String getFileName() {
			if (fileName == null && ggep.hasKey(GGEP.GGEP_HEADER_FILE_NAME)) {
                try {
                    byte[] b = ggep.get(GGEP.GGEP_HEADER_FILE_NAME);
                    fileName = new String(b, Constants.UTF_8_ENCODING);
                } catch (UnsupportedEncodingException e) {
                }
			}
			return fileName;
		}

		public long getFileSize() {
			if (fileSize == -1L && ggep.hasKey(GGEP.GGEP_HEADER_FILE_SIZE)) {
				try {
					fileSize = ggep.getLong(GGEP.GGEP_HEADER_FILE_SIZE);
				} catch (BadGGEPPropertyException e) {
				}
			}
			return fileSize;
		}

		public URN getSHA1Urn() {
			if (urn == null && ggep.hasKey(GGEP.GGEP_HEADER_SHA1)) {
				try { 
					urn = URN.createSHA1UrnFromBytes(ggep.getBytes(GGEP.GGEP_HEADER_SHA1));
				} catch (BadGGEPPropertyException e) {
				} catch (IOException e) {
				}
			}
			return urn;
		}

		public InetSocketAddress getSocketAddress() {
			return null;
		}

		public Set<URN> getUrns() {
            URN urn = this.urn;
            if (urn != null) {
                return Collections.singleton(urn);
            }
            return Collections.emptySet();
		}

		public LimeXMLDocument getXMLDocument() {
			return null;
		}

		public boolean isFirewalled() {
			return false;
		}
    }
}
