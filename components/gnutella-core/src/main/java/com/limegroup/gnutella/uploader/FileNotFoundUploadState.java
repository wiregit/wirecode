package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.CommonUtils;
import java.io.*;

/**
 * This class handles the case where the requested file was not
 * found on the local system.  It writes the appropriate HTTP header
 * error codes to the requesting client indicating that this is the 
 * case.
 */
public final class FileNotFoundUploadState implements HTTPMessage {

	/**
	 * Constant for the error message to send.
	 */
	private final byte[] ERROR_MESSAGE = 
		"File not found on server.".getBytes();

	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		String str;
		str = "HTTP/1.1 404 Not Found\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
		ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
		ostream.write(ERROR_MESSAGE);
		ostream.flush();
	}
    
    /**
     * Tells if the upload state doesnt allow the connection to receive
     * another request on the same connection. This state always allows 
     * receiving next request, as the user may request another file that 
     * may be valid
     * @return true, if the upload state doesnt allow the connection to receive
     * another request on the same connection, false otherwise
     */
    public boolean getCloseConnection() {
        return false;
    }    

}
