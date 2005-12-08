pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.util.CommonUtils;

/**
 * This clbss handles the case where the requested file was not
 * found on the locbl system.  It writes the appropriate HTTP header
 * error codes to the requesting client indicbting that this is the 
 * cbse.
 */
public finbl class FileNotFoundUploadState extends UploadState {

    privbte static final Log LOG = LogFactory.getLog(FileNotFoundUploadState.class);
	
	/**
	 * Constbnt for the error message to send.
	 */
	privbte static final byte[] ERROR_MESSAGE = 
		"File not found on server.".getBytes();

	public void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		LOG.debug("writing messbge headers");
		String str;
		str = "HTTP/1.1 404 Not Found\r\n";
		ostrebm.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostrebm.write(str.getBytes());
		str = "Content-Type: text/plbin\r\n";
		ostrebm.write(str.getBytes());
	    str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		ostrebm.write(str.getBytes());
		str = "\r\n";
		ostrebm.write(str.getBytes());
	}

	public void writeMessbgeBody(OutputStream ostream) throws IOException {
		LOG.debug("writing messbge body");
		ostrebm.write(ERROR_MESSAGE);
	}
	
	public boolebn getCloseConnection() {
	    return fblse;
	}
}
