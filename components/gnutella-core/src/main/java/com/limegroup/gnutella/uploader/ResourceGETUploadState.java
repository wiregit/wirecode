package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.html.*;
import com.limegroup.gnutella.util.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import com.limegroup.gnutella.util.CommonUtils;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.stream.*;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;



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
        try {
            if(_uploader.getFileDesc() != null) {
                File f = _uploader.getFileDesc().getFile();
                BufferedImage bm = ImageIO.read(f);
    			int width = bm.getWidth();
    			int height = bm.getHeight();
    			if(width == 0 || height == 0)
    			    throw new IOException("no file");
    
    			int thumbWidth = 100;
    			if(width > thumbWidth) {
    			    int div = width/thumbWidth;
    			    height = height/div;
        			Image img = bm.getScaledInstance(thumbWidth, height ,Image.SCALE_FAST);
        
        			BufferedImage bi = new BufferedImage(thumbWidth, height, BufferedImage.TYPE_INT_RGB);
        			Graphics2D biContext = bi.createGraphics();
        			biContext.drawImage(img, 0, 0, null);
        			biContext.dispose();
        			bm = bi;
                }
    
    			ByteArrayOutputStream bo = new ByteArrayOutputStream();
    			if(!ImageIO.write(bm, "jpg", bo))
    			    throw new IOException();
                _resourceBytes = bo.toByteArray();
            } else {
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
		str = "Content-Type: image/jpg\r\n";
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
