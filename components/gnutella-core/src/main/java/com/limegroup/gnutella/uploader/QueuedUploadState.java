package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import java.io.*;


public class QueuedUploadState implements HTTPMessage {

    private UploadManager uploadManager;
    private Uploader uploader;
    private boolean closeConnection = false;
    private final FileDesc FILE_DESC;

	/**
	 * The error message to send in the message body.
	 */
	private final byte[] ERROR_MESSAGE = 
		"Server busy.  Too many active uploads.".getBytes();

    public QueuedUploadState(UploadManager manager, Uploader uploader, 
                             FileDesc desc) {
        this.uploadManager = manager;
        this.uploader = uploader;
        this.FILE_DESC= desc;
    }

    public void writeMessageHeaders(OutputStream ostream) throws IOException {
        String str;
        int pos = uploadManager.positionInQueue(uploader);
        Assert.that(pos!=-1);//if not queued, this should never be the state
        str = "HTTP/1.1 503 Service Unavailable\r\n";
        ostream.write(str.getBytes());
        str = "X-Queue: position="+pos+
        ", pollMin="+(UploadManager.MIN_POLL_TIME/1000)+/*mS to S*/
        ", pollMax="+(UploadManager.MAX_POLL_TIME/1000)+/*mS to S*/"\r\n";
        ostream.write(str.getBytes());
        if(FILE_DESC != null) {
            // write the URN in case the caller wants it
            URN sha1 = FILE_DESC.getSHA1Urn();
            if(sha1!=null) {
                HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_URN,
                                      FILE_DESC.getSHA1Urn(),
                                      ostream);
                if(FILE_DESC.hasAlternateLocations()) {
                    HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                  FILE_DESC.getAlternateLocationCollection(),
                                          ostream);
                }
            }
        }
        str = "\r\n";
        ostream.write(str.getBytes());
    }

    public void writeMessageBody(OutputStream ostream) throws IOException {
        ostream.write(ERROR_MESSAGE);
    }
    
    public boolean getCloseConnection() {
        return closeConnection;
    }

}
