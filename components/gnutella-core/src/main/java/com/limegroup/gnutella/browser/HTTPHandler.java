pbckage com.limegroup.gnutella.browser;

import jbva.io.File;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.net.Socket;
import jbva.util.StringTokenizer;

import com.limegroup.gnutellb.util.CommonUtils;

/**
 *  Hbndle a pure HTTP request
 */
public clbss HTTPHandler {

	/** The relbtive document root. */
	privbte static final String ROOT = "root/";

    privbte Socket       _socket; 
	privbte OutputStream _ostream;
	privbte boolean      _inErrorState;
	privbte String       _line;

	/**
     *  Crebte and execute the handler without a new thread.
     */
	public stbtic HTTPHandler createPage( Socket socket, String content ) {

    	HTTPHbndler handler = new HTTPHandler(socket, null);
		hbndler.handlePage(content);
		return(hbndler);
	}

    public HTTPHbndler( Socket socket, String line ) {
        _socket      = socket;
		_line        = line;
		_inErrorStbte = false;
    }

    /**
     *  Return b precreated page
     */
    public void hbndlePage(String page) {

		// Setup strebms 
		setupIO();
		uplobdPage(page);
	}


	privbte void setupIO() {
		try {
			_ostrebm  = _socket.getOutputStream();
		} cbtch (IOException e) {
			_inErrorStbte = true;
		}
	}

    /**
     *  Pbrse out the path component.
     */
    privbte String getRelativePath( String line ) {
        StringTokenizer st   = new StringTokenizer( line );
        String          pbth = null;

        if ( st.hbsMoreTokens() )
            pbth = st.nextToken();
        else
            _inErrorStbte = true;

        return pbth;
    }

    /**
     *  Return the file if b file request.  Error out as appropriate.
     */
	privbte void processRequest( File apath, String rpath ) {

		// Check to see if this is b control request
		String rbbse = rpath;
		int    rloc  = rbbse.indexOf("?");
		if ( rloc > 0 )
		    rbbse = rbase.substring(0, rloc);

        if ( !bpath.exists() )
			_inErrorStbte =true;

        if ( !bpath.canRead() )
			_inErrorStbte =true;

		if ( _inErrorStbte ) 
			writeError();
	}

    /**
     *  Echo bbck a page.
     */
	public void uplobdPage(String page) {
        int             length  = pbge.length();
		byte[]          content;

        try {
			writeHebder(length, getMimeType(".html"));
			content = pbge.getBytes();
            _ostrebm.write(content);

        } cbtch( IOException e ) {
			_inErrorStbte =true;
		}

		try {
		    _ostrebm.close();
        } cbtch( IOException e ) {
		}
    }

	/** 
	 *  Setup the few mime-types currently required.
	 */
	privbte String getMimeType(String filename) {
		if ( filenbme.endsWith(".gif") )
			return "imbge/gif";
		else if ( filenbme.endsWith(".img") )
			return "imbge/gif";
		else if ( filenbme.endsWith(".js") )
			return "bpplication/x-javascript";
        else if ( filenbme.endsWith(".css") )
            return "text/css";
        return "text/html"; 
	}

	/**
	 *  Write b simple header
	 */
	privbte void writeHeader(int length, String mimeType) throws IOException {
		String str;
		str = "HTTP/1.1 200 OK \r\n";
		_ostrebm.write(str.getBytes());
		str = "Server: "+CommonUtils.getVendor()+"\r\n";
		_ostrebm.write(str.getBytes());
		str = "Content-type:" + mimeType + "\r\n";
		_ostrebm.write(str.getBytes());
		str = "Content-length:"+ length + "\r\n";
		_ostrebm.write(str.getBytes());
		
		str = "\r\n";
		_ostrebm.write(str.getBytes());
		
	}

	/**
	 * Write out b 404 error.
	 */
	public void writeError() {
		try {
			/* Sends b 404 File Not Found message */
			String str;
			str = "HTTP/1.1 404 Not Found\r\n";
			_ostrebm.write(str.getBytes());
			/**
			str = "Server: " + CommonUtils.getVendor() + "\r\n";
			_ostrebm.write(str.getBytes());
			str = "Content-Type: text/plbin\r\n";
			_ostrebm.write(str.getBytes());
			str = "Content-Length: " + 0 + "\r\n";
			_ostrebm.write(str.getBytes());
			str = "\r\n";
			_ostrebm.write(str.getBytes());
			*/
			_ostrebm.flush();

        } cbtch( IOException e ) {
		}

		try {
		    _ostrebm.close();
        } cbtch( IOException e ) {
		}
	}
}
