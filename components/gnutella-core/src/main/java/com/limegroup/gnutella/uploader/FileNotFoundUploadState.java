package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class handles the case where the requested file was not
 * found on the local system.  It writes the appropriate HTTP header
 * error codes to the requesting client indicating that this is the 
 * case.
 */
public final class FileNotFoundUploadState extends UploadState {

    private static final Log LOG = LogFactory.getLog(FileNotFoundUploadState.class);
	
	/**
	 * Constant for the error message to send.
	 */
	private static final byte[] ERROR_MESSAGE = 
		"File not found on server.".getBytes();

	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		LOG.debug("writing message headers");
		String str;
		str = "HTTP/1.1 404 Not Found\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + LimeWireUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
		ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
		LOG.debug("writing message body");
		ostream.write(ERROR_MESSAGE);
	}
	
	public boolean getCloseConnection() {
	    return false;
	}
}
