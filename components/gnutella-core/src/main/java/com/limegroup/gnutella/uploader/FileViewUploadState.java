package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.html.*;
import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * An implementaiton of the UploadState interface
 * when the request is to get a file view.
 */
public final class FileViewUploadState implements HTTPMessage {

    public final static byte[] BAD_PASS_REPLY = "Wrong Password!".getBytes();
    
    private final HTTPUploader _uploader;

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    public FileViewUploadState(HTTPUploader uploader) {
		this._uploader = uploader;
    }
        
	public void writeMessageHeaders(OutputStream ostream) throws IOException {
        final String fileName = _uploader.getFileName();
        if (fileName.endsWith(UploadManager.FV_PASS)) {
            FileListHTMLPage htmlGen = FileListHTMLPage.instance();
            BAOS.write(htmlGen.getSharedFilePage().getBytes());
        }
        else
            BAOS.write(BAD_PASS_REPLY);
     
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/html; charset=ISO-8859-1\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + BAOS.size() + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
        ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
        ostream.write(BAOS.toByteArray());
        _uploader.setAmountUploaded(BAOS.size());
        debug("BHUS.doUpload(): returning.");
	}
	
	public boolean getCloseConnection() {
	    return false;
	}  	

    private final static boolean debugOn = false;
    private final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }

    
}
