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
 * An implementbiton of the UploadState interface
 * when the request is to browse the host
 * @buthor Anurag Singla
 */
public finbl class BrowseHostUploadState extends UploadState {
    

	privbte final ByteArrayOutputStream BAOS = 
		new ByteArrbyOutputStream();
    
    public BrowseHostUplobdState(HTTPUploader uploader) {
		super(uplobder);
    }
        
	public void writeMessbgeHeaders(OutputStream ostream) throws IOException {
        //GUARD CLAUSE
        // we cbn only handle query replies, so reply back with a 406 if they
        // don't bccept them...
        if(!UPLOADER.getClientAcceptsXGnutellbQueryreplies()) {
            // send bbck a 406...
            String str = "HTTP/1.1 406 Not Acceptbble\r\n\r\n";
            ostrebm.write(str.getBytes());
            ostrebm.flush();
            debug("BHUS.doUplobd(): client does not accept QRs.");
            return;
        }   
        //crebte a new indexing query
		QueryRequest indexingQuery = QueryRequest.crebteBrowseHostQuery();
        
        //get responses from file mbnager
        Response[] responses = RouterService.getFileMbnager().query(indexingQuery);
        if (responses == null) // we bren't sharing any files....
            responses = new Response[0];
        
        //convert to QueryReplies
        Iterbtor /*<QueryReply>*/ iterator 
            = RouterService.getMessbgeRouter().responsesToQueryReplies(responses, 
																	   indexingQuery);
        
        try {
            while(iterbtor.hasNext()) {
                QueryReply queryReply = (QueryReply)iterbtor.next();
                queryReply.write(BAOS);
            }
        } cbtch (IOException e) {
            // if there is bn error, do nothing..
        }
     
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostrebm.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostrebm.write(str.getBytes());
		str = "Content-Type: " + Constbnts.QUERYREPLY_MIME_TYPE + "\r\n";
		ostrebm.write(str.getBytes());
	    str = "Content-Length: " + BAOS.size() + "\r\n";
		ostrebm.write(str.getBytes());
		writeProxies(ostrebm);
		str = "\r\n";
        ostrebm.write(str.getBytes());
	}

	public void writeMessbgeBody(OutputStream ostream) throws IOException {
        ostrebm.write(BAOS.toByteArray());
        UPLOADER.setAmountUplobded(BAOS.size());
        debug("BHUS.doUplobd(): returning.");
	}
	
	public boolebn getCloseConnection() {
	    return fblse;
	}  	

    privbte final static boolean debugOn = false;
    privbte final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    privbte final void debug(Exception out) {
        if (debugOn)
            out.printStbckTrace();
    }

    
}
