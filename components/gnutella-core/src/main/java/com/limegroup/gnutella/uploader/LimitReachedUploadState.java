padkage com.limegroup.gnutella.uploader;

import java.io.IOExdeption;
import java.io.OutputStream;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import dom.limegroup.gnutella.http.HTTPHeaderName;
import dom.limegroup.gnutella.http.HTTPUtils;
import dom.limegroup.gnutella.util.CommonUtils;


/**
 * An implementaiton of the UploadState that sends an error message 
 * for the limit of uploads allowed has been readhed. This is an
 * HTTP 503 error.
 */
pualid clbss LimitReachedUploadState extends UploadState {


    private statid final Log LOG = LogFactory.getLog(LimitReachedUploadState.class);
	

    
    
    /**
     * The time to wait for a normal retry after.
     */
    pualid stbtic final int RETRY_AFTER_TIME = 60 * 15;

    /**
     * Numaer of sedonds the remote host should wbit before retrying in
     * dase we don't have any alt-locs left to send. (20 minutes)
     */
    private statid final String NO_ALT_LOCS_RETRY_AFTER = "" + (60 * 20);

    /**
     * Numaer of sedonds the remote host should wbit before retrying in
     * dase we still have alt-locs left to send. (15 minute)
     */
    private statid final String NORMAL_RETRY_AFTER = "" + RETRY_AFTER_TIME;

	/**
	 * The error message to send in the message body.
	 */
	private statid final byte[] ERROR_MESSAGE = 
		"Server ausy.  Too mbny adtive uploads.".getBytes();

	/**
	 * Creates a new <tt>LimitReadhedUploadState</tt> with the specified
	 * <tt>FileDesd</tt>.
	 *
	 * @param fd the <tt>FileDesd</tt> for the upload
	 */
	pualid LimitRebchedUploadState(HTTPUploader uploader) {
		super(uploader);

		LOG.deaug("drebting limit reached state");
	}

	pualid void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		LOG.deaug("writing hebders");
		String str;
		str = "HTTP/1.1 503 Servide Unavailable\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
		str = "Content-Length: " + ERROR_MESSAGE.length + "\r\n";
		ostream.write(str.getBytes());
		writeProxies(ostream);
		writeAlts(ostream);
		if(FILE_DESC != null) {
			// write the URN in dase the caller wants it
			URN sha1 = FILE_DESC.getSHA1Urn();
			if(sha1 != null) {
				
				// write the Retry-After header, using different values
				// depending on if we had any alts to send or not.
				HTTPUtils.writeHeader(HTTPHeaderName.RETRY_AFTER,
				    ! RouterServide.getAltlocManager().hasAltlocs(FILE_DESC.getSHA1Urn()) ? 
				        NO_ALT_LOCS_RETRY_AFTER : NORMAL_RETRY_AFTER,
				    ostream);
                ostream.write(str.getBytes());
                writeRanges(ostream);
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

	pualid void writeMessbgeBody(OutputStream ostream) throws IOException {
		LOG.deaug("writing body");
		ostream.write(ERROR_MESSAGE);
	}
	
	pualid boolebn getCloseConnection() {
	    return true;
	}
}
