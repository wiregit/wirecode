package com.limegroup.gnutella.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * Provides socket operations that are not available on all platforms,
 * like connecting with timeouts and settings the SO_KEEPALIVE option.
 * Obsoletes the old SocketOpener class.
 */
public class Sockets {
    
    /**
     * The maximum number of concurrent connection attempts.
     */
    private static final int MAX_CONNECTING_SOCKETS = 8;
    
    /**
     * The current number of waiting socket attempts.
     */
    private static int _socketsConnecting = 0;
    

    private static volatile int _attempts=0;
	/**
	 * Ensure this cannot be constructed.
	 */
	private Sockets() {}

    /**
     * Sets the SO_KEEPALIVE option on the socket, if this platform supports it.
     * (Otherwise, it does nothing.)  
     *
     * @param socket the socket to modify
     * @param on the desired new value for SO_KEEPALIVE
     * @return true if this was able to set SO_KEEPALIVE
     */
    public static boolean setKeepAlive(Socket socket, boolean on) {
        try {
            socket.setKeepAlive(on);
            return true;
        } catch(SocketException se) {
            return false;
        }
    }

    /**
     * Connects and returns a socket to the given host, with a timeout.
     *
     * @param host the address of the host to connect to
     * @param port the port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
	 *  or 0 for no timeout. In case of a proxy connection, this timeout
	 *  might be exceeded
     * @return the connected Socket
     * @throws IOException the connections couldn't be made in the 
     *  requested time
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(String host, int port, int timeout) 
		throws IOException {
        if(!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("port out of range: "+port);
        }

		// if the user specified that he wanted to use a proxy to connect
		// to the network, we will use that proxy unless the host we
		// want to connect to is a private address
		int connectionType = ConnectionSettings.CONNECTION_METHOD.getValue();
		if (connectionType != ConnectionSettings.C_NO_PROXY) {
			InetAddress address = null;
			try {
				address = InetAddress.getByName(host);
			} catch (UnknownHostException e) {
				throw new IOException();
			}
			if (!NetworkUtils.isPrivateAddress(address)
				|| ConnectionSettings.USE_PROXY_FOR_PRIVATE.getValue()) {
				if (connectionType == ConnectionSettings.C_HTTP_PROXY)
					return connectHTTP(host, port, timeout);
				else if (connectionType == ConnectionSettings.C_SOCKS4_PROXY)
					return connectSocksV4(host, port, timeout);
				else if (connectionType == ConnectionSettings.C_SOCKS5_PROXY)
					return connectSocksV5(host, port, timeout);
			}
		}
		_attempts++;
		return connectPlain(host, port, timeout);
	}

	/** 
	 * connect to a host directly
	 * @see connect(String, int, int)
	 */
	private static Socket connectPlain(String host, int port, int timeout)
		throws IOException {
        
        waitForSocket();
		    
        try {
            if (CommonUtils.isJava14OrLater())
                //a) 1.4-style sockets
                return Sockets14.getSocket(host, port, timeout);
            else if (timeout!=0)
                //b) Emulation using threads
                return (new SocketOpener(host, port)).connect(timeout);
            else
                //c) No timeouts
                return new Socket(host, port);
        } finally {
            releaseSocket();
        }
    }

	/** 
	 * connect to a host using a socks v4 proxy
	 * @see connect(String, int, int)
	 */
	private static Socket connectSocksV4(String host, int port, int timeout)
		throws IOException {
		byte[] hostBytes;
		try {
			hostBytes = InetAddress.getByName(host).getAddress();
		} catch (UnknownHostException e) {
			throw new IOException("invalid host name");
		}

		byte[] portBytes = new byte[2];
		portBytes[0] = ((byte) (port >> 8));
		portBytes[1] = ((byte) port);

		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();
		OutputStream os = null;
		InputStream in = null;

		Socket proxySocket = connectPlain(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout);
		os = proxySocket.getOutputStream();
		in = proxySocket.getInputStream();

		os.write(0x04); //version
		os.write(0x01); //connect command
		os.write(portBytes); //port to connect to
		os.write(hostBytes); //host to connect to
		//write user name if necessary
		if (ConnectionSettings.PROXY_AUTHENTICATE.getValue())
			os.write(ConnectionSettings.PROXY_USERNAME.getValue().getBytes());
		os.write(0x00); //terminating 0
		os.flush();

		// read response
		// version should be 0 but some socks proxys answer 4
		int version = in.read();
		if (version != 0x00 && version != 0x04) {
			proxySocket.close();
			throw new IOException(
				"Invalid version from socks proxy: "
					+ version
					+ " expected 0 or 4");
		}

		// read the status, 0x5A is success
		int status = in.read();
		if (status != 0x5A) {
			proxySocket.close();
			throw new IOException("Request rejected with status: " + status);
		}

		// the socks proxy will now send the connected port and host
		// we don't really check if it's the right one.
		byte[] connectedHostPort = new byte[2];
		byte[] connectedHostAddress = new byte[4];
		if (in.read(connectedHostPort) == -1
			|| in.read(connectedHostAddress) == -1) {
			proxySocket.close();
			throw new IOException("Connection failed");
		}
		proxySocket.setSoTimeout(0);
		return proxySocket;
	}

	/** 
	 * connect to a host using a socks v5 proxy
	 * @see connect(String, int, int)
	 */
	private static Socket connectSocksV5(String host, int port, int timeout)
		throws IOException {
		byte[] hostBytes;
		try {
			hostBytes = InetAddress.getByName(host).getAddress();
		} catch (UnknownHostException e) {
			throw new IOException("invalid host name");
		}

		byte[] portBytes = new byte[2];
		portBytes[0] = ((byte) (port >> 8));
		portBytes[1] = ((byte) port);

		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();
		OutputStream os = null;
		InputStream in = null;
		Socket proxySocket;

		proxySocket = connectPlain(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout);
		os = proxySocket.getOutputStream();
		in = proxySocket.getInputStream();

		os.write(0x05); //version
		if (ConnectionSettings.PROXY_AUTHENTICATE.getValue()) {
			os.write(0x02); //the number of authentication methods we support
			os.write(0x00); //authentication method: no authentication
			os.write(0x02); //authentication method: username/password
		} else {
			os.write(0x01); //the number of authentication methods we support
			os.write(0x00); //authentication method: no authentication
		}
		os.flush();

		int version = in.read();
		if (version != 0x05) {
			proxySocket.close();
			throw new IOException(
				"Invalid version from socks proxy: " + version + " expected 5");
		}

		int auth_method = in.read();
		if (auth_method == 0x00) {
			// no authentication
		} else if (auth_method == 0x02) {
			// username/password
			String username = ConnectionSettings.PROXY_USERNAME.getValue();
			String password = ConnectionSettings.PROXY_PASS.getValue();

			os.write(0x01); // version of authentication protocol
			os.write((byte) username.length()); // length of username field
			os.write(username.getBytes()); // username
			os.write((byte) password.length()); // length of password field
			os.write(password.getBytes()); // password
			os.flush();

			// read version for auth protocol from proxy, expects 1
			version = in.read();
			if (version != 0x01) {
				proxySocket.close();
				throw new IOException(
					"Invalid version for authentication: "
						+ version
						+ " expected 1");
			}

			// read status, 0 is success
			int status = in.read();
			if (status != 0x00) {
				proxySocket.close();
				throw new IOException(
					"Authentication failed with status: " + status);
			}
		}

		// request connection
		os.write(0x05); // version again
		os.write(0x01); // connect command, 
		// 0x02 would be bind, 0x03 UDP associate
		os.write(0x00); // reserved field, must be 0x00
		os.write(0x01); // address type: 0x01 is IPv4, 0x04 would be IPv6
		os.write(hostBytes); //host to connect to
		os.write(portBytes); //port to connect to
		os.flush();

		// read response
		// version should be 0x05
		version = in.read();
		if (version != 0x05) {
			proxySocket.close();
			throw new IOException(
				"Invalid version from socks proxy: " + version + " expected 5");
		}
		// read the status, 0x00 is success
		int status = in.read();
		if (status != 0x00) {
			proxySocket.close();
			throw new IOException("Request rejected with status: " + status);
		}

		// skip reserved byte;
		in.read();

		// read the address type in the reply and skip it.
		int addrType = in.read();
		int bytesToSkip = 0;
		if (addrType == 1) { // IPv4
			bytesToSkip = 4 + 2;
		} else if (addrType == 3) { // domain name
			bytesToSkip = in.read() + 2;
		} else if (addrType == 4) { // IPv6
			bytesToSkip = 16 + 2;
		}

		for (int i = 0; i < bytesToSkip; i++) {
			if (in.read() == -1) {
				throw new IOException("Connection failed");
			}
		}

		proxySocket.setSoTimeout(0);
		return proxySocket;
	}

	/** 
	 * connect to a host using a http proxy
	 * @see connect(String, int, int)
	 */
	private static Socket connectHTTP(String host, int port, int timeout)
		throws IOException {
		String connectString =
			"CONNECT " + host + ":" + port + " HTTP/1.0\r\n\r\n";

		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();

		OutputStream os = null;
		InputStream in = null;
		Socket proxySocket;

		proxySocket = connectPlain(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout);
		os = proxySocket.getOutputStream();
		in = proxySocket.getInputStream();

		// write connection string
		os.write(connectString.getBytes());
		os.flush();

		// read response;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		String line = reader.readLine();
        
		// look for code 200
		if (line==null || line.indexOf("200") == -1)
			throw new IOException("HTTP connection failed");

		// skip the rest of the response
		while (!line.equals("")) {
			line = reader.readLine();
            if(line == null)
                throw new IOException("end of stream");
		}

		// we should be connected now
		proxySocket.setSoTimeout(0);
		return proxySocket;
	}

	public static int getAttempts() {
	    return _attempts;
	}
	
	public static void clearAttempts() {
	    _attempts=0;
	}
	
	/**
	 * Waits until we're allowed to do an active outgoing socket
	 * connection.
	 */
	private static void waitForSocket() throws IOException {
	    if(!CommonUtils.isWindowsXP())
	        return;
	    synchronized(Sockets.class) {
	        while(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
	            try {
	                Sockets.class.wait();
	            } catch(InterruptedException ignored) {
	                throw new IOException(ignored.getMessage());
	            }
	        }
	        _socketsConnecting++;	        
	    }
	}
	
	/**
	 * Notification that a socket has been released.
	 */
	private static void releaseSocket() {
	    if(!CommonUtils.isWindowsXP())
	        return;
	    synchronized(Sockets.class) {
	        _socketsConnecting--;
	        Sockets.class.notifyAll();
	    }
	}   
	
	/** 
	 * Opens Java sockets with a bounded timeout using threads.  Typical use:
	 *
	 * <pre>
	 *    try {
	 *        Socket socket=(new SocketOpener(host, port)).connect(timeout);
	 *    } catch (IOException e) {
	 *        System.out.println("Couldn't connect in time.");
	 *    }
	 * </pre>
	 *
	 * This is basically just a hack to work around JDK bug 4110694.  It is
	 * implemented in a similar way as Wayne Conrad's SocketOpener class, using
	 * Thread.stop().  This is necessary because of bugs in earlier Java 
	 * implementations, where certain sockets fail to die.
	 * It is imperitive that interrupt is used instead of stop, or a thread
	 * in the middle of loading the native socket library could be killed,
	 * causing NoClassDefFoundErrors to pop up in every thread attempting
	 * to create a Socket.
	 *
	 * For an outrageous listing of large amounts of SocketOpener threads
	 * left open, see the following bug reports:
	 *
	 * at http://www9.limewire.com:82/dev/exceptions/3.3.5/
	 *          java.lang.OutOfMemoryError/start4794.txt    (1407 threads)
	 *          java.io.FileNotFoundException/open24829.txt (177 threads)
     *          java.io.FileNotFoundException/open24960.txt (168 threads)
	 *          java.lang.OutOfMemoryError/start3462.txt    (45 threads)
	 *          java.lang.OutOfMemoryError/err32041.txt     (56 threads)
	 *          java.lang.OutOfMemoryError/err3183.txt      (29 threads)
	 * etc..
	 *
	 * This class is currently NOT thread safe.  Currently connect() can only be 
	 * called once.
	 */
	private static class SocketOpener {
		private String host;
		private int port;
		/** The established socket, or null if not established OR couldn't be
		 *  established.. Notify this when socket becomes non-null. */
		private Socket socket=null;
		/** True iff the connecting thread should close the socket if/when it
		 *  is established. */
		private boolean timedOut=false;
		private boolean completed=false;
		
		public SocketOpener(String host, int port) {
			if((port & 0xFFFF0000) != 0) {
				throw new IllegalArgumentException("port out of range: "+port);
			} 
			this.host=host;
			this.port=port;
		}
		
		/** 
		 * Returns a new socket to the given host/port.  If the socket couldn't be
		 * established withing timeout milliseconds, throws IOException.  If
		 * timeout==0, no timeout occurs.  If this thread is interrupted while
		 * making connection, throws IOException.
		 *
		 * @requires connect has only been called once, no other thread calling
		 *  connect.  Timeout must be non-negative.  
		 */
		public synchronized Socket connect(int timeout) 
            throws IOException {
			//Asynchronously establish socket.
			Thread t = new ManagedThread(new SocketOpenerThread(), "SocketOpener");
			t.setDaemon(true);
			t.start();
			
			//Wait for socket to be established, or for timeout.
			try {
				this.wait(timeout);
			} catch (InterruptedException e) {
				if (socket==null)
					timedOut=true;
				else
					try { socket.close(); } catch (IOException e2) { }
				throw new IOException();
			}
			// Ensure that the SocketOpener is killed.
            if( !completed )
			    t.interrupt();
			
			//a) Normal case
			if (socket!=null) {
				return socket;
			} 
			//b) Timeout case
			else {            
				timedOut=true;
				throw new IOException();
			}            
		}
		
		private class SocketOpenerThread implements Runnable {
			public void run() {
			    Socket sock = null;
				try {
					try {
						sock=new Socket(host, port);
					} catch (IOException e) { }                
					
					synchronized (SocketOpener.this) {
					    completed = true;
						if (timedOut && sock!=null)
							try { sock.close(); } catch (IOException e) { }
						else {
							socket=sock;   //may be null
							SocketOpener.this.notify();
						}
					}
                } catch(Throwable t) {
                    //We actively call Thread.interrupt() on this thread,
                    //and we've received reports of the Socket constructor
                    //throwing InterruptedException.
                    //(See: http://www9.limewire.com:82/dev/exceptions/3.4.4/
                    //         java.lang.InterruptedException/Socket.19534.txt)
                    //However, nothing declares it to be thrown, so we can't
                    //catch and discard seperately.
                    //As a workaround, we only error if t is not an
                    //instanceof InterruptedException.
                    if(!(t instanceof InterruptedException))
                        ErrorService.error(t);
				}
			}
		}
	}
}
