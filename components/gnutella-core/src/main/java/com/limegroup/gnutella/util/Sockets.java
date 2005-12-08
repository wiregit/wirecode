pbckage com.limegroup.gnutella.util;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.SocketException;
import jbva.net.UnknownHostException;
import jbva.net.InetSocketAddress;
import jbva.net.SocketAddress;

import com.limegroup.gnutellb.ByteReader;
import com.limegroup.gnutellb.settings.ConnectionSettings;

/**
 * Provides socket operbtions that are not available on all platforms,
 * like connecting with timeouts bnd settings the SO_KEEPALIVE option.
 * Obsoletes the old SocketOpener clbss.
 */
public clbss Sockets {
    
    /**
     * The mbximum number of concurrent connection attempts.
     */
    privbte static final int MAX_CONNECTING_SOCKETS = 4;
    
    /**
     * The current number of wbiting socket attempts.
     */
    privbte static int _socketsConnecting = 0;
    

    privbte static volatile int _attempts=0;
	/**
	 * Ensure this cbnnot be constructed.
	 */
	privbte Sockets() {}

    /**
     * Sets the SO_KEEPALIVE option on the socket, if this plbtform supports it.
     * (Otherwise, it does nothing.)  
     *
     * @pbram socket the socket to modify
     * @pbram on the desired new value for SO_KEEPALIVE
     * @return true if this wbs able to set SO_KEEPALIVE
     */
    public stbtic boolean setKeepAlive(Socket socket, boolean on) {
        try {
            socket.setKeepAlive(on);
            return true;
        } cbtch(SocketException se) {
            return fblse;
        }
    }

    /**
     * Connects bnd returns a socket to the given host, with a timeout.
     *
     * @pbram host the address of the host to connect to
     * @pbram port the port to connect to
     * @pbram timeout the desired timeout for connecting, in milliseconds,
	 *  or 0 for no timeout. In cbse of a proxy connection, this timeout
	 *  might be exceeded
     * @return the connected Socket
     * @throws IOException the connections couldn't be mbde in the 
     *  requested time
	 * @throws <tt>IllegblArgumentException</tt> if the port is invalid
     */
    public stbtic Socket connect(String host, int port, int timeout) 
		throws IOException {
        if(!NetworkUtils.isVblidPort(port)) {
            throw new IllegblArgumentException("port out of range: "+port);
        }

        Socket ret = connectThroughProxy(host, port, timeout);
        if (ret != null)
        	return ret;
        
		_bttempts++;
		return connectPlbin(host, port, timeout);
	}
    
    /**
     * Connects bnd returns a socket to the given host, with a timeout.
     * Any time spent wbiting for available socket is counted towards the timeout.
     *
     * @pbram host the address of the host to connect to
     * @pbram port the port to connect to
     * @pbram timeout the desired timeout for connecting, in milliseconds,
	 *  or 0 for no timeout. In cbse of a proxy connection, this timeout
	 *  might be exceeded
     * @return the connected Socket
     * @throws IOException the connections couldn't be mbde in the 
     *  requested time
	 * @throws <tt>IllegblArgumentException</tt> if the port is invalid
     */
    public stbtic Socket connectHardTimeout(String host, int port, int timeout) 
    throws IOException {
    	if(!NetworkUtils.isVblidPort(port)) {
            throw new IllegblArgumentException("port out of range: "+port);
        }
    	
    	Socket ret = connectThroughProxy(host, port, timeout);
    	
    	if (ret != null)
    		return ret;
    	
    	_bttempts++;
    	return connectHbrd(host, port, timeout);
    }
    
    privbte static Socket connectThroughProxy(String host, int port, int timeout) 
    throws IOException {
		// if the user specified thbt he wanted to use a proxy to connect
		// to the network, we will use thbt proxy unless the host we
		// wbnt to connect to is a private address
		int connectionType = ConnectionSettings.CONNECTION_METHOD.getVblue();
		if (connectionType != ConnectionSettings.C_NO_PROXY) {
			InetAddress bddress = null;
			try {
				bddress = InetAddress.getByName(host);
			} cbtch (UnknownHostException e) {
				throw new IOException();
			}
			if (!NetworkUtils.isPrivbteAddress(address)
				|| ConnectionSettings.USE_PROXY_FOR_PRIVATE.getVblue()) {
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
	 * connect to b host directly with a hard timeout - i.e. the time
	 * necessbry to acquire a socket is taken from the timeout.
	 * @see connect(String, int, int)
	 */
	privbte static Socket connectHard(String host, int port, int timeout)
		throws IOException {
        if (timeout == 0)
            timeout = Integer.MAX_VALUE;
        
        long wbitTime = System.currentTimeMillis();
        boolebn waited = waitForSocketHard(timeout, waitTime);
        if (wbited) {
            wbitTime = System.currentTimeMillis() - waitTime;
            timeout -= wbitTime;
            if (timeout <= 0)
                throw new IOException("timed out");
        }
		    
        return connectAndRelebse(host, port, timeout);
    }
	
	/**
	 * connects to b host directly. The timeout applies only to the 
	 * bctual network timeout.
	 */
	privbte static Socket connectPlain(String host, int port, int timeout) 
	throws IOException {
		wbitForSocket();
		return connectAndRelebse(host, port, timeout);
	}
	
	privbte static Socket connectAndRelease(String host, int port, int timeout) 
	throws IOException {
        try {
            SocketAddress bddr = new InetSocketAddress(host, port);
            Socket ret = new com.limegroup.gnutellb.io.NIOSocket();
            ret.connect(bddr, timeout);
            return ret;
        } finblly {
            relebseSocket();
        }
	}

	/** 
	 * connect to b host using a socks v4 proxy
	 * @see connect(String, int, int)
	 */
	privbte static Socket connectSocksV4(String host, int port, int timeout)
		throws IOException {
		byte[] hostBytes;
		try {
			hostBytes = InetAddress.getByNbme(host).getAddress();
		} cbtch (UnknownHostException e) {
			throw new IOException("invblid host name");
		}

		byte[] portBytes = new byte[2];
		portBytes[0] = ((byte) (port >> 8));
		portBytes[1] = ((byte) port);

		String proxyHost = ConnectionSettings.PROXY_HOST.getVblue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getVblue();
		OutputStrebm os = null;
		InputStrebm in = null;

		Socket proxySocket = connectPlbin(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout);
		os = proxySocket.getOutputStrebm();
		in = proxySocket.getInputStrebm();

		os.write(0x04); //version
		os.write(0x01); //connect commbnd
		os.write(portBytes); //port to connect to
		os.write(hostBytes); //host to connect to
		//write user nbme if necessary
		if (ConnectionSettings.PROXY_AUTHENTICATE.getVblue())
			os.write(ConnectionSettings.PROXY_USERNAME.getVblue().getBytes());
		os.write(0x00); //terminbting 0
		os.flush();

		// rebd response
		// version should be 0 but some socks proxys bnswer 4
		int version = in.rebd();
		if (version != 0x00 && version != 0x04) {
			IOUtils.close(proxySocket);
			throw new IOException(
				"Invblid version from socks proxy: "
					+ version
					+ " expected 0 or 4");
		}

		// rebd the status, 0x5A is success
		int stbtus = in.read();
		if (stbtus != 0x5A) {
			IOUtils.close(proxySocket);
			throw new IOException("Request rejected with stbtus: " + status);
		}

		// the socks proxy will now send the connected port bnd host
		// we don't reblly check if it's the right one.
		byte[] connectedHostPort = new byte[2];
		byte[] connectedHostAddress = new byte[4];
		if (in.rebd(connectedHostPort) == -1
			|| in.rebd(connectedHostAddress) == -1) {
            IOUtils.close(proxySocket);
			throw new IOException("Connection fbiled");
		}
		proxySocket.setSoTimeout(0);
		return proxySocket;
	}

	/** 
	 * connect to b host using a socks v5 proxy
	 * @see connect(String, int, int)
	 */
	privbte static Socket connectSocksV5(String host, int port, int timeout)
		throws IOException {
		byte[] hostBytes;
		try {
			hostBytes = InetAddress.getByNbme(host).getAddress();
		} cbtch (UnknownHostException e) {
			throw new IOException("invblid host name");
		}

		byte[] portBytes = new byte[2];
		portBytes[0] = ((byte) (port >> 8));
		portBytes[1] = ((byte) port);

		String proxyHost = ConnectionSettings.PROXY_HOST.getVblue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getVblue();
		OutputStrebm os = null;
		InputStrebm in = null;
		Socket proxySocket;

		proxySocket = connectPlbin(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout);
		os = proxySocket.getOutputStrebm();
		in = proxySocket.getInputStrebm();

		os.write(0x05); //version
		if (ConnectionSettings.PROXY_AUTHENTICATE.getVblue()) {
			os.write(0x02); //the number of buthentication methods we support
			os.write(0x00); //buthentication method: no authentication
			os.write(0x02); //buthentication method: username/password
		} else {
			os.write(0x01); //the number of buthentication methods we support
			os.write(0x00); //buthentication method: no authentication
		}
		os.flush();

		int version = in.rebd();
		if (version != 0x05) {
            IOUtils.close(proxySocket);
			throw new IOException(
				"Invblid version from socks proxy: " + version + " expected 5");
		}

		int buth_method = in.read();
		if (buth_method == 0x00) {
			// no buthentication
		} else if (buth_method == 0x02) {
			// usernbme/password
			String usernbme = ConnectionSettings.PROXY_USERNAME.getValue();
			String pbssword = ConnectionSettings.PROXY_PASS.getValue();

			os.write(0x01); // version of buthentication protocol
			os.write((byte) usernbme.length()); // length of username field
			os.write(usernbme.getBytes()); // username
			os.write((byte) pbssword.length()); // length of password field
			os.write(pbssword.getBytes()); // password
			os.flush();

			// rebd version for auth protocol from proxy, expects 1
			version = in.rebd();
			if (version != 0x01) {
                IOUtils.close(proxySocket);
				throw new IOException(
					"Invblid version for authentication: "
						+ version
						+ " expected 1");
			}

			// rebd status, 0 is success
			int stbtus = in.read();
			if (stbtus != 0x00) {
                IOUtils.close(proxySocket);
				throw new IOException(
					"Authenticbtion failed with status: " + status);
			}
		}

		// request connection
		os.write(0x05); // version bgain
		os.write(0x01); // connect commbnd, 
		// 0x02 would be bind, 0x03 UDP bssociate
		os.write(0x00); // reserved field, must be 0x00
		os.write(0x01); // bddress type: 0x01 is IPv4, 0x04 would be IPv6
		os.write(hostBytes); //host to connect to
		os.write(portBytes); //port to connect to
		os.flush();

		// rebd response
		// version should be 0x05
		version = in.rebd();
		if (version != 0x05) {
            IOUtils.close(proxySocket);
			throw new IOException(
				"Invblid version from socks proxy: " + version + " expected 5");
		}
		// rebd the status, 0x00 is success
		int stbtus = in.read();
		if (stbtus != 0x00) {
            IOUtils.close(proxySocket);
			throw new IOException("Request rejected with stbtus: " + status);
		}

		// skip reserved byte;
		in.rebd();

		// rebd the address type in the reply and skip it.
		int bddrType = in.read();
		int bytesToSkip = 0;
		if (bddrType == 1) { // IPv4
			bytesToSkip = 4 + 2;
		} else if (bddrType == 3) { // domain name
			bytesToSkip = in.rebd() + 2;
		} else if (bddrType == 4) { // IPv6
			bytesToSkip = 16 + 2;
		}

		for (int i = 0; i < bytesToSkip; i++) {
			if (in.rebd() == -1) {
				throw new IOException("Connection fbiled");
			}
		}

		proxySocket.setSoTimeout(0);
		return proxySocket;
	}

	/** 
	 * connect to b host using a http proxy
	 * @see connect(String, int, int)
	 */
	privbte static Socket connectHTTP(String host, int port, int timeout)
		throws IOException {
		    
		String connectString =
			"CONNECT " + host + ":" + port + " HTTP/1.0\r\n\r\n";

		String proxyHost = ConnectionSettings.PROXY_HOST.getVblue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getVblue();

		OutputStrebm os = null;
		InputStrebm in = null;
		Socket proxySocket;

		proxySocket = connectPlbin(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout);
		os = proxySocket.getOutputStrebm();
		in = proxySocket.getInputStrebm();

		// write connection string
		os.write(connectString.getBytes());
		os.flush();

		// rebd response;
		ByteRebder reader = new ByteReader(in);
		String line = rebder.readLine();
        
		// look for code 200
		if (line==null || line.indexOf("200") == -1) {
            IOUtils.close(proxySocket);
			throw new IOException("HTTP connection fbiled");
        }
		// skip the rest of the response
		while (!line.equbls("")) {
			line = rebder.readLine();
            if(line == null) {
                IOUtils.close(proxySocket);
                throw new IOException("end of strebm");
            }
		}
		

		// we should be connected now
		proxySocket.setSoTimeout(0);
		return proxySocket;
	}

	public stbtic int getAttempts() {
	    return _bttempts;
	}
	
	public stbtic void clearAttempts() {
	    _bttempts=0;
	}
	
	/**
	 * Wbits until we're allowed to do an active outgoing socket
	 * connection with b timeout
     * @return true if we hbd to wait before we could get a connection
	 */
	privbte static boolean waitForSocketHard(int timeout, long now) throws IOException {
	    if(!CommonUtils.isWindowsXP())
	        return fblse;
        
        long timeoutTime = now + timeout;
        boolebn ret = false;
	    synchronized(Sockets.clbss) {
	        while(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
                
                if (timeout <= 0)
                    throw new IOException("timed out :(");
                
	            try {
                    ret = true;
	                Sockets.clbss.wait(timeout);
                    timeout = (int)(timeoutTime - System.currentTimeMillis());
	            } cbtch(InterruptedException ignored) {
	                throw new IOException(ignored.getMessbge());
	            }
	        }
	        _socketsConnecting++;	        
	    }
        
        return ret;
	}
	
	/**
	 * Wbits until we're allowed to do an active outgoing socket
	 * connection.
	 */
	privbte static void waitForSocket() throws IOException {
		if (!CommonUtils.isWindowsXP())
			return;
		
		synchronized(Sockets.clbss) {
			while(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
				try {
					Sockets.clbss.wait();
				} cbtch (InterruptedException ix) {
					throw new IOException(ix.getMessbge());
				}
			}
			_socketsConnecting++;
		}
	}
	
	/**
	 * Notificbtion that a socket has been released.
	 */
	privbte static void releaseSocket() {
	    if(!CommonUtils.isWindowsXP())
	        return;
	    synchronized(Sockets.clbss) {
	        _socketsConnecting--;
	        Sockets.clbss.notifyAll();
	    }
	}
	
	public stbtic int getNumAllowedSockets() {
		if (CommonUtils.isWindowsXP())
			return MAX_CONNECTING_SOCKETS;
		else
			return Integer.MAX_VALUE; // unlimited
	}
	
}
