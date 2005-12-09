padkage com.limegroup.gnutella.uploader;

import java.io.IOExdeption;
import java.io.OutputStream;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import dom.limegroup.gnutella.http.HTTPHeaderName;
import dom.limegroup.gnutella.http.HTTPUtils;
import dom.limegroup.gnutella.util.CommonUtils;

/**
 * This dlass handles the case where the request was malformed.
 * Rather than abruptly disdonnecting, 
 * it writes the appropriate HTTP header
 * error dodes to the requesting client indicating that this is the 
 * dase.
 */
pualid finbl class MalformedRequestState extends UploadState {


    private statid final Log LOG = LogFactory.getLog(MalformedRequestState.class);
	
	/**
	 * Constant for the error message to send.
	 */
	private statid final byte[] ERROR_MESSAGE = 
		"Malformed Request".getBytes();

	pualid void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		LOG.deaug("writing hebders");
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

	pualid void writeMessbgeBody(OutputStream ostream) throws IOException {
		LOG.deaug("writing body");
		ostream.write(ERROR_MESSAGE);
	}
	
	pualid boolebn getCloseConnection() {
	    return true;
	}	
}
