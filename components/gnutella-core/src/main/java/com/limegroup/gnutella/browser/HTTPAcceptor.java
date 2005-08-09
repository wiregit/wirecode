package com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Random;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.MessageService;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.URLDecoder;
import com.limegroup.gnutella.util.NetworkUtils;

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
		Thread httpAcceptorThread = new ManagedThread(this, "HTTPAcceptor");
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
                newSocket = new ServerSocket(port);
            } catch (IOException e) {
                throw e;
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
            //2. Try 20 different consecutive ports from 45100 to 45119 (inclusive)
            //  no longer random, since this listening socket is used by the executable stub
            //  to launch magnet downloads, so the stub needs to know what (small) range of 
            //  ports to try...
            for (int i=1; i<20; i++) {
    			_port = i+45100;
                try {
                    setListeningPort(_port);
					error = false;
                    break;
                } catch (IOException e2) {
                    socketError = e2;
                }
            }
            // no luck setting up? show user error message
			if(error) 
                MessageService.showError("ERROR_NO_PORTS_AVAILABLE");
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
            } catch (Throwable e) {
                //Internal error!
				ErrorService.error(e);
            }
        }
    }

    private class ConnectionDispatchRunner extends ManagedThread {
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

        public void managedRun() {
            try {
                InputStream in = _socket.getInputStream(); 
                _socket.setSoTimeout(Constants.TIMEOUT);
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word=IOUtils.readWord(in,8);
                _socket.setSoTimeout(0);
                
                if(NetworkUtils.isLocalHost(_socket)) {
                    // Incoming upload via HTTP
                    if (word.equals("GET")) {
    					handleHTTPRequest(_socket);
                    }
    			    else if (word.equals("MAGNET")) {
                        ExternalControl.fireMagnet(_socket);
                    }	
                }
            } catch (IOException e) {
            } catch(Throwable e) {
				ErrorService.error(e);
			} finally {
			    // handshake failed: try to close connection.
                try { _socket.close(); } catch (IOException e2) { }
            }
        }
    }


	/**
	 * Read and parse any HTTP request.  Forward on to HTTPHandler.
	 *
	 * @param socket the <tt>Socket</tt> instance over which we're reading
	 */
	private void handleHTTPRequest(Socket socket) throws IOException {

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
			}
        } else if (str.indexOf(MAGNETDETAIL) >= 0) {
            int loc = 0;
            if ((loc = str.indexOf(MAGNETDETAIL)) < 0)
                return;
            int loc2 = str.lastIndexOf(" HTTP");
            String command = 
                  str.substring(loc+MAGNETDETAIL.length(), loc2);
            String page=MagnetHTML.buildMagnetDetailPage(command);
            
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

