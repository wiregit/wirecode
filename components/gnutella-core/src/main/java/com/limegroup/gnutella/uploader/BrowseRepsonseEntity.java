package com.limegroup.gnutella.uploader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

public class BrowseRepsonseEntity extends AbstractHttpEntity {

    private final ByteArrayOutputStream BAOS = 
        new ByteArrayOutputStream();

    public BrowseRepsonseEntity() {
        //create a new indexing query
        QueryRequest indexingQuery = QueryRequest.createBrowseHostQuery();
        
        //get responses from file manager
        Response[] responses = RouterService.getFileManager().query(indexingQuery);
        if (responses == null) // we aren't sharing any files....
            responses = new Response[0];
        
        //convert to QueryReplies
        Iterable<QueryReply> iterable 
            = RouterService.getMessageRouter().responsesToQueryReplies(responses, 
                                                                       indexingQuery);
        
        try {
            for(QueryReply queryReply : iterable)
                queryReply.write(BAOS);
        } catch (IOException e) {
            // if there is an error, do nothing..
        }
     
        //UPLOADER.setAmountUploaded(BAOS.size());
        setContentType(Constants.QUERYREPLY_MIME_TYPE);
    }
    
    public InputStream getContent() throws IOException, IllegalStateException {
        return new ByteArrayInputStream(BAOS.toByteArray());
    }

    public long getContentLength() {
        return -1;
    }

    public boolean isRepeatable() {
        return false;
    }

    public boolean isStreaming() {
        return true;
    }

    public void writeTo(OutputStream outstream) throws IOException {
        outstream.write(BAOS.toByteArray());        
    }


}
