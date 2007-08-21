package com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.nio.SocketFactory;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.service.MessageService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.I18n;
import com.limegroup.gnutella.util.URLDecoder;

/**
 * Listens on an HTTP port, accepts incoming connections, and dispatches 
 * threads to handle requests.  This allows simple HTTP requests.
 */
@Singleton
public class HTTPAcceptor {
	/** Magnet request for a default action on parameters */
    private static final String MAGNET_DEFAULT = "magnet10/default.js?";
	/** Magnet request for a paused response */
    private static final String MAGNET_PAUSE   = "magnet10/pause";
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

	/** Try to supress duplicate requests from some browsers */
	private static String         _lastRequest     = null;
	private static long           _lastRequestTime = 0;

	private final Provider<ExternalControl> externalControl;

	@Inject
    public HTTPAcceptor(Provider<ExternalControl> externalControl) {
        this.externalControl = externalControl;
    }

    /**
     * Starts listening to incoming connections.
     */
    public void start() {
        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
        try {
            setListeningPort(_port);
        } catch (IOException e) {
            boolean error = true;
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
                } catch (IOException ignored) {}
            }
            // no luck setting up? show user error message
            if(error) 
                MessageService.showError(I18n.marktr("LimeWire was unable to set up a port to listen for incoming connections. Some features of LimeWire may not work as expected."));
        }
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
        if (_socket!=null && _port==port) {
            return;
        }
        //2. Special case if port==0.  This ALWAYS works.
        //Note that we must close the socket BEFORE grabbing
        //the lock.  Otherwise deadlock will occur since
        //the acceptor thread is listening to the socket
        //while holding the lock.  Also note that port
        //will not have changed before we grab the lock.
        else if (port == 0) {
            IOUtils.close(_socket);
            _socket = null;
            _port = 0;
            return;
        }
        //3. Normal case.  See note about locking above.
        else {
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket = SocketFactory.newServerSocket(port, new SocketListener());
            } catch (IOException e) {
                throw e;
            }
            
            //b) Close old socket
            IOUtils.close(_socket);
            
            //c) Replace with new sock.
            _socket=newSocket;
            _port=port;
            return;
        }
    }

    /**
     *  Return the listening port.
     */
    public int getPort() {
		return _port;
	}
    
    /** Dispatches sockets to a thread that'll handle them. */
    private class SocketListener implements AcceptObserver {
        public void handleIOException(IOException ignored) {}
        public void shutdown() {}
        
        public void handleAccept(Socket client) {
            if(NetworkUtils.isLocalHost(client)) {                
                // Dispatch asynchronously.
                ThreadExecutor.startThread(new ConnectionDispatchRunner(client), "ConnectionDispatchRunner");
            } else {
                IOUtils.close(client);
            }
        }
    }
    
    private class ConnectionDispatchRunner implements Runnable {
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
            _socket=socket;
        }

        public void run() {
            try {
                InputStream in = _socket.getInputStream(); 
                _socket.setSoTimeout(Constants.TIMEOUT);
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word=IOUtils.readWord(in,8);
                _socket.setSoTimeout(0);
                System.out.println("word:" + word);
                if (word.equals("GET")) {
                    handleHTTPRequest(_socket);
                } else if (word.equals("MAGNET")) {
                    externalControl.get().fireControlThread(_socket, true);
                } else if (word.equals("TORRENT")) {
                    externalControl.get().fireControlThread(_socket, false);
                }
            } catch (IOException e) {
            } finally {
                IOUtils.close(_socket);
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
		    externalControl.get().handleMagnetRequest(command);
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

