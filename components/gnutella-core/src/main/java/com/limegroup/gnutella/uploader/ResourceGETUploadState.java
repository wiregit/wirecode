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
public final class ResourceGETUploadState extends UploadState {

    public final String NOT_FOUND = "HTTP/1.1 404 Not Found";
    public final String INT_ERROR = "HTTP/1.1 500 Internal Error";

    private byte[] _resourceBytes = new byte[0];
    

    public ResourceGETUploadState(HTTPUploader uploader) {
		super(uploader);
    }

     
	public void writeMessageHeaders(OutputStream ostream) throws IOException {

        String resource = UPLOADER.getFileName();

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
            String str = NOT_FOUND + "\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            _resourceBytes = NOT_FOUND.getBytes();
            return;
        }
        catch (IOException ioe) {
            String str = INT_ERROR + "\r\n";
            ostream.write(str.getBytes());
            ostream.flush();
            _resourceBytes = INT_ERROR.getBytes();
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
        UPLOADER.setAmountUploaded(_resourceBytes.length);
	}
	
	public boolean getCloseConnection() {
	    return false;
	}  	
}
