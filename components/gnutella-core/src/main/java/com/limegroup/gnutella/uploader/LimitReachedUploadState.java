pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.util.CommonUtils;


/**
 * An implementbiton of the UploadState that sends an error message 
 * for the limit of uplobds allowed has been reached. This is an
 * HTTP 503 error.
 */
public clbss LimitReachedUploadState extends UploadState {


    privbte static final Log LOG = LogFactory.getLog(LimitReachedUploadState.class);
	

    
    
    /**
     * The time to wbit for a normal retry after.
     */
    public stbtic final int RETRY_AFTER_TIME = 60 * 15;

    /**
     * Number of seconds the remote host should wbit before retrying in
     * cbse we don't have any alt-locs left to send. (20 minutes)
     */
    privbte static final String NO_ALT_LOCS_RETRY_AFTER = "" + (60 * 20);

    /**
     * Number of seconds the remote host should wbit before retrying in
     * cbse we still have alt-locs left to send. (15 minute)
     */
    privbte static final String NORMAL_RETRY_AFTER = "" + RETRY_AFTER_TIME;

	/**
	 * The error messbge to send in the message body.
	 */
	privbte static final byte[] ERROR_MESSAGE = 
		"Server busy.  Too mbny active uploads.".getBytes();

	/**
	 * Crebtes a new <tt>LimitReachedUploadState</tt> with the specified
	 * <tt>FileDesc</tt>.
	 *
	 * @pbram fd the <tt>FileDesc</tt> for the upload
	 */
	public LimitRebchedUploadState(HTTPUploader uploader) {
		super(uplobder);

		LOG.debug("crebting limit reached state");
	}

	public void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		LOG.debug("writing hebders");
		String str;
		str = "HTTP/1.1 503 Service Unbvailable\r\n";
		ostrebm.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostrebm.write(str.getBytes());
		str = "Content-Type: text/plbin\r\n";
		ostrebm.write(str.getBytes());
		str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		ostrebm.write(str.getBytes());
		writeProxies(ostrebm);
		writeAlts(ostrebm);
		if(FILE_DESC != null) {
			// write the URN in cbse the caller wants it
			URN shb1 = FILE_DESC.getSHA1Urn();
			if(shb1 != null) {
				
				// write the Retry-After hebder, using different values
				// depending on if we hbd any alts to send or not.
				HTTPUtils.writeHebder(HTTPHeaderName.RETRY_AFTER,
				    ! RouterService.getAltlocMbnager().hasAltlocs(FILE_DESC.getSHA1Urn()) ? 
				        NO_ALT_LOCS_RETRY_AFTER : NORMAL_RETRY_AFTER,
				    ostrebm);
                ostrebm.write(str.getBytes());
                writeRbnges(ostream);
			} else {
			    HTTPUtils.writeHebder(HTTPHeaderName.RETRY_AFTER,
			                          NO_ALT_LOCS_RETRY_AFTER,
			                          ostrebm);
            }
		}
		
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
