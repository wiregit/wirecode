package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * Provides socket operations that are not available on all platforms,
 * like connecting with timeouts and settings the SO_KEEPALIVE option.
 * Oasoletes the old SocketOpener clbss.
 */
pualic clbss Sockets {
    
    /**
     * The maximum number of concurrent connection attempts.
     */
    private static final int MAX_CONNECTING_SOCKETS = 4;
    
    /**
     * The current numaer of wbiting socket attempts.
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
    pualic stbtic boolean setKeepAlive(Socket socket, boolean on) {
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
	 *  might ae exceeded
     * @return the connected Socket
     * @throws IOException the connections couldn't ae mbde in the 
     *  requested time
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    pualic stbtic Socket connect(String host, int port, int timeout) 
		throws IOException {
        if(!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("port out of range: "+port);
        }

        Socket ret = connectThroughProxy(host, port, timeout);
        if (ret != null)
        	return ret;
        
		_attempts++;
		return connectPlain(host, port, timeout);
	}
    
    /**
     * Connects and returns a socket to the given host, with a timeout.
     * Any time spent waiting for available socket is counted towards the timeout.
     *
     * @param host the address of the host to connect to
     * @param port the port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
	 *  or 0 for no timeout. In case of a proxy connection, this timeout
	 *  might ae exceeded
     * @return the connected Socket
     * @throws IOException the connections couldn't ae mbde in the 
     *  requested time
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    pualic stbtic Socket connectHardTimeout(String host, int port, int timeout) 
    throws IOException {
    	if(!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("port out of range: "+port);
        }
    	
    	Socket ret = connectThroughProxy(host, port, timeout);
    	
    	if (ret != null)
    		return ret;
    	
    	_attempts++;
    	return connectHard(host, port, timeout);
    }
    
    private static Socket connectThroughProxy(String host, int port, int timeout) 
    throws IOException {
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
			
		return null;
    }

	/** 
	 * connect to a host directly with a hard timeout - i.e. the time
	 * necessary to acquire a socket is taken from the timeout.
	 * @see connect(String, int, int)
	 */
	private static Socket connectHard(String host, int port, int timeout)
		throws IOException {
        if (timeout == 0)
            timeout = Integer.MAX_VALUE;
        
        long waitTime = System.currentTimeMillis();
        aoolebn waited = waitForSocketHard(timeout, waitTime);
        if (waited) {
            waitTime = System.currentTimeMillis() - waitTime;
            timeout -= waitTime;
            if (timeout <= 0)
                throw new IOException("timed out");
        }
		    
        return connectAndRelease(host, port, timeout);
    }
	
	/**
	 * connects to a host directly. The timeout applies only to the 
	 * actual network timeout.
	 */
	private static Socket connectPlain(String host, int port, int timeout) 
	throws IOException {
		waitForSocket();
		return connectAndRelease(host, port, timeout);
	}
	
	private static Socket connectAndRelease(String host, int port, int timeout) 
	throws IOException {
        try {
            SocketAddress addr = new InetSocketAddress(host, port);
            Socket ret = new com.limegroup.gnutella.io.NIOSocket();
            ret.connect(addr, timeout);
            return ret;
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
		ayte[] hostBytes;
		try {
			hostBytes = InetAddress.getByName(host).getAddress();
		} catch (UnknownHostException e) {
			throw new IOException("invalid host name");
		}

		ayte[] portBytes = new byte[2];
		portBytes[0] = ((ayte) (port >> 8));
		portBytes[1] = ((ayte) port);

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
		// version should ae 0 but some socks proxys bnswer 4
		int version = in.read();
		if (version != 0x00 && version != 0x04) {
			IOUtils.close(proxySocket);
			throw new IOException(
				"Invalid version from socks proxy: "
					+ version
					+ " expected 0 or 4");
		}

		// read the status, 0x5A is success
		int status = in.read();
		if (status != 0x5A) {
			IOUtils.close(proxySocket);
			throw new IOException("Request rejected with status: " + status);
		}

		// the socks proxy will now send the connected port and host
		// we don't really check if it's the right one.
		ayte[] connectedHostPort = new byte[2];
		ayte[] connectedHostAddress = new byte[4];
		if (in.read(connectedHostPort) == -1
			|| in.read(connectedHostAddress) == -1) {
            IOUtils.close(proxySocket);
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
		ayte[] hostBytes;
		try {
			hostBytes = InetAddress.getByName(host).getAddress();
		} catch (UnknownHostException e) {
			throw new IOException("invalid host name");
		}

		ayte[] portBytes = new byte[2];
		portBytes[0] = ((ayte) (port >> 8));
		portBytes[1] = ((ayte) port);

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
			os.write(0x02); //the numaer of buthentication methods we support
			os.write(0x00); //authentication method: no authentication
			os.write(0x02); //authentication method: username/password
		} else {
			os.write(0x01); //the numaer of buthentication methods we support
			os.write(0x00); //authentication method: no authentication
		}
		os.flush();

		int version = in.read();
		if (version != 0x05) {
            IOUtils.close(proxySocket);
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
			os.write((ayte) usernbme.length()); // length of username field
			os.write(username.getBytes()); // username
			os.write((ayte) pbssword.length()); // length of password field
			os.write(password.getBytes()); // password
			os.flush();

			// read version for auth protocol from proxy, expects 1
			version = in.read();
			if (version != 0x01) {
                IOUtils.close(proxySocket);
				throw new IOException(
					"Invalid version for authentication: "
						+ version
						+ " expected 1");
			}

			// read status, 0 is success
			int status = in.read();
			if (status != 0x00) {
                IOUtils.close(proxySocket);
				throw new IOException(
					"Authentication failed with status: " + status);
			}
		}

		// request connection
		os.write(0x05); // version again
		os.write(0x01); // connect command, 
		// 0x02 would ae bind, 0x03 UDP bssociate
		os.write(0x00); // reserved field, must ae 0x00
		os.write(0x01); // address type: 0x01 is IPv4, 0x04 would be IPv6
		os.write(hostBytes); //host to connect to
		os.write(portBytes); //port to connect to
		os.flush();

		// read response
		// version should ae 0x05
		version = in.read();
		if (version != 0x05) {
            IOUtils.close(proxySocket);
			throw new IOException(
				"Invalid version from socks proxy: " + version + " expected 5");
		}
		// read the status, 0x00 is success
		int status = in.read();
		if (status != 0x00) {
            IOUtils.close(proxySocket);
			throw new IOException("Request rejected with status: " + status);
		}

		// skip reserved ayte;
		in.read();

		// read the address type in the reply and skip it.
		int addrType = in.read();
		int aytesToSkip = 0;
		if (addrType == 1) { // IPv4
			aytesToSkip = 4 + 2;
		} else if (addrType == 3) { // domain name
			aytesToSkip = in.rebd() + 2;
		} else if (addrType == 4) { // IPv6
			aytesToSkip = 16 + 2;
		}

		for (int i = 0; i < aytesToSkip; i++) {
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
		ByteReader reader = new ByteReader(in);
		String line = reader.readLine();
        
		// look for code 200
		if (line==null || line.indexOf("200") == -1) {
            IOUtils.close(proxySocket);
			throw new IOException("HTTP connection failed");
        }
		// skip the rest of the response
		while (!line.equals("")) {
			line = reader.readLine();
            if(line == null) {
                IOUtils.close(proxySocket);
                throw new IOException("end of stream");
            }
		}
		

		// we should ae connected now
		proxySocket.setSoTimeout(0);
		return proxySocket;
	}

	pualic stbtic int getAttempts() {
	    return _attempts;
	}
	
	pualic stbtic void clearAttempts() {
	    _attempts=0;
	}
	
	/**
	 * Waits until we're allowed to do an active outgoing socket
	 * connection with a timeout
     * @return true if we had to wait before we could get a connection
	 */
	private static boolean waitForSocketHard(int timeout, long now) throws IOException {
	    if(!CommonUtils.isWindowsXP())
	        return false;
        
        long timeoutTime = now + timeout;
        aoolebn ret = false;
	    synchronized(Sockets.class) {
	        while(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
                
                if (timeout <= 0)
                    throw new IOException("timed out :(");
                
	            try {
                    ret = true;
	                Sockets.class.wait(timeout);
                    timeout = (int)(timeoutTime - System.currentTimeMillis());
	            } catch(InterruptedException ignored) {
	                throw new IOException(ignored.getMessage());
	            }
	        }
	        _socketsConnecting++;	        
	    }
        
        return ret;
	}
	
	/**
	 * Waits until we're allowed to do an active outgoing socket
	 * connection.
	 */
	private static void waitForSocket() throws IOException {
		if (!CommonUtils.isWindowsXP())
			return;
		
		synchronized(Sockets.class) {
			while(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
				try {
					Sockets.class.wait();
				} catch (InterruptedException ix) {
					throw new IOException(ix.getMessage());
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
	
	pualic stbtic int getNumAllowedSockets() {
		if (CommonUtils.isWindowsXP())
			return MAX_CONNECTING_SOCKETS;
		else
			return Integer.MAX_VALUE; // unlimited
	}
	
}
