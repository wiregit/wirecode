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
    
    /**
     * Generates a thumbnail for the given FileDesc image.
     */
    private byte[] generateThumbnail(FileDesc fd) throws IOException {
        ImageHandler handler = ImageManipulator.getDefaultImageHandler();
        Image wrote;
        BufferedImage read = handler.getBufferedImage(
                            handler.readImage(fd.getFile())
                          );
        int width = read.getWidth();
        int height = read.getHeight();
		int thumbWidth = 100;
		if(width > thumbWidth) {
		    int div = width/thumbWidth;
		    height = height/div;
		    wrote = handler.resize(read, thumbWidth, height);
        } else {
            wrote = read;
        }
        
	    return handler.write(wrote);
    }
     
	public void writeMessageHeaders(OutputStream ostream) throws IOException {

        boolean noResource = false;
        String mimeType = null;
        try {
            FileDesc fd = _uploader.getFileDesc();
            if(fd != null) {
                mimeType = "image/jpg";
                _resourceBytes = generateThumbnail(fd);
            } else {
                mimeType = HTTPUtils.getMimeType(_uploader.getFileName());
                InputStream fr = _uploader.getInputStream();
                _resourceBytes = new byte[_uploader.getFileSize()];
                int read = 0;
                while (read < _resourceBytes.length)
                    read += fr.read(_resourceBytes, read,
                                    (_resourceBytes.length - read));
            }
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
        
        _uploader.setFileSize(_resourceBytes.length);
     
        String str;
		str = "HTTP/1.1 200 OK\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + CommonUtils.getHttpServer() + "\r\n";
		ostream.write(str.getBytes());
		if(mimeType != null) {
		    str = "Content-Type: " + mimeType + "\r\n";
		    ostream.write(str.getBytes());
        }
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
