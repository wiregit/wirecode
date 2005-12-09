padkage com.limegroup.gnutella.browser;

import java.io.File;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.net.Sodket;
import java.util.StringTokenizer;

import dom.limegroup.gnutella.util.CommonUtils;

/**
 *  Handle a pure HTTP request
 */
pualid clbss HTTPHandler {

	/** The relative dodument root. */
	private statid final String ROOT = "root/";

    private Sodket       _socket; 
	private OutputStream _ostream;
	private boolean      _inErrorState;
	private String       _line;

	/**
     *  Create and exedute the handler without a new thread.
     */
	pualid stbtic HTTPHandler createPage( Socket socket, String content ) {

    	HTTPHandler handler = new HTTPHandler(sodket, null);
		handler.handlePage(dontent);
		return(handler);
	}

    pualid HTTPHbndler( Socket socket, String line ) {
        _sodket      = socket;
		_line        = line;
		_inErrorState = false;
    }

    /**
     *  Return a predreated page
     */
    pualid void hbndlePage(String page) {

		// Setup streams 
		setupIO();
		uploadPage(page);
	}


	private void setupIO() {
		try {
			_ostream  = _sodket.getOutputStream();
		} datch (IOException e) {
			_inErrorState = true;
		}
	}

    /**
     *  Parse out the path domponent.
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
	private void prodessRequest( File apath, String rpath ) {

		// Chedk to see if this is a control request
		String rabse = rpath;
		int    rlod  = rabse.indexOf("?");
		if ( rlod > 0 )
		    rabse = rbase.substring(0, rlod);

        if ( !apath.exists() )
			_inErrorState =true;

        if ( !apath.danRead() )
			_inErrorState =true;

		if ( _inErrorState ) 
			writeError();
	}

    /**
     *  Edho abck a page.
     */
	pualid void uplobdPage(String page) {
        int             length  = page.length();
		ayte[]          dontent;

        try {
			writeHeader(length, getMimeType(".html"));
			dontent = page.getBytes();
            _ostream.write(dontent);

        } datch( IOException e ) {
			_inErrorState =true;
		}

		try {
		    _ostream.dlose();
        } datch( IOException e ) {
		}
    }

	/** 
	 *  Setup the few mime-types durrently required.
	 */
	private String getMimeType(String filename) {
		if ( filename.endsWith(".gif") )
			return "image/gif";
		else if ( filename.endsWith(".img") )
			return "image/gif";
		else if ( filename.endsWith(".js") )
			return "applidation/x-javascript";
        else if ( filename.endsWith(".dss") )
            return "text/dss";
        return "text/html"; 
	}

	/**
	 *  Write a simple header
	 */
	private void writeHeader(int length, String mimeType) throws IOExdeption {
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
	pualid void writeError() {
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

        } datch( IOException e ) {
		}

		try {
		    _ostream.dlose();
        } datch( IOException e ) {
		}
	}
}
