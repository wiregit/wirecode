package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * An implementaiton of the UploadState interface
 * when the request is to browse the host
 * @author Anurag Singla
 */

public class BrowseHostUploadState implements UploadState
{
    
    private HTTPUploader _uploader;
    private OutputStream _ostream;
    private FileManager _fileManager;
    private MessageRouter _router;
    
    /** Indexing query (4 white spaces) */
    public static final String INDEXING_QUERY = "    ";
    
    
    public BrowseHostUploadState(FileManager fileManager, MessageRouter router)
    {
        this._fileManager = fileManager;
        this._router = router;
    }
    
    /**
     * This class implements a HTTP response for the file not being
     * found on the server.
     */
    public void doUpload(HTTPUploader uploader) throws IOException
    {
        //GUARD CLAUSE
        //dont do anything if the client cant accept Queryreplies in the 
        //response
        if(!uploader.getClientAcceptsXGnutellaQueryreplies())
            throw new IOException(
                "Client can not accept QueryReplies in HTTP Response");
        
        _uploader = uploader;
        _ostream = uploader.getOutputStream();
        
        //create a new indexing query
        QueryRequest indexingQuery
            = new QueryRequest((byte)1, 0, INDEXING_QUERY);
        
        //get responses from file manager
        Response[] responses = _fileManager.query(indexingQuery);
        
        //convert to QueryReplies
        Iterator /*<QueryReply>*/ iterator 
            = _router.responsesToQueryReplies(responses, indexingQuery);
        
        //get the bytes out of queryReplies
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        try {
            while(iterator.hasNext()) {
                QueryReply queryReply = (QueryReply)iterator.next();
                queryReply.write(outBytes);
            }
        } catch (IOException e) {
            // if there is an error, do nothing..
        }
        
        //write out the headers
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		_ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getVendor() + "\r\n";
		_ostream.write(str.getBytes());
		str = "Content-Type: " + Constants.QUERYREPLY_MIME_TYPE + "\r\n";
		_ostream.write(str.getBytes());
	    str = "Content-Length: " + outBytes.size() + "\r\n";
		_ostream.write(str.getBytes());
		str = "\r\n";
        _ostream.write(str.getBytes());
        
        //write out the content (query replies)
        _ostream.write(outBytes.toByteArray());
        _ostream.flush();
        
        _uploader.setAmountUploaded(outBytes.size());
        _uploader.setState(_uploader.COMPLETE);
    }
    
    /**
     * Tells if the upload state doesnt allow the connection to receive
     * another request on the same connection. This state doesnt allow
     * next request
     * @return always true
     */
    public boolean getCloseConnection()
    {
        return true;
    }
    
}
