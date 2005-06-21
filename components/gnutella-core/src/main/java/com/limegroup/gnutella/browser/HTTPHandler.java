package com.limegroup.gnutella.browser;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.StringTokenizer;

import com.limegroup.gnutella.util.CommonUtils;

/**
 *  Handle a pure HTTP request
 */
public class HTTPHandler {

	/** The relative document root. */
	private static final String ROOT = "root/";

    private Socket       _socket; 
	private OutputStream _ostream;
	private boolean      _inErrorState;
	private String       _line;

	/**
     *  Create and execute the handler without a new thread.
     */
	public static HTTPHandler createPage( Socket socket, String content ) {

    	HTTPHandler handler = new HTTPHandler(socket, null);
		handler.handlePage(content);
		return(handler);
	}

    public HTTPHandler( Socket socket, String line ) {
        _socket      = socket;
		_line        = line;
		_inErrorState = false;
    }

    /**
     *  Return a precreated page
     */
    public void handlePage(String page) {

		// Setup streams 
		setupIO();
		uploadPage(page);
	}


	private void setupIO() {
		try {
			_ostream  = _socket.getOutputStream();
		} catch (IOException e) {
			_inErrorState = true;
		}
	}

    /**
     *  Parse out the path component.
     */
    private String getRelativePath( String line ) {
        StringTokenizer st   = new StringTokenizer( line );
        String          path = null;

        if ( st.hasMoreTokens() )
            path = st.nextToken();
        else
            _inErrorState = true;

        return path;
    }

    /**
     *  Return the file if a file request.  Error out as appropriate.
     */
	private void processRequest( File apath, String rpath ) {

		// Check to see if this is a control request
		String rbase = rpath;
		int    rloc  = rbase.indexOf("?");
		if ( rloc > 0 )
		    rbase = rbase.substring(0, rloc);

        if ( !apath.exists() )
			_inErrorState =true;

        if ( !apath.canRead() )
			_inErrorState =true;

		if ( _inErrorState ) 
			writeError();
	}

    /**
     *  Echo back a page.
     */
	public void uploadPage(String page) {
        int             length  = page.length();
		byte[]          content;

        try {
			writeHeader(length, getMimeType(".html"));
			content = page.getBytes();
            _ostream.write(content);

        } catch( IOException e ) {
			_inErrorState =true;
		}

		try {
		    _ostream.close();
        } catch( IOException e ) {
		}
    }

	/** 
	 *  Setup the few mime-types currently required.
	 */
	private String getMimeType(String filename) {
		if ( filename.endsWith(".gif") )
			return "image/gif";
		else if ( filename.endsWith(".img") )
			return "image/gif";
		else if ( filename.endsWith(".js") )
			return "application/x-javascript";
        else if ( filename.endsWith(".css") )
            return "text/css";
        return "text/html"; 
	}

	/**
	 *  Write a simple header
	 */
	private void writeHeader(int length, String mimeType) throws IOException {
		String str;
		str = "HTTP/1.1 200 OK \r\n";
		_ostream.write(str.getBytes());
		str = "Server: "+CommonUtils.getVendor()+"\r\n";
		_ostream.write(str.getBytes());
		str = "Content-type:" + mimeType + "\r\n";
		_ostream.write(str.getBytes());
		str = "Content-length:"+ length + "\r\n";
		_ostream.write(str.getBytes());
		
		str = "\r\n";
		_ostream.write(str.getBytes());
		
	}

	/**
	 * Write out a 404 error.
	 */
	public void writeError() {
		try {
			/* Sends a 404 File Not Found message */
			String str;
			str = "HTTP/1.1 404 Not Found\r\n";
			_ostream.write(str.getBytes());
			/**
			str = "Server: " + CommonUtils.getVendor() + "\r\n";
			_ostream.write(str.getBytes());
			str = "Content-Type: text/plain\r\n";
			_ostream.write(str.getBytes());
			str = "Content-Length: " + 0 + "\r\n";
			_ostream.write(str.getBytes());
			str = "\r\n";
			_ostream.write(str.getBytes());
			*/
			_ostream.flush();

        } catch( IOException e ) {
		}

		try {
		    _ostream.close();
        } catch( IOException e ) {
		}
	}
}
