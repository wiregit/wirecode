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
public final class ResourceGETUploadState implements HTTPMessage {

    private byte[] _resourceBytes = null;
    private final HTTPUploader _uploader;

    public ResourceGETUploadState(HTTPUploader uploader) {
		this._uploader = uploader;
    }

     
	public void writeMessageHeaders(OutputStream ostream) throws IOException {

        String resource = _uploader.getFileName();

        boolean noResource = false;
        try {
            File resourceFile = CommonUtils.getResourceFile(resource);
            FileInputStream fr = new FileInputStream(resourceFile);
            _resourceBytes = new byte[(int)resourceFile.length()];
            int read = 0;
            while (read < _resourceBytes.length)
                read += fr.read(_resourceBytes, read,
                                (_resourceBytes.length - read));
        }
        catch (FileNotFoundException fnfe) {
            String str = "HTTP/1.1 404 Not Found\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            return;
        }
        catch (IOException ioe) {
            String str = "HTTP/1.1 500 Internal Error\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            return;
        }
     
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: image/gif\r\n";
		ostream.write(str.getBytes());
	    str = "Content-Length: " + _resourceBytes.length + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
        ostream.write(str.getBytes());
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
        ostream.write(_resourceBytes);
        _uploader.setAmountUploaded(_resourceBytes.length);
	}
	
	public boolean getCloseConnection() {
	    return false;
	}  	
}
