package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.http.*;

import java.io.*;
import com.sun.java.util.collections.Set;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Iterator;

public class QueuedUploadState implements HTTPMessage {

    private final FileDesc FILE_DESC;
    private final HTTPUploader UPLOADER;
    private final int POSITION;

    public QueuedUploadState(int pos, HTTPUploader uploader) {
        this.POSITION = pos;
        this.UPLOADER = uploader;
        this.FILE_DESC= uploader.getFileDesc();
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
        ", pollMin="+(UploadManager.MIN_POLL_TIME/1000)+/*mS to S*/
        ", pollMax="+(UploadManager.MAX_POLL_TIME/1000)+/*mS to S*/"\r\n";
        ostream.write(str.getBytes());
        if(FILE_DESC != null) {
            // write the URN in case the caller wants it
            URN sha1 = FILE_DESC.getSHA1Urn();
            if(sha1!=null) {
                HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN,
                                      FILE_DESC.getSHA1Urn(),
                                      ostream);
                Set alts = UPLOADER.getNextSetOfAltsToSend();
				if(alts.size() > 0) {
					HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHeaderValueCollection(alts),
                                          ostream);
				}
				
				if (UPLOADER.wantsFAlts()) {
					alts = UPLOADER.getNextSetOfPushAltsToSend();
					if(alts.size() > 0) {
						HTTPUtils.writeHeader(HTTPHeaderName.FALT_LOCATION,
	                                          new HTTPHeaderValueCollection(alts),
	                                          ostream);
					}
					
				}
                if (FILE_DESC instanceof IncompleteFileDesc) {
                    HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                          ((IncompleteFileDesc)FILE_DESC),
                                          ostream);
                }
            }
        }
        
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
