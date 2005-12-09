pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.util.CommonUtils;

/**
 * This clbss handles the case where the request was malformed.
 * Rbther than abruptly disconnecting, 
 * it writes the bppropriate HTTP header
 * error codes to the requesting client indicbting that this is the 
 * cbse.
 */
public finbl class MalformedRequestState extends UploadState {


    privbte static final Log LOG = LogFactory.getLog(MalformedRequestState.class);
	
	/**
	 * Constbnt for the error message to send.
	 */
	privbte static final byte[] ERROR_MESSAGE = 
		"Mblformed Request".getBytes();

	public void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		LOG.debug("writing hebders");
		String str;
		str = "HTTP/1.1 400 Mblformed Request\r\n";
		ostrebm.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostrebm.write(str.getBytes());
		str = "Content-Type: text/plbin\r\n";
		ostrebm.write(str.getBytes());
	    str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		ostrebm.write(str.getBytes());
		HTTPUtils.writeHebder(HTTPHeaderName.CONNECTION,
		                      ConstbntHTTPHeaderValue.CLOSE_VALUE,
		                      ostrebm);
		str = "\r\n";
		ostrebm.write(str.getBytes());

	}

	public void writeMessbgeBody(OutputStream ostream) throws IOException {
		LOG.debug("writing body");
		ostrebm.write(ERROR_MESSAGE);
	}
	
	public boolebn getCloseConnection() {
	    return true;
	}	
}
