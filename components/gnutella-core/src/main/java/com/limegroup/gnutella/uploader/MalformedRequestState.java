package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.CommonUtils;
import java.io.*;

/**
 * This class handles the case where the request was malformed.
 * Rather than abruptly disconnecting, 
 * it writes the appropriate HTTP header
 * error codes to the requesting client indicating that this is the 
 * case.
 */
public final class MalformedRequestState extends UploadState {


	/**
	 * Constant for the error message to send.
	 */
	private static final byte[] ERROR_MESSAGE = 
		"Malformed Request".getBytes();

	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		String str;
		str = "HTTP/1.1 400 Malformed Request\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		ostream.write(str.getBytes());
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
