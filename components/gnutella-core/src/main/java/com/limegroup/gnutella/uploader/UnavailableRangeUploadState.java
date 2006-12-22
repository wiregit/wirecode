package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * An implementaiton of the UploadState that sends an error message 
 * for an unavailable range that has been requested. This is an
 * HTTP 416 error.
 */
public class UnavailableRangeUploadState extends UploadState {
    
    
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
        super(uploader);
	}

	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		String str;
		str = "HTTP/1.1 416 Requested Range Unavailable\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + LimeWireUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
		str = "Content-Length: 0\r\n";
		ostream.write(str.getBytes());
		
		writeAlts(ostream);
		writeRanges(ostream);
		writeProxies(ostream);

		
        if (FILE_DESC!=null && FILE_DESC instanceof IncompleteFileDesc) {
        	IncompleteFileDesc ifd = (IncompleteFileDesc)FILE_DESC;
            if(!ifd.isActivelyDownloading()) {
                HTTPUtils.writeHeader(HTTPHeaderName.RETRY_AFTER,
                                      INACTIVE_RETRY_AFTER,
                                      ostream);    
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
