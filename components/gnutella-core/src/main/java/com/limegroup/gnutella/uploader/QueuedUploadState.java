package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import java.io.*;


public class QueuedUploadState implements HTTPMessage {

    private UploadManager uploadManager;
    private Uploader uploader;


    public QueuedUploadState(UploadManager manager, Uploader uploader) {
        this.uploadManager = manager;
        this.uploader = uploader;
    }

    public void writeMessageHeaders(OutputStream ostream) throws IOException {
        //TODO1:
        System.out.println("incomplete");
    }

    public void writeMessageBody(OutputStream ostream) throws IOException {
        //TODO1:
        System.out.println("incomplete");
    }
    
    public boolean getCloseConnection() {
        //TODO1:depends on the timeout 
        return false;
    }

}
