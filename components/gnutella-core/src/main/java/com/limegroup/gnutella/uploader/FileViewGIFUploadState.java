package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.html.*;
import java.io.*;
import java.util.*;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * An implementaiton of the UploadState interface where you just give out a gif.
 */
public final class FileViewGIFUploadState implements HTTPMessage {

    public static final String logo = 
        "com/limegroup/gnutella/html/file_view_logo.gif";

    public static File _gif = null;
    public static byte[] _gifContents = new byte[0];

    private final HTTPUploader _uploader;

	private final ByteArrayOutputStream BAOS = 
		new ByteArrayOutputStream();
    
    public FileViewGIFUploadState(HTTPUploader uploader) {
		this._uploader = uploader;

        // get the logo
        synchronized (FileViewGIFUploadState.class) {
            if (_gifContents.length == 0) {
                try {
                    _gif = CommonUtils.getResourceFile(logo);
                    if (_gif != null) {
                        FileInputStream fr = new FileInputStream(_gif);
                        _gifContents = new byte[(int)_gif.length()];
                        int read = 0;
                        while (read < _gifContents.length)
                            read += fr.read(_gifContents, read, 
                                            (_gifContents.length - read));
                    }
                }
                catch (FileNotFoundException fnfe) {
                    ErrorService.error(fnfe, "Missing Resource!");
                }
                catch (IOException ignored) {}
            }
        }
    }

     
	public void writeMessageHeaders(OutputStream ostream) throws IOException {
     
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: image/gif\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + _gifContents.length + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
        ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
        ostream.write(_gifContents);
        _uploader.setAmountUploaded(_gifContents.length);
	}
	
	public boolean getCloseConnection() {
	    return false;
	}  	
}
