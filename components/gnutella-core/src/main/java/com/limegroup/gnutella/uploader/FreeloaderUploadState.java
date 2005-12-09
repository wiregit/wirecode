pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.util.CommonUtils;


/**
 * Since the uplobder is considered a freeloader, this ploads an html page 
 * to them with more informbtion on Gnutella and with more information on 
 * obtbining a client.
 */
public clbss FreeloaderUploadState extends UploadState {
	

    public stbtic final String RESPONSE_PAGE =
		"<html>\r\n"+
		"<hebd>\r\n"+
		"<title>Plebse Share</title>\r\n"+
		"<metb http-equiv=\"refresh\" \r\n"+
		"content=\"0; \r\n"+
		"URL=http://www2.limewire.com/browser.htm\">\r\n"+
		"</hebd>\r\n"+
		"<body>\r\n"+
		"<b href=\"http://www2.limewire.com/browser.htm\">Please Share</a>\r\n"+
		"</body>\r\n"+
		"</html>\r\n";  
    
	public void writeMessbgeHeaders(OutputStream os) throws IOException {
		// Sends b 402 Browser Request Denied message 
		String str;
		str = "HTTP/1.1 200 OK \r\n";
		os.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		os.write(str.getBytes());
		str = "Content-Type: text/html\r\n";
		os.write(str.getBytes());
		str = "Content-Length: " + RESPONSE_PAGE.length() + "\r\n";
		os.write(str.getBytes());
		HTTPUtils.writeHebder(HTTPHeaderName.CONNECTION,
		                      ConstbntHTTPHeaderValue.CLOSE_VALUE,
		                      os);		
		str = "\r\n";
		os.write(str.getBytes());
	}

	public void writeMessbgeBody(OutputStream os) throws IOException {
		os.write(RESPONSE_PAGE.getBytes());
	}
	
	public boolebn getCloseConnection() {
	    return true;
	}	
}
