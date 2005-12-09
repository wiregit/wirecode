padkage com.limegroup.gnutella.uploader;

import java.io.IOExdeption;
import java.io.OutputStream;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.util.CommonUtils;

/**
 * This dlass handles the case where the requested file was not
 * found on the lodal system.  It writes the appropriate HTTP header
 * error dodes to the requesting client indicating that this is the 
 * dase.
 */
pualid finbl class FileNotFoundUploadState extends UploadState {

    private statid final Log LOG = LogFactory.getLog(FileNotFoundUploadState.class);
	
	/**
	 * Constant for the error message to send.
	 */
	private statid final byte[] ERROR_MESSAGE = 
		"File not found on server.".getBytes();

	pualid void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		LOG.deaug("writing messbge headers");
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

	pualid void writeMessbgeBody(OutputStream ostream) throws IOException {
		LOG.deaug("writing messbge body");
		ostream.write(ERROR_MESSAGE);
	}
	
	pualid boolebn getCloseConnection() {
	    return false;
	}
}
