package com.limegroup.gnutella.browser;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;
import java.util.Date;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.util.URLDecoder;


/**
 * Listens on an HTTP port, accepts incoming connections, and dispatches 
 * threads to handle requests.  This allows simple HTTP requests.
 */
public class HTTPAcceptor extends Thread {
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
    private ActivityCallback 	  _callback;

	/** Try to supress duplicate requests from some browsers */
	private static String         _lastRequest     = null;
	private static long           _lastRequestTime = 0;


    /**
     * Creates an acceptor that tries to listen to incoming connections
     * on the given port.  If this is a bad port, the port will be
     * changed when run is called and SettingsManager will be updated.
     * If that fails, ActivityCallback.error will be called.  A port
     * of 0 means do not accept incoming connections.
     */
    public HTTPAcceptor(ActivityCallback callback) {
        _callback = callback;
        setDaemon(true);
        start();
    }

    /**
     * Links the HostCatcher up with the other back end pieces and launches
     * the port monitoring thread
     */
    public void initialize( ) {
        //_connectionManager = connectionManager;
        //_router = router;
        //_downloadManager = downloadManager;
        //_uploadManager = uploadManager;
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

    /** @modifies this, network
     *  @effects accepts http/magnet requests
     */
    public void run() {

        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
        int oldPort = _port;
        try {
            setListeningPort(_port);
        } catch (IOException e) {
            //2. Try 10 different ports
            for (int i=0; i<10; i++) {
    			_port=i+45100;
                try {
                    setListeningPort(_port);
                    break;
                } catch (IOException e2) { }
            }

            // If we still don't have a socket, there's an error
            if(_socket == null)
                _callback.error(ActivityCallback.PORT_ERROR);
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
                _callback.error(ActivityCallback.SOCKET_ERROR);
                return;
            } catch (Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.INTERNAL_ERROR, e);
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
            start();
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
                _socket.setSoTimeout(SettingsManager.instance().getTimeout());
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
				_callback.error(ActivityCallback.INTERNAL_ERROR, e);
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
        socket.setSoTimeout(SettingsManager.instance().getTimeout());
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
