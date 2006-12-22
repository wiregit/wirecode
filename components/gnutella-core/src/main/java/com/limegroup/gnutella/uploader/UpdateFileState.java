package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.util.LimeWireUtils;

public class UpdateFileState extends UploadState {
    
    //this will be populated with the contents of the file.
    //this need not be created everytime we get a request for a file. 
    //rather it should be cached and updated everytime the file gets updated.
    byte[] updateContents;

    

    public UpdateFileState(HTTPUploader uploader) {
    	super(uploader);
        
    }


    public void writeMessageHeaders(OutputStream ostream) throws IOException {
        //If any of this throws an exception, we will not send the headers.
        File f = new File(LimeWireUtils.getUserSettingsDir(),"update.xml");
        RandomAccessFile raf = new RandomAccessFile(f,"r");
        int len = (int)raf.length();//not a very long file so no risk
        updateContents = new byte[len];
        raf.read(updateContents);
        raf.close();
        //Read the file OK. Now send the headers. 
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "User-Agent: " + LimeWireUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
        str = "Content-Type: " + Constants.QUERYREPLY_MIME_TYPE + "\r\n";
        ostream.write(str.getBytes());
	    str = "Content-Length: " + updateContents.length + "\r\n";
		ostream.write(str.getBytes());
		HTTPUtils.writeHeader(HTTPHeaderName.CONNECTION,
		                      ConstantHTTPHeaderValue.CLOSE_VALUE,
		                      ostream);		
		str = "\r\n";
        ostream.write(str.getBytes());
    }
    
    public void writeMessageBody(OutputStream ostream) throws IOException  {
        ostream.write(updateContents); 
        UPLOADER.setAmountUploaded(updateContents.length);
    }
    
	public boolean getCloseConnection() {
	    return true;
	}    
}
