package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import java.io.*;
import com.limegroup.gnutella.util.CommonUtils;


/**
 * An implementaiton of the UploadState that sends an error message 
 * for an unavailable range that has been requested. This is an
 * HTTP 503 error.
 */
public class UnavailableRangeUploadState implements HTTPMessage {
    
	/**
	 * Constant for the <tt>FileDesc</tt> instance that was requested.
	 */
	private final FileDesc FILE_DESC;

	/**
	 * The error message to send in the message body.
	 */
	private static final byte[] ERROR_MESSAGE = 
		"The requested range is not available".getBytes();

	/**
	 * Creates a new <tt>LimitReachedUploadState</tt> with the specified
	 * <tt>FileDesc</tt>.
	 *
	 * @param fd the <tt>FileDesc</tt> for the upload
	 */
	public UnavailableRangeUploadState(FileDesc fd) {
		FILE_DESC = fd;
	}

	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		String str;
		str = "HTTP/1.1 416 Requested Range Unavailable\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
		str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		ostream.write(str.getBytes());
		if(FILE_DESC != null) {
			// write the URN in case the caller wants it
			URN sha1 = FILE_DESC.getSHA1Urn();
			if(sha1 != null) {
				HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN,
									  sha1,
									  ostream);
				if(FILE_DESC.hasAlternateLocations()) {
					HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
										  FILE_DESC.getAlternateLocationCollection(),
										  ostream);
				}
			}
            if (FILE_DESC instanceof IncompleteFileDesc) {
                HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                      ((IncompleteFileDesc)FILE_DESC),
                                      ostream);
            }
		}
        
		str = "\r\n";
		ostream.write(str.getBytes());
	}
    
	public void writeMessageBody(OutputStream ostream) throws IOException {
		ostream.write(ERROR_MESSAGE);
	}
	
	public boolean getCloseConnection() {
	    return false;
	}	
}
