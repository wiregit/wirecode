package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.CommonUtils;

import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.java.util.collections.Set;


/**
 * An implementaiton of the UploadState that sends an error message 
 * for the limit of uploads allowed has been reached. This is an
 * HTTP 503 error.
 */
public class LimitReachedUploadState implements HTTPMessage {

    private static final Log LOG = LogFactory.getLog(LimitReachedUploadState.class);
	
	/**
	 * Constant for the <tt>FileDesc</tt> instance that was requested.
	 */
	private final FileDesc FILE_DESC;

    private final HTTPUploader UPLOADER;
    
    /**
     * The time to wait for a normal retry after.
     */
    public static final int RETRY_AFTER_TIME = 60 * 15;

    /**
     * Number of seconds the remote host should wait before retrying in
     * case we don't have any alt-locs left to send. (20 minutes)
     */
    private static final String NO_ALT_LOCS_RETRY_AFTER = "" + (60 * 20);

    /**
     * Number of seconds the remote host should wait before retrying in
     * case we still have alt-locs left to send. (15 minute)
     */
    private static final String NORMAL_RETRY_AFTER = "" + RETRY_AFTER_TIME;

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
		LOG.debug("creating limit reached state");
        UPLOADER = uploader;
		FILE_DESC = uploader.getFileDesc();
	}

	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		LOG.debug("writing headers");
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
				// write the Retry-After header, using different values
				// depending on if we had any alts to send or not.
				HTTPUtils.writeHeader(HTTPHeaderName.RETRY_AFTER,
				    alts.size() == 0 ? 
				        NO_ALT_LOCS_RETRY_AFTER : NORMAL_RETRY_AFTER,
				    ostream);
                ostream.write(str.getBytes());
                if (FILE_DESC instanceof IncompleteFileDesc) {
                    HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                          ((IncompleteFileDesc)FILE_DESC),
                                          ostream);
                }
			} else {
			    HTTPUtils.writeHeader(HTTPHeaderName.RETRY_AFTER,
			                          NO_ALT_LOCS_RETRY_AFTER,
			                          ostream);
            }
		}
		
		HTTPUtils.writeHeader(HTTPHeaderName.CONNECTION,
		                      ConstantHTTPHeaderValue.CLOSE_VALUE,
		                      ostream);
		                      
		str = "\r\n";
		ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
		LOG.debug("writing body");
		ostream.write(ERROR_MESSAGE);
	}
	
	public boolean getCloseConnection() {
	    return true;
	}
}
