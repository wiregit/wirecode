package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.CommonUtils;
import java.io.*;
import com.sun.java.util.collections.Set;


/**
 * An implementaiton of the UploadState that sends an error message 
 * for an unavailable range that has been requested. This is an
 * HTTP 416 error.
 */
public class UnavailableRangeUploadState implements HTTPMessage {
    
	/**
	 * Constant for the <tt>FileDesc</tt> instance that was requested.
	 */
	private final FileDesc FILE_DESC;

    private final HTTPUploader UPLOADER;
    
    /**
     * Constant for the amount of time to wait before retrying if we are
     * not actively downloading this file. (1 hour)
     *
     * The value is meant to be used only as a suggestion to when
     * newer ranges may be available if we do not have any ranges
     * that the downloader may want.
     */
    private static final String INACTIVE_RETRY_AFTER = "" + (60 * 60);

	/**
	 * Creates a new <tt>UnavailableRangeUploadState</tt> with the specified
	 * <tt>FileDesc</tt>.
	 *
	 * @param fd the <tt>FileDesc</tt> for the upload
	 */
	public UnavailableRangeUploadState(HTTPUploader uploader) {
        UPLOADER = uploader;
		FILE_DESC = UPLOADER.getFileDesc();
	}

	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		String str;
		str = "HTTP/1.1 416 Requested Range Unavailable\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
		str = "Content-Length: 0\r\n";
		ostream.write(str.getBytes());
		if(FILE_DESC != null) {
			// write the URN in case the caller wants it
			URN sha1 = FILE_DESC.getSHA1Urn();
			if(sha1 != null) {
				HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN,
									  sha1,
									  ostream);
                Set alts = UPLOADER.getNextSetOfAltsToSend();
				if(alts.size() > 0) {
					HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHeaderValueCollection(alts),
                                          ostream);
				}
				
				if (UPLOADER.wantsFAlts()) {
					alts = UPLOADER.getNextSetOfPushAltsToSend();
					if(alts.size() > 0) {
						HTTPUtils.writeHeader(HTTPHeaderName.FALT_LOCATION,
	                                          new HTTPHeaderValueCollection(alts),
	                                          ostream);
					}
					
				}
			}
            if (FILE_DESC instanceof IncompleteFileDesc) {
                IncompleteFileDesc ifd = (IncompleteFileDesc)FILE_DESC;
                HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                      ifd, ostream);
                if(!ifd.isActivelyDownloading()) {
                    HTTPUtils.writeHeader(HTTPHeaderName.RETRY_AFTER,
                                          INACTIVE_RETRY_AFTER,
                                          ostream);    
                }                                  
            }
		}
        
		str = "\r\n";
		ostream.write(str.getBytes());
	}
    
	public void writeMessageBody(OutputStream ostream) throws IOException {
		// no message, do nothing.
	}
	
	public boolean getCloseConnection() {
	    return false;
	}	
}
