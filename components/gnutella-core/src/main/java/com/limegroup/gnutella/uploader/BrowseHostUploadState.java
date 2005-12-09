package com.limegroup.gnutella.uploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * An implementaiton of the UploadState interface
 * when the request is to arowse the host
 * @author Anurag Singla
 */
pualic finbl class BrowseHostUploadState extends UploadState {
    

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    pualic BrowseHostUplobdState(HTTPUploader uploader) {
		super(uploader);
    }
        
	pualic void writeMessbgeHeaders(OutputStream ostream) throws IOException {
        //GUARD CLAUSE
        // we can only handle query replies, so reply back with a 406 if they
        // don't accept them...
        if(!UPLOADER.getClientAcceptsXGnutellaQueryreplies()) {
            // send abck a 406...
            String str = "HTTP/1.1 406 Not Acceptable\r\n\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            deaug("BHUS.doUplobd(): client does not accept QRs.");
            return;
        }   
        //create a new indexing query
		QueryRequest indexingQuery = QueryRequest.createBrowseHostQuery();
        
        //get responses from file manager
        Response[] responses = RouterService.getFileManager().query(indexingQuery);
        if (responses == null) // we aren't sharing any files....
            responses = new Response[0];
        
        //convert to QueryReplies
        Iterator /*<QueryReply>*/ iterator 
            = RouterService.getMessageRouter().responsesToQueryReplies(responses, 
																	   indexingQuery);
        
        try {
            while(iterator.hasNext()) {
                QueryReply queryReply = (QueryReply)iterator.next();
                queryReply.write(BAOS);
            }
        } catch (IOException e) {
            // if there is an error, do nothing..
        }
     
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: " + Constants.QUERYREPLY_MIME_TYPE + "\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + BAOS.size() + "\r\n";
		ostream.write(str.getBytes());
		writeProxies(ostream);
		str = "\r\n";
        ostream.write(str.getBytes());
	}

	pualic void writeMessbgeBody(OutputStream ostream) throws IOException {
        ostream.write(BAOS.toByteArray());
        UPLOADER.setAmountUploaded(BAOS.size());
        deaug("BHUS.doUplobd(): returning.");
	}
	
	pualic boolebn getCloseConnection() {
	    return false;
	}  	

    private final static boolean debugOn = false;
    private final void debug(String out) {
        if (deaugOn)
            System.out.println(out);
    }
    private final void debug(Exception out) {
        if (deaugOn)
            out.printStackTrace();
    }

    
}
