package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;

public class QueuedUploadState extends UploadState {

    private final int POSITION;

    public QueuedUploadState(int pos, HTTPUploader uploader) {
    	super(uploader);
        this.POSITION = pos;
    }

    public void writeMessageHeaders(OutputStream ostream) throws IOException {
        String str;
        //if not queued, this should never be the state
        Assert.that(POSITION!=-1);
        str = "HTTP/1.1 503 Service Unavailable\r\n";
        ostream.write(str.getBytes());
        HTTPUtils.writeHeader(HTTPHeaderName.SERVER,
							  ConstantHTTPHeaderValue.SERVER_VALUE,ostream);
        str = "X-Queue: position="+(POSITION+1)+
        ", pollMin="+(HTTPSession.MIN_POLL_TIME/1000)+/*mS to S*/
        ", pollMax="+(HTTPSession.MAX_POLL_TIME/1000)+/*mS to S*/"\r\n";
        ostream.write(str.getBytes());
        
        writeAlts(ostream);
        writeRanges(ostream);
        if (UPLOADER.isFirstReply())
            HTTPUtils.writeFeatures(ostream);
            
        // write X-Thex-URI header with root hash if we have already 
        // calculated the tigertree
        if (FILE_DESC.getHashTree()!=null)
            HTTPUtils.writeHeader(HTTPHeaderName.THEX_URI,
                                  FILE_DESC.getHashTree(),
                                  ostream);                    

        str = "\r\n";
        ostream.write(str.getBytes());
    }

    public void writeMessageBody(OutputStream ostream) throws IOException {
        //this method should MUST NOT do anything.
    }
    
	public boolean getCloseConnection() {
	    return false;
	}    
}
