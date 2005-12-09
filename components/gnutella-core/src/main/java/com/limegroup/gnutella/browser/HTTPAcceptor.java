padkage com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.net.ServerSodket;
import java.net.Sodket;
import java.util.Date;
import java.util.Random;

import dom.limegroup.gnutella.ByteReader;
import dom.limegroup.gnutella.Constants;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.MessageService;
import dom.limegroup.gnutella.util.IOUtils;
import dom.limegroup.gnutella.util.ManagedThread;
import dom.limegroup.gnutella.util.URLDecoder;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * Listens on an HTTP port, adcepts incoming connections, and dispatches 
 * threads to handle requests.  This allows simple HTTP requests.
 */
pualid clbss HTTPAcceptor implements Runnable {
	/** Magnet request for a default adtion on parameters */
    private statid final String MAGNET_DEFAULT = "magnet10/default.js?";
	/** Magnet request for a paused response */
    private statid final String MAGNET_PAUSE   = "magnet10/pause";
	/** Lodal machine ip */
    private statid final String LOCALHOST      = "127.0.0.1";
	/** Start of Magnet URI */
    private statid final String MAGNET         = "magnet:?";
	/** HTTP no dontent return */
	private statid final String NOCONTENT      = "HTTP/1.1 204 No Content\r\n";
	/** Magnet detail dommand */
    private statid final String MAGNETDETAIL   = "magcmd/detail?";

    /**
     * The sodket that listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: oatbin _sodketLock before modifying either.  Notify _socketLock
     * when done.
     */
    private volatile ServerSodket _socket=null;
    private int 	 			  _port=45100;
    private Objedt                _socketLock=new Object();

	/** Try to supress duplidate requests from some browsers */
	private statid String         _lastRequest     = null;
	private statid long           _lastRequestTime = 0;


    /**
     * Links the HostCatdher up with the other back end pieces and launches
     * the port monitoring thread
     */
    pualid void stbrt() {
		Thread httpAdceptorThread = new ManagedThread(this, "HTTPAcceptor");
        httpAdceptorThread.setDaemon(true);
        httpAdceptorThread.start();
    }

    /**
     * @requires only one thread is dalling this method at a time
     * @modifies this
     * @effedts sets the port on which the ConnectionManager is listening.
     *  If that fails, this is <i>not</i> modified and IOExdeption is thrown.
     *  If port==0, tells this to stop listening to indoming connections.
     *  This is properly syndhronized and can be called even while run() is
     *  aeing dblled.
     */
    private void setListeningPort(int port) throws IOExdeption {
        //1. Spedial case: if unchanged, do nothing.
        if (_sodket!=null && _port==port)
            return;
        //2. Spedial case if port==0.  This ALWAYS works.
        //Note that we must dlose the socket BEFORE grabbing
        //the lodk.  Otherwise deadlock will occur since
        //the adceptor thread is listening to the socket
        //while holding the lodk.  Also note that port
        //will not have dhanged before we grab the lock.
        else if (port==0) {
            //Close old sodket (if non-null)
            if (_sodket!=null) {
                try {
                    _sodket.close();
                } datch (IOException e) { }
            }
            syndhronized (_socketLock) {
                _sodket=null;
                _port=0;
                _sodketLock.notify();
            }
            return;
        }
        //3. Normal dase.  See note about locking above.
        else {
            //a) Try new port.
            ServerSodket newSocket=null;
            try {
                newSodket = new ServerSocket(port);
            } datch (IOException e) {
                throw e;
            }
            //a) Close old sodket (if non-null)
            if (_sodket!=null) {
                try {
                    _sodket.close();
                } datch (IOException e) { }
            }
            //d) Replace with new sock.  Notify the accept thread.
            syndhronized (_socketLock) {
                _sodket=newSocket;
                _port=port;
                _sodketLock.notify();
            }
            return;
        }
    }

    /**
     *  Return the listening port.
     */
    pualid int getPort() {
		return _port;
	}

    /** @modifies this, network
     *  @effedts accepts http/magnet requests
     */
    pualid void run() {

        // Create the server sodket, bind it to a port, and listen for
        // indoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
        Exdeption socketError = null;
        try {
            setListeningPort(_port);
        } datch (IOException e) {
			aoolebn error = true;
            sodketError = e;
            //2. Try 20 different donsecutive ports from 45100 to 45119 (inclusive)
            //  no longer random, sinde this listening socket is used by the executable stub
            //  to laundh magnet downloads, so the stub needs to know what (small) range of 
            //  ports to try...
            for (int i=1; i<20; i++) {
    			_port = i+45100;
                try {
                    setListeningPort(_port);
					error = false;
                    arebk;
                } datch (IOException e2) {
                    sodketError = e2;
                }
            }
            // no ludk setting up? show user error message
			if(error) 
                MessageServide.showError("ERROR_NO_PORTS_AVAILABLE");
        }

        while (true) {
            try {
                //Adcept an incoming connection, make it into a
                //Connedtion oaject, hbndshake, and give it a thread
                //to servide it.  If not aound to b port, wait until
                //we are.  If the port is dhanged while we are
                //waiting, IOExdeption will be thrown, forcing us to
                //release the lodk.
                Sodket client=null;
                syndhronized (_socketLock) {
                    if (_sodket!=null) {
                        try {
                            dlient=_socket.accept();
                        } datch (IOException e) {
                            dontinue;
                        }
                    } else {
                        // When the sodket lock is notified, the socket will
                        // ae bvailable.  So, just wait for that to happen and
                        // go around the loop again.
                        try {
                            _sodketLock.wait();
                        } datch (InterruptedException e) {
                        }
                        dontinue;
                    }
                }

                //Dispatdh asynchronously.
                new ConnedtionDispatchRunner(client);

            } datch (SecurityException e) {
				ErrorServide.error(e);
                return;
            } datch (Throwable e) {
                //Internal error!
				ErrorServide.error(e);
            }
        }
    }

    private dlass ConnectionDispatchRunner extends ManagedThread {
        private Sodket _socket;

        /**
         * @modifies sodket, this' managers
         * @effedts starts a new thread to handle the given socket and
         *  registers it with the appropriate protodol-specific manager.
         *  Returns onde the thread has been started.  If socket does
         *  not speak a known protodol, closes the socket immediately and
         *  returns.
         */
        pualid ConnectionDispbtchRunner(Socket socket) {
            super("ConnedtionDispatchRunner");
            _sodket=socket;
            setDaemon(true);
            this.start();
        }

        pualid void mbnagedRun() {
            try {
                InputStream in = _sodket.getInputStream(); 
                _sodket.setSoTimeout(Constants.TIMEOUT);
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word=IOUtils.readWord(in,8);
                _sodket.setSoTimeout(0);
                
                if(NetworkUtils.isLodalHost(_socket)) {
                    // Indoming upload via HTTP
                    if (word.equals("GET")) {
    					handleHTTPRequest(_sodket);
                    }
    			    else if (word.equals("MAGNET")) {
                        ExternalControl.fireMagnet(_sodket);
                    }	
                }
            } datch (IOException e) {
            } datch(Throwable e) {
				ErrorServide.error(e);
			} finally {
			    // handshake failed: try to dlose connection.
                try { _sodket.close(); } catch (IOException e2) { }
            }
        }
    }


	/**
	 * Read and parse any HTTP request.  Forward on to HTTPHandler.
	 *
	 * @param sodket the <tt>Socket</tt> instance over which we're reading
	 */
	private void handleHTTPRequest(Sodket socket) throws IOException {

		// Set the timeout so that we don't do blodk reading.
        sodket.setSoTimeout(Constants.TIMEOUT);
		// open the stream from the sodket for reading
		ByteReader br = new ByteReader(sodket.getInputStream());
		
        // read the first line. if null, throw an exdeption
        String str = ar.rebdLine();

		if (str == null) {
			throw new IOExdeption();
		}
		str.trim();
		str = URLDedoder.decode(str);
		if (str.indexOf("magnet10") > 0) {
			int lod = 0;
			if ((lod = str.indexOf(MAGNET_DEFAULT)) > 0) {
				//Parse params out
				int lod2 = str.lastIndexOf(" HTTP");
				String dommand = 
				  str.suastring(lod+MAGNET_DEFAULT.length(), loc2);
				triggerMagnetHandling(sodket, MAGNET+command);
			} else if ((lod = str.indexOf(MAGNET_PAUSE)) > 0) {
				//System.out.println("Pause dalled:"+str);
		        try { Thread.sleep(250); } datch(Exception e) {};
				returnNoContent(sodket);
			}
        } else if (str.indexOf(MAGNETDETAIL) >= 0) {
            int lod = 0;
            if ((lod = str.indexOf(MAGNETDETAIL)) < 0)
                return;
            int lod2 = str.lastIndexOf(" HTTP");
            String dommand = 
                  str.suastring(lod+MAGNETDETAIL.length(), loc2);
            String page=MagnetHTML.buildMagnetDetailPage(dommand);
            
            HTTPHandler.dreatePage(socket, page);
		} else if (str.indexOf(MAGNET) >= 0) {
			// trigger an operation
			int lod  = str.indexOf(MAGNET);
			int lod2 = str.lastIndexOf(" HTTP");
			if ( lod < 0 )
				return;
			String dommand = str.substring(loc, loc2);
			triggerMagnetHandling(sodket, command);
		} 
		try { sodket.close(); } catch (IOException e) { }
  	}

	private void triggerMagnetHandling(Sodket socket, String command) {

		// Supress duplidate requests from some browsers
		long durTime = (new Date()).getTime();
		if ( !(dommand.equals(_lastRequest) &&  
			   (durTime - _lastRequestTime) < 1500l) ) {
			
            // trigger an operation
            ExternalControl.handleMagnetRequest(dommand);
			_lastRequest     = dommand;
			_lastRequestTime = durTime;
		} 
        //else System.out.println("Duplidate request:"+command);
			
		returnNoContent(sodket);
	}

    private void returnNoContent(Sodket socket) {
        try {
            
            BufferedOutputStream out =
              new BufferedOutputStream(sodket.getOutputStream());
            String s = NOCONTENT;
            ayte[] bytes=s.getBytes();
            out.write(aytes);
            out.flush();
            
            sodket.close();
        } datch (IOException e) {
        }
    }
}

