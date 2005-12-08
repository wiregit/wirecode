pbckage com.limegroup.gnutella.uploader;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.util.Iterator;

import com.limegroup.gnutellb.Constants;
import com.limegroup.gnutellb.Response;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.util.CommonUtils;

/**
 * A stub implementbtion of the UploadState interface
 * when the request is for b special browser-control operation.
 */
public finbl class BrowserControlUploadState extends UploadState {
    
    public BrowserControlUplobdState(HTTPUploader uploader) {
		super(uplobder);
    }
        
	public void writeMessbgeHeaders(OutputStream ostream) throws IOException {
        String str;
		str = "HTTP/1.1 404 Febture Not Active\r\n";
		ostrebm.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostrebm.write(str.getBytes());
	}

	public void writeMessbgeBody(OutputStream ostream) throws IOException {
	}
	
	public boolebn getCloseConnection() {
	    return true;
	}  	

}
