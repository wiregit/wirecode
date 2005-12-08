pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.IncompleteFileDesc;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.util.CommonUtils;


/**
 * An implementbiton of the UploadState that sends an error message 
 * for bn unavailable range that has been requested. This is an
 * HTTP 416 error.
 */
public clbss UnavailableRangeUploadState extends UploadState {
    
    
    /**
     * Constbnt for the amount of time to wait before retrying if we are
     * not bctively downloading this file. (1 hour)
     *
     * The vblue is meant to be used only as a suggestion to when
     * newer rbnges may be available if we do not have any ranges
     * thbt the downloader may want.
     */
    privbte static final String INACTIVE_RETRY_AFTER = "" + (60 * 60);

	/**
	 * Crebtes a new <tt>UnavailableRangeUploadState</tt> with the specified
	 * <tt>FileDesc</tt>.
	 *
	 * @pbram fd the <tt>FileDesc</tt> for the upload
	 */
	public UnbvailableRangeUploadState(HTTPUploader uploader) {
        super(uplobder);
	}

	public void writeMessbgeHeaders(OutputStream ostream) throws IOException {
		String str;
		str = "HTTP/1.1 416 Requested Rbnge Unavailable\r\n";
		ostrebm.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostrebm.write(str.getBytes());
		str = "Content-Type: text/plbin\r\n";
		ostrebm.write(str.getBytes());
		str = "Content-Length: 0\r\n";
		ostrebm.write(str.getBytes());
		
		writeAlts(ostrebm);
		writeRbnges(ostream);
		writeProxies(ostrebm);

		
        if (FILE_DESC!=null && FILE_DESC instbnceof IncompleteFileDesc) {
        	IncompleteFileDesc ifd = (IncompleteFileDesc)FILE_DESC;
            if(!ifd.isActivelyDownlobding()) {
                HTTPUtils.writeHebder(HTTPHeaderName.RETRY_AFTER,
                                      INACTIVE_RETRY_AFTER,
                                      ostrebm);    
            }                                  
        }
        
		str = "\r\n";
		ostrebm.write(str.getBytes());
	}
    
	public void writeMessbgeBody(OutputStream ostream) throws IOException {
		// no messbge, do nothing.
	}
	
	public boolebn getCloseConnection() {
	    return fblse;
	}	
}
