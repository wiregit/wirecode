package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.altlocs.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.CommonUtils;

import java.io.*;
import com.sun.java.util.collections.Set;


/**
 * An implementaiton of the UploadState that sends an error message 
 * for the limit of uploads allowed has been reached. This is an
 * HTTP 503 error.
 */
public class LimitReachedUploadState implements HTTPMessage {

	/**
	 * Constant for the <tt>FileDesc</tt> instance that was requested.
	 */
	private final FileDesc FILE_DESC;

    private final HTTPUploader UPLOADER;

    /**
     * Number of seconds the remote host should wait before retrying in
     * case we don't have any alt-locs left to send
     */
    private static final int NO_ALT_LOCS_RETRY_AFTER = 60 * 5; // 5 minutes

    /**
     * Number of seconds the remote host should wait before retrying in
     * case we still have alt-locs left to send
     */
    private static final int NORMAL_RETRY_AFTER = 60 * 1; // 1 minute

	/**
	 * The error message to send in the message body.
	 */
	private static final byte[] ERROR_MESSAGE = 
		"Server busy.  Too many active uploads.".getBytes();

	/**
	 * Creates a new <tt>LimitReachedUploadState</tt> with the specified
	 * <tt>FileDesc</tt>.
	 *
	 * @param fd the <tt>FileDesc</tt> for the upload
	 */
	public LimitReachedUploadState(HTTPUploader uploader) {
        UPLOADER = uploader;
		FILE_DESC = uploader.getFileDesc();
	}

	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		String str;
		str = "HTTP/1.1 503 Service Unavailable\r\n";
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
                Set alts = UPLOADER.getNextSetOfAltsToSend();
				if(alts.size() > 0) {
					HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHeaderValueCollection(alts),
                                          ostream);
				}
                // write retry after if the sha1 is not null
                str = HTTPHeaderName.RETRY_AFTER.toString() + ": ";
                if (alts.size() == 0) {
                    str = str + String.valueOf(NO_ALT_LOCS_RETRY_AFTER);
                } else {
                    str = str + String.valueOf(NORMAL_RETRY_AFTER);
                }
                ostream.write(str.getBytes());
                if (FILE_DESC instanceof IncompleteFileDesc) {
                    HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                          ((IncompleteFileDesc)FILE_DESC),
                                          ostream);
                }
			} else {
                // write retry after if sha1 is null
                str = HTTPHeaderName.RETRY_AFTER.toString() + ": "
                    + String.valueOf(NO_ALT_LOCS_RETRY_AFTER);
                ostream.write(str.getBytes());
            }
		}
		
		HTTPUtils.writeHeader(HTTPHeaderName.CONNECTION,
		                      ConstantHTTPHeaderValue.CLOSE_VALUE,
		                      ostream);
		                      
		str = "\r\n";
		ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
		ostream.write(ERROR_MESSAGE);
	}
	
	public boolean getCloseConnection() {
	    return true;
	}
}
