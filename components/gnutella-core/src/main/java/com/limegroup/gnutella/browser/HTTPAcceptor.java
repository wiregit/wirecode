package com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.URLDecoder;
import java.util.StringTokenizer;
import java.net.URLEncoder;

/**
 * Listens on an HTTP port, accepts incoming connections, and dispatches 
 * threads to handle requests.  This allows simple HTTP requests.
 */
public class HTTPAcceptor implements Runnable {
	/** Magnet request for a default action on parameters */
    private static final String MAGNET_DEFAULT = "magnet10/default.js?";
	/** Magnet request for a paused response */
    private static final String MAGNET_PAUSE   = "magnet10/pause";
	/** Local machine ip */
    private static final String LOCALHOST      = "127.0.0.1";
	/** Start of Magnet URI */
    private static final String MAGNET         = "magnet:?";
	/** HTTP no content return */
	private static final String NOCONTENT      = "HTTP/1.1 204 No Content\r\n";
	/** Magnet detail command */
    private static final String MAGNETDETAIL   = "magcmd/detail?";
    /** Magnet email command */
    private static final String MAGNETEMAIL    = "magcmd/email?";

    /**
     * The socket that listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: obtain _socketLock before modifying either.  Notify _socketLock
     * when done.
     */
    private volatile ServerSocket _socket=null;
    private int 	 			  _port=45100;
    private Object                _socketLock=new Object();

	/** Try to supress duplicate requests from some browsers */
	private static String         _lastRequest     = null;
	private static long           _lastRequestTime = 0;


    /**
     * Links the HostCatcher up with the other back end pieces and launches
     * the port monitoring thread
     */
    public void start() {
		Thread httpAcceptorThread = new Thread(this, "HTTPAcceptor");
        httpAcceptorThread.setDaemon(true);
        httpAcceptorThread.start();
    }

    /**
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager is listening.
     *  If that fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     *  This is properly synchronized and can be called even while run() is
     *  being called.
     */
    private void setListeningPort(int port) throws IOException {
        //1. Special case: if unchanged, do nothing.
        if (_socket!=null && _port==port)
            return;
        //2. Special case if port==0.  This ALWAYS works.
        //Note that we must close the socket BEFORE grabbing
        //the lock.  Otherwise deadlock will occur since
        //the acceptor thread is listening to the socket
        //while holding the lock.  Also note that port
        //will not have changed before we grab the lock.
        else if (port==0) {
            //Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            synchronized (_socketLock) {
                _socket=null;
                _port=0;
                _socketLock.notify();
            }
            return;
        }
        //3. Normal case.  See note about locking above.
        else {
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket=new ServerSocket(port);
            } catch (IOException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new IOException();
            }
            //b) Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            //c) Replace with new sock.  Notify the accept thread.
            synchronized (_socketLock) {
                _socket=newSocket;
                _port=port;
                _socketLock.notify();
            }
            return;
        }
    }

    /**
     *  Return the listening port.
     */
    public int getPort() {
		return _port;
	}

    /** @modifies this, network
     *  @effects accepts http/magnet requests
     */
    public void run() {

        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
        Exception socketError = null;
        try {
            setListeningPort(_port);
        } catch (IOException e) {
			boolean error = true;
            socketError = e;
            //2. Try 10 different ports
            for (int i=0; i<10; i++) {
    			_port=i+45100;
                try {
                    setListeningPort(_port);
					error = false;
                    break;
                } catch (IOException e2) {
                    socketError = e2;
                }
            }

			if(error) {
                // If we still don't have a socket, there's an error
                // but ignore buggy tcp/ip startup on Mac Classic
                if ( !(socketError instanceof UnknownHostException &&
                      CommonUtils.isMacClassic()) ) 
				    ErrorService.error(e);
			}
        }

        while (true) {
            try {
                //Accept an incoming connection, make it into a
                //Connection object, handshake, and give it a thread
                //to service it.  If not bound to a port, wait until
                //we are.  If the port is changed while we are
                //waiting, IOException will be thrown, forcing us to
                //release the lock.
                Socket client=null;
                synchronized (_socketLock) {
                    if (_socket!=null) {
                        try {
                            client=_socket.accept();
                        } catch (IOException e) {
                            continue;
                        }
                    } else {
                        // When the socket lock is notified, the socket will
                        // be available.  So, just wait for that to happen and
                        // go around the loop again.
                        try {
                            _socketLock.wait();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                }

                //Dispatch asynchronously.
                new ConnectionDispatchRunner(client);

            } catch (SecurityException e) {
				ErrorService.error(e);
                return;
            } catch (Exception e) {
                //Internal error!
				ErrorService.error(e);
            }
        }
    }

    private class ConnectionDispatchRunner extends Thread {
        private Socket _socket;

        /**
         * @modifies socket, this' managers
         * @effects starts a new thread to handle the given socket and
         *  registers it with the appropriate protocol-specific manager.
         *  Returns once the thread has been started.  If socket does
         *  not speak a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispatchRunner(Socket socket) {
            super("ConnectionDispatchRunner");
            _socket=socket;
            setDaemon(true);
            this.start();
        }

        public void run() {
            try {
                //The try-catch below is a work-around for JDK bug 4091706.
                InputStream in=null;
                try {
                    in=_socket.getInputStream(); 
                } catch (Exception e) {
                    throw new IOException();
                }
                _socket.setSoTimeout(Constants.TIMEOUT);
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word=IOUtils.readWord(in,8);
                _socket.setSoTimeout(0);


                // Incoming upload via HTTP
                if (word.equals("GET")) {
					handleHTTPRequest(_socket);
                }
			    else if (word.equals("MAGNET")) {
                    ExternalControl.fireMagnet(_socket);
                }	
                //4. Unknown protocol
                else {
                    throw new IOException();
                }
            } catch (IOException e) {
                //handshake failed: try to close connection.
                try { _socket.close(); } catch (IOException e2) { }
            } catch(Exception e) {
				ErrorService.error(e);
			}
        }
    }


	/**
	 * Read and parse any HTTP request.  Forward on to HTTPHandler.
	 *
	 * @param socket the <tt>Socket</tt> instance over which we're reading
	 */
	private void handleHTTPRequest(Socket socket) throws IOException {

		// Only respond to localhost
		if ( !LOCALHOST.equals(
			  socket.getInetAddress().getHostAddress()) )
			return;

		// Set the timeout so that we don't do block reading.
        socket.setSoTimeout(Constants.TIMEOUT);
		// open the stream from the socket for reading
		ByteReader br = new ByteReader(socket.getInputStream());
		
        // read the first line. if null, throw an exception
        String str = br.readLine();

		if (str == null) {
			throw new IOException();
		}

		str.trim();
		str = URLDecoder.decode(str);

		if (str.indexOf("magnet10") > 0) {
			int loc = 0;
			if ((loc = str.indexOf(MAGNET_DEFAULT)) > 0) {
				//Parse params out
				int loc2 = str.lastIndexOf(" HTTP");
				String command = 
				  str.substring(loc+MAGNET_DEFAULT.length(), loc2);
				triggerMagnetHandling(socket, MAGNET+command);
			} else if ((loc = str.indexOf(MAGNET_PAUSE)) > 0) {
				//System.out.println("Pause called:"+str);
		        try { Thread.sleep(250); } catch(Exception e) {};
				returnNoContent(socket);
			} else {
			    // upload browsed files
		        HTTPHandler.create(socket, str);
			}
        } else if (str.indexOf(MAGNETDETAIL) >= 0) {
            int loc = 0;
            if ((loc = str.indexOf(MAGNETDETAIL)) < 0)
                return;
            int loc2 = str.lastIndexOf(" HTTP");
            String command = 
                  str.substring(loc+MAGNETDETAIL.length(), loc2);
            String page=buildMagnetDetailPage(command);
            
            HTTPHandler.createPage(socket, page);
		} else if (str.indexOf(MAGNETEMAIL) >= 0) {
			int loc = 0;
			if ((loc = str.indexOf(MAGNETEMAIL)) < 0)
				return;
			int loc2 = str.lastIndexOf(" HTTP");
			String command = 
				  str.substring(loc+MAGNETEMAIL.length(), loc2);
			String page=buildMagnetEmailPage(command);
			
		    HTTPHandler.createPage(socket, page);
		} else if (str.indexOf(MAGNET) >= 0) {
			// trigger an operation
			int loc  = str.indexOf(MAGNET);
			int loc2 = str.lastIndexOf(" HTTP");
			if ( loc < 0 )
				return;
			String command = str.substring(loc, loc2);
			triggerMagnetHandling(socket, command);
		} 
		try { socket.close(); } catch (IOException e) { }
  	}

	private void triggerMagnetHandling(Socket socket, String command) {

		// Supress duplicate requests from some browsers
		long curTime = (new Date()).getTime();
		if ( !(command.equals(_lastRequest) &&  
			   (curTime - _lastRequestTime) < 1500l) ) {
			
			// trigger an operation
			ExternalControl.handleMagnetRequest(command);
			_lastRequest     = command;
			_lastRequestTime = curTime;
		} 
        //else System.out.println("Duplicate request:"+command);
			
		returnNoContent(socket);
	}

    private String buildMagnetDetailPage(String cmd) {
        StringTokenizer st = new StringTokenizer(cmd, "&");
        String keystr;
        String valstr;
        int    start;
        String address = "";
        String fname   = "";
        String sha1    = "";
        String ret= magnetDetailPageHeader();
        
        // Process each key=value pair
        while (st.hasMoreTokens()) {
            keystr = st.nextToken();
            keystr = keystr.trim();
            start  = keystr.indexOf("=")+1;
            valstr = keystr.substring(start);
            keystr = keystr.substring(0,start-1);
            valstr=URLDecoder.decode(valstr);   
            if ( keystr.equals("addr") ) {
                address = valstr;
            } else if ( keystr.startsWith("n") ) {
                fname = valstr;
            } else if ( keystr.startsWith("u") ) {
                sha1 = valstr;
                ret += magnetDetail(address, fname, sha1);
            }
        }
        ret += 
          "</table>"+
          "</body></html>";
        return ret;
    }

	private String buildMagnetEmailPage(String cmd) {
		StringTokenizer st = new StringTokenizer(cmd, "&");
		String keystr;
		String valstr;
		int    start;
		String address = "";
		String fname   = "";
		String sha1    = "";
        String ret     = "";
        int    count   = 0;
        
		// Process each key=value pair
     	while (st.hasMoreTokens()) {
		    keystr = st.nextToken();
			keystr = keystr.trim();
			start  = keystr.indexOf("=")+1;
		    valstr = keystr.substring(start);
			keystr = keystr.substring(0,start-1);
			valstr=URLDecoder.decode(valstr);	
			if ( keystr.equals("addr") ) {
				address = valstr;
                ret= magnetEmailHeader(address);
			} else if ( keystr.startsWith("n") ) {
				fname = valstr;
			} else if ( keystr.startsWith("u") ) {
				sha1 = valstr;
				ret += magnetEmailDetail(address, fname, sha1, count);
                count++;
			}
		}
        ret += magnetEmailTrailer(count);
		return ret;
	}

    private String magnetDetail(String address, String fname, String sha1) {
        String ret =
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Name</b></td>"+
         "    <td bgcolor=\"#FFFFFF\" class=\"name\">"+fname+"</td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>SHA1</b></td>"+
         "    <td bgcolor=\"#ffffff\" class=\"text\">"+sha1+"</td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Link</b></td>"+
         "    <td bgcolor=\"#ffffff\" class=\"text\"><a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+
         fname+"</a></td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Magnet</b></td>"+
         "    <td bgcolor=\"#ffffff\"><textarea name=\"textarea\" cols=\"80\" rows=\"4\" wrap=\"VIRTUAL\" class=\"area\">magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"</textarea></td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Html link</b></td>"+
         "    <td bgcolor=\"#ffffff\"><textarea name=\"textarea\" cols=\"80\" rows=\"5\" wrap=\"VIRTUAL\" class=\"area\"><a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+fname+"</a></textarea></td>"+
         "  </tr>"+
         "  <tr bgcolor=\"#333333\"> "+
         "    <td colspan=\"2\" class=\"text\" height=\"5\"></td></tr>";

        return ret;
    }


    private static String magnetDetailPageHeader() {
       String ret= 
         "<html>"+
         "<head>"+
         "<title>LimeWire Magnet Descriptions</title>"+
         "<style type=\"text/css\">"+
         "<!--"+
         ".text {"+
         "    font-family: Verdana, Arial, Helvetica, sans-serif;"+
         "    font-size: 11px;"+
         "    color: #333333;"+
         "}"+
         ".header {"+
         "    font-family: Arial, Helvetica, sans-serif;"+
         "    font-size: 14pt;"+
         "    color: #ffffff;"+
         "}"+
         ".name {"+
         "    font-family: Verdana, Arial, Helvetica, sans-serif;"+
         "    font-size: 11px;"+
         "    font-weight: bold;"+
         "    color: #000000;"+
         "}"+
         ".area  { "+
         "border: 1px solid;"+
          "margin: 0;"+
          "padding: 4px;"+
          "background: #FFFEF4;"+
          "color: #333333;"+
          "font: 11px Verdana, Arial;"+
          "text-align: left;"+
         "}"+
         "-->"+
         "</style>"+
         "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">"+
         "</head>"+
         "<body bgcolor=\"#666666\">"+
         "<span class=\"header\"><center>"+
         "  LimeWire Magnet Details "+
         "</center></span><br>"+
         "<table border=\"0\" cellpadding=\"5\" cellspacing=\"1\" bgcolor=\"#999999\" align=\"center\">";

        return ret;
    }


    private String magnetEmailHeader(String address){
        return
          "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n"+
          "<html>\n"+
          "<head>\n"+
          "<title>LimeWire Send Magnet Form</title>\n"+
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\n"+
          "<script LANGUAGE=\"JavaScript1.2\" SRC=\"/magnet10/scripts.js\" TYPE=\"text/javascript\"></script>\n"+
          "<link href=\"/magnet10/style.css\" rel=\"stylesheet\" type=\"text/css\">\n"+
          "</head>\n"+
          "<body>\n"+
          "<form name=\"form1\" method=\"post\" action=\""+
          "http://email.limewire.com/cgi-bin/send_email.cgi"+
          "\" onsubmit=\"return Validator(this)\">\n"+
          "    <input name=\"address\" type=\"hidden\" value=\"\n"+address+"\">\n"+
          "    <table width=\"500\" border=\"0\" align=\"center\" cellpadding=\"5\" cellspacing=\"1\" bgcolor=\"#FFFFFF\" align=\"center\">" +
          "    <tr> \n"+
          "      <td colspan=\"2\">To EMAIL direct links to the selected files in your Shared Library, fill out and submit the form below.</td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td width=\"42\" align=\"right\"><b>To:</b></td>\n"+
          "      <td width=\"415\"> \n"+
          "        <input name=\"to_email\" type=\"text\" maxlength=\"100\">\n"+
          "         (required)</td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td align=\"right\"><b>CC:</b></td>\n"+
          "      <td> \n"+
          "        <input name=\"cc_email\" type=\"text\" maxlength=\"100\"></td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td align=\"right\"><b>From: </b></td>\n"+
          "      <td> \n"+
          "        <input name=\"from_email\" type=\"text\" maxlength=\"100\">\n"+
          "        (required)</td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td align=\"right\"><b>Subject: </b></td>\n"+
          "      <td> \n"+
          "        <input name=\"subject\" type=\"text\" maxlength=\"100\">\n"+
          "      </td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td>&nbsp;</td>\n"+
          "      <td>Message to send with links: <br> <textarea name=\"comment\" cols=\"40\" rows=\"5\" wrap=\"VIRTUAL\"></textarea> </td>\n"+
          "    </tr>\n"+
          "    <tr> \n"+
          "      <td colspan=\"2\" class=\"yellow2\"> LINKS TO BE MAILED:<br>\n"+
          "        <ol>";
    }


    private String magnetEmailDetail(String address, String fname, String sha1,
         int count) {
        return
          "          <li><span class=\"links\">"+
          "<a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+
          "&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+
          fname+"</a>"+
          "</span><br>\n"+
          "          <input name=\"f"+count+"\" type=\"hidden\" value=\""+fname+"\">\n"+
          "          <input name=\"s"+count+"\" type=\"hidden\" value=\""+sha1+"\">\n"+
          "            <br>\n"+
          "            Link Description:<br>\n"+
          "            <textarea name=\"desc"+count+"\" cols=\"40\" rows=\"2\" wrap=\"VIRTUAL\"></textarea>\n"+
          "            <hr size=\"1\">\n"+
          "          </li>\n";
    }


    private String magnetEmailTrailer(int count) {
        return
          "        </ol>\n"+
          "        <input name=\"count\" type=\"hidden\" value=\""+count+"\">"+
          "        <center><input name=\"Submit\" type=\"submit\" value=\"Submit and Send\"></center></td>\n"+
          "    </tr>";
    }

    private void returnNoContent(Socket socket) {
        try {
            
            BufferedOutputStream out =
              new BufferedOutputStream(socket.getOutputStream());
            String s = NOCONTENT;
            byte[] bytes=s.getBytes();
            out.write(bytes);
            out.flush();
            
            socket.close();
        } catch (IOException e) {
        }
    }
}

