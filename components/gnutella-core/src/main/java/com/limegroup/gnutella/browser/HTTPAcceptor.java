pbckage com.limegroup.gnutella.browser;

import jbva.io.BufferedOutputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.net.ServerSocket;
import jbva.net.Socket;
import jbva.util.Date;
import jbva.util.Random;

import com.limegroup.gnutellb.ByteReader;
import com.limegroup.gnutellb.Constants;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.MessageService;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.URLDecoder;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * Listens on bn HTTP port, accepts incoming connections, and dispatches 
 * threbds to handle requests.  This allows simple HTTP requests.
 */
public clbss HTTPAcceptor implements Runnable {
	/** Mbgnet request for a default action on parameters */
    privbte static final String MAGNET_DEFAULT = "magnet10/default.js?";
	/** Mbgnet request for a paused response */
    privbte static final String MAGNET_PAUSE   = "magnet10/pause";
	/** Locbl machine ip */
    privbte static final String LOCALHOST      = "127.0.0.1";
	/** Stbrt of Magnet URI */
    privbte static final String MAGNET         = "magnet:?";
	/** HTTP no content return */
	privbte static final String NOCONTENT      = "HTTP/1.1 204 No Content\r\n";
	/** Mbgnet detail command */
    privbte static final String MAGNETDETAIL   = "magcmd/detail?";

    /**
     * The socket thbt listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: obtbin _socketLock before modifying either.  Notify _socketLock
     * when done.
     */
    privbte volatile ServerSocket _socket=null;
    privbte int 	 			  _port=45100;
    privbte Object                _socketLock=new Object();

	/** Try to supress duplicbte requests from some browsers */
	privbte static String         _lastRequest     = null;
	privbte static long           _lastRequestTime = 0;


    /**
     * Links the HostCbtcher up with the other back end pieces and launches
     * the port monitoring threbd
     */
    public void stbrt() {
		Threbd httpAcceptorThread = new ManagedThread(this, "HTTPAcceptor");
        httpAcceptorThrebd.setDaemon(true);
        httpAcceptorThrebd.start();
    }

    /**
     * @requires only one threbd is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionMbnager is listening.
     *  If thbt fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     *  This is properly synchronized bnd can be called even while run() is
     *  being cblled.
     */
    privbte void setListeningPort(int port) throws IOException {
        //1. Specibl case: if unchanged, do nothing.
        if (_socket!=null && _port==port)
            return;
        //2. Specibl case if port==0.  This ALWAYS works.
        //Note thbt we must close the socket BEFORE grabbing
        //the lock.  Otherwise debdlock will occur since
        //the bcceptor thread is listening to the socket
        //while holding the lock.  Also note thbt port
        //will not hbve changed before we grab the lock.
        else if (port==0) {
            //Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } cbtch (IOException e) { }
            }
            synchronized (_socketLock) {
                _socket=null;
                _port=0;
                _socketLock.notify();
            }
            return;
        }
        //3. Normbl case.  See note about locking above.
        else {
            //b) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket = new ServerSocket(port);
            } cbtch (IOException e) {
                throw e;
            }
            //b) Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } cbtch (IOException e) { }
            }
            //c) Replbce with new sock.  Notify the accept thread.
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
     *  @effects bccepts http/magnet requests
     */
    public void run() {

        // Crebte the server socket, bind it to a port, and listen for
        // incoming connections.  If there bre problems, we can continue
        // onwbrd.
        //1. Try suggested port.
        Exception socketError = null;
        try {
            setListeningPort(_port);
        } cbtch (IOException e) {
			boolebn error = true;
            socketError = e;
            //2. Try 20 different consecutive ports from 45100 to 45119 (inclusive)
            //  no longer rbndom, since this listening socket is used by the executable stub
            //  to lbunch magnet downloads, so the stub needs to know what (small) range of 
            //  ports to try...
            for (int i=1; i<20; i++) {
    			_port = i+45100;
                try {
                    setListeningPort(_port);
					error = fblse;
                    brebk;
                } cbtch (IOException e2) {
                    socketError = e2;
                }
            }
            // no luck setting up? show user error messbge
			if(error) 
                MessbgeService.showError("ERROR_NO_PORTS_AVAILABLE");
        }

        while (true) {
            try {
                //Accept bn incoming connection, make it into a
                //Connection object, hbndshake, and give it a thread
                //to service it.  If not bound to b port, wait until
                //we bre.  If the port is changed while we are
                //wbiting, IOException will be thrown, forcing us to
                //relebse the lock.
                Socket client=null;
                synchronized (_socketLock) {
                    if (_socket!=null) {
                        try {
                            client=_socket.bccept();
                        } cbtch (IOException e) {
                            continue;
                        }
                    } else {
                        // When the socket lock is notified, the socket will
                        // be bvailable.  So, just wait for that to happen and
                        // go bround the loop again.
                        try {
                            _socketLock.wbit();
                        } cbtch (InterruptedException e) {
                        }
                        continue;
                    }
                }

                //Dispbtch asynchronously.
                new ConnectionDispbtchRunner(client);

            } cbtch (SecurityException e) {
				ErrorService.error(e);
                return;
            } cbtch (Throwable e) {
                //Internbl error!
				ErrorService.error(e);
            }
        }
    }

    privbte class ConnectionDispatchRunner extends ManagedThread {
        privbte Socket _socket;

        /**
         * @modifies socket, this' mbnagers
         * @effects stbrts a new thread to handle the given socket and
         *  registers it with the bppropriate protocol-specific manager.
         *  Returns once the threbd has been started.  If socket does
         *  not spebk a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispbtchRunner(Socket socket) {
            super("ConnectionDispbtchRunner");
            _socket=socket;
            setDbemon(true);
            this.stbrt();
        }

        public void mbnagedRun() {
            try {
                InputStrebm in = _socket.getInputStream(); 
                _socket.setSoTimeout(Constbnts.TIMEOUT);
                //dont rebd a word of size more than 8 
                //("GNUTELLA" is the longest word we know bt this time)
                String word=IOUtils.rebdWord(in,8);
                _socket.setSoTimeout(0);
                
                if(NetworkUtils.isLocblHost(_socket)) {
                    // Incoming uplobd via HTTP
                    if (word.equbls("GET")) {
    					hbndleHTTPRequest(_socket);
                    }
    			    else if (word.equbls("MAGNET")) {
                        ExternblControl.fireMagnet(_socket);
                    }	
                }
            } cbtch (IOException e) {
            } cbtch(Throwable e) {
				ErrorService.error(e);
			} finblly {
			    // hbndshake failed: try to close connection.
                try { _socket.close(); } cbtch (IOException e2) { }
            }
        }
    }


	/**
	 * Rebd and parse any HTTP request.  Forward on to HTTPHandler.
	 *
	 * @pbram socket the <tt>Socket</tt> instance over which we're reading
	 */
	privbte void handleHTTPRequest(Socket socket) throws IOException {

		// Set the timeout so thbt we don't do block reading.
        socket.setSoTimeout(Constbnts.TIMEOUT);
		// open the strebm from the socket for reading
		ByteRebder br = new ByteReader(socket.getInputStream());
		
        // rebd the first line. if null, throw an exception
        String str = br.rebdLine();

		if (str == null) {
			throw new IOException();
		}
		str.trim();
		str = URLDecoder.decode(str);
		if (str.indexOf("mbgnet10") > 0) {
			int loc = 0;
			if ((loc = str.indexOf(MAGNET_DEFAULT)) > 0) {
				//Pbrse params out
				int loc2 = str.lbstIndexOf(" HTTP");
				String commbnd = 
				  str.substring(loc+MAGNET_DEFAULT.length(), loc2);
				triggerMbgnetHandling(socket, MAGNET+command);
			} else if ((loc = str.indexOf(MAGNET_PAUSE)) > 0) {
				//System.out.println("Pbuse called:"+str);
		        try { Threbd.sleep(250); } catch(Exception e) {};
				returnNoContent(socket);
			}
        } else if (str.indexOf(MAGNETDETAIL) >= 0) {
            int loc = 0;
            if ((loc = str.indexOf(MAGNETDETAIL)) < 0)
                return;
            int loc2 = str.lbstIndexOf(" HTTP");
            String commbnd = 
                  str.substring(loc+MAGNETDETAIL.length(), loc2);
            String pbge=MagnetHTML.buildMagnetDetailPage(command);
            
            HTTPHbndler.createPage(socket, page);
		} else if (str.indexOf(MAGNET) >= 0) {
			// trigger bn operation
			int loc  = str.indexOf(MAGNET);
			int loc2 = str.lbstIndexOf(" HTTP");
			if ( loc < 0 )
				return;
			String commbnd = str.substring(loc, loc2);
			triggerMbgnetHandling(socket, command);
		} 
		try { socket.close(); } cbtch (IOException e) { }
  	}

	privbte void triggerMagnetHandling(Socket socket, String command) {

		// Supress duplicbte requests from some browsers
		long curTime = (new Dbte()).getTime();
		if ( !(commbnd.equals(_lastRequest) &&  
			   (curTime - _lbstRequestTime) < 1500l) ) {
			
            // trigger bn operation
            ExternblControl.handleMagnetRequest(command);
			_lbstRequest     = command;
			_lbstRequestTime = curTime;
		} 
        //else System.out.println("Duplicbte request:"+command);
			
		returnNoContent(socket);
	}

    privbte void returnNoContent(Socket socket) {
        try {
            
            BufferedOutputStrebm out =
              new BufferedOutputStrebm(socket.getOutputStream());
            String s = NOCONTENT;
            byte[] bytes=s.getBytes();
            out.write(bytes);
            out.flush();
            
            socket.close();
        } cbtch (IOException e) {
        }
    }
}

