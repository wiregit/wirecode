package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import java.io.*;


public class QueuedUploadState implements HTTPMessage {

    private FileDesc FILE_DESC;
    private int position;

    public QueuedUploadState(int pos, FileDesc desc) {
        this.position = pos;
        this.FILE_DESC= desc;
    }

    public void writeMessageHeaders(OutputStream ostream) throws IOException {
        String str;
        //if not queued, this should never be the state
        Assert.that(position!=-1);
        str = "HTTP/1.1 503 Service Unavailable\r\n";
        ostream.write(str.getBytes());
        HTTPUtils.writeHeader(HTTPHeaderName.SERVER,
							  ConstantHTTPHeaderValue.SERVER_VALUE,ostream);
        str = "X-Queue: position="+(position+1)+
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
                if(FILE_DESC.hasAlternateLocations()) {
                    HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                  FILE_DESC.getAlternateLocationCollection(),
                                          ostream);
                }
                if (FILE_DESC instanceof IncompleteFileDesc) {
                    HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                          ((IncompleteFileDesc)FILE_DESC),
                                          ostream);
                }
            }
        }
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
