package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.html.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.image.ImageHandler;
import com.limegroup.gnutella.image.ImageManipulator;

import java.io.*;
import java.util.*;
import java.awt.Image;
import java.awt.image.BufferedImage;



/**
 * An implementaiton of the UploadState interface where you just give out a gif.
 */
public final class ResourceGETUploadState implements HTTPMessage {

    public final String NOT_FOUND = "HTTP/1.1 404 Not Found";
    public final String INT_ERROR = "HTTP/1.1 500 Internal Error";

    private byte[] _resourceBytes = new byte[0];
    private final HTTPUploader _uploader;

    public ResourceGETUploadState(HTTPUploader uploader) {
		this._uploader = uploader;
    }
    
    public void writeMessageHeaders(OutputStream ostream) throws IOException {

        boolean noResource = false;
        String mimeType = HTTPUtils.getMimeType(_uploader.getFileName());
        String str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		if(mimeType != null) {
		    str = "Content-Type: " + mimeType + "\r\n";
		    ostream.write(str.getBytes());
        }
	    str = "Content-Length: " + _uploader.getFileSize() + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
        ostream.write(str.getBytes());
	}
	
    private static final int BLOCK_SIZE=1024;
	public void writeMessageBody(OutputStream ostream) throws IOException {
        // construct the buffer outside of the loop, so we don't
        // have to reconstruct new byte arrays every BLOCK_SIZE.
        byte[] buf = new byte[BLOCK_SIZE];
        InputStream fr = _uploader.getInputStream();        
        while (true) {
            int c = fr.read(buf, 0, BLOCK_SIZE);
            if (c == -1)
                return;
            ostream.write(buf, 0, c);
        }
	}
	
	public boolean getCloseConnection() {
	    return false;
	}  	
}
