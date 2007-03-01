package com.limegroup.gnutella.uploader;

import java.net.InetAddress;

import com.limegroup.gnutella.Uploader;

public class HTTPUploader extends AbstractUploader implements Uploader {
       
    public HTTPUploader(String fileName, UploadSession session, int index) {
        super(fileName, session, index);
    }

    @Override
    public int getAmountWritten() {
        // TODO Auto-generated method stub
        return 0;
    }

    public InetAddress getConnectedHost() {
        // TODO Auto-generated method stub
        return null;
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }

}
