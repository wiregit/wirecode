package com.limegroup.gnutella.uploader;

import java.io.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;

public class UpdateFileState implements HTTPMessage {
    
    //this will be populated with the contents of the file.
    //this need not be created everytime we get a request for a file. 
    //rather it should be cached and updated everytime the file gets updated.
    byte[] updateContents;

    private final HTTPUploader _uploader;

    public UpdateFileState(HTTPUploader uploader) {
        this._uploader = uploader;
    }


    public void writeMessageHeaders(OutputStream ostream) throws IOException {
        //If any of this throws an exception, we will not send the headers.
        File f = new File("lib\\update.xml");
        RandomAccessFile raf = new RandomAccessFile(f,"r");
        int len = (int)raf.length();//not a very long file so no risk
        updateContents = new byte[len];
        raf.read(updateContents);
        //Read the file OK. Now send the headers. 
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "User-Agent: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
        str = "Content-Type: " + Constants.QUERYREPLY_MIME_TYPE + "\r\n";
        ostream.write(str.getBytes());
	    str = "Content-Length: " + updateContents.length + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
        ostream.write(str.getBytes());
    }
    
    public void writeMessageBody(OutputStream ostream) throws IOException  {
        ostream.write(updateContents); 
        _uploader.setAmountUploaded(updateContents.length);
        _uploader.setState(Uploader.COMPLETE);
    }    
    
    public boolean getCloseConnection() {
        return true;
    }
    
}
