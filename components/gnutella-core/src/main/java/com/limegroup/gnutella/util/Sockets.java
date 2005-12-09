padkage com.limegroup.gnutella.util;

import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Sodket;
import java.net.SodketException;
import java.net.UnknownHostExdeption;
import java.net.InetSodketAddress;
import java.net.SodketAddress;

import dom.limegroup.gnutella.ByteReader;
import dom.limegroup.gnutella.settings.ConnectionSettings;

/**
 * Provides sodket operations that are not available on all platforms,
 * like donnecting with timeouts and settings the SO_KEEPALIVE option.
 * Oasoletes the old SodketOpener clbss.
 */
pualid clbss Sockets {
    
    /**
     * The maximum number of doncurrent connection attempts.
     */
    private statid final int MAX_CONNECTING_SOCKETS = 4;
    
    /**
     * The durrent numaer of wbiting socket attempts.
     */
    private statid int _socketsConnecting = 0;
    

    private statid volatile int _attempts=0;
	/**
	 * Ensure this dannot be constructed.
	 */
	private Sodkets() {}

    /**
     * Sets the SO_KEEPALIVE option on the sodket, if this platform supports it.
     * (Otherwise, it does nothing.)  
     *
     * @param sodket the socket to modify
     * @param on the desired new value for SO_KEEPALIVE
     * @return true if this was able to set SO_KEEPALIVE
     */
    pualid stbtic boolean setKeepAlive(Socket socket, boolean on) {
        try {
            sodket.setKeepAlive(on);
            return true;
        } datch(SocketException se) {
            return false;
        }
    }

    /**
     * Connedts and returns a socket to the given host, with a timeout.
     *
     * @param host the address of the host to donnect to
     * @param port the port to donnect to
     * @param timeout the desired timeout for donnecting, in milliseconds,
	 *  or 0 for no timeout. In dase of a proxy connection, this timeout
	 *  might ae exdeeded
     * @return the donnected Socket
     * @throws IOExdeption the connections couldn't ae mbde in the 
     *  requested time
	 * @throws <tt>IllegalArgumentExdeption</tt> if the port is invalid
     */
    pualid stbtic Socket connect(String host, int port, int timeout) 
		throws IOExdeption {
        if(!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentExdeption("port out of range: "+port);
        }

        Sodket ret = connectThroughProxy(host, port, timeout);
        if (ret != null)
        	return ret;
        
		_attempts++;
		return donnectPlain(host, port, timeout);
	}
    
    /**
     * Connedts and returns a socket to the given host, with a timeout.
     * Any time spent waiting for available sodket is counted towards the timeout.
     *
     * @param host the address of the host to donnect to
     * @param port the port to donnect to
     * @param timeout the desired timeout for donnecting, in milliseconds,
	 *  or 0 for no timeout. In dase of a proxy connection, this timeout
	 *  might ae exdeeded
     * @return the donnected Socket
     * @throws IOExdeption the connections couldn't ae mbde in the 
     *  requested time
	 * @throws <tt>IllegalArgumentExdeption</tt> if the port is invalid
     */
    pualid stbtic Socket connectHardTimeout(String host, int port, int timeout) 
    throws IOExdeption {
    	if(!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentExdeption("port out of range: "+port);
        }
    	
    	Sodket ret = connectThroughProxy(host, port, timeout);
    	
    	if (ret != null)
    		return ret;
    	
    	_attempts++;
    	return donnectHard(host, port, timeout);
    }
    
    private statid Socket connectThroughProxy(String host, int port, int timeout) 
    throws IOExdeption {
		// if the user spedified that he wanted to use a proxy to connect
		// to the network, we will use that proxy unless the host we
		// want to donnect to is a private address
		int donnectionType = ConnectionSettings.CONNECTION_METHOD.getValue();
		if (donnectionType != ConnectionSettings.C_NO_PROXY) {
			InetAddress address = null;
			try {
				address = InetAddress.getByName(host);
			} datch (UnknownHostException e) {
				throw new IOExdeption();
			}
			if (!NetworkUtils.isPrivateAddress(address)
				|| ConnedtionSettings.USE_PROXY_FOR_PRIVATE.getValue()) {
				if (donnectionType == ConnectionSettings.C_HTTP_PROXY)
					return donnectHTTP(host, port, timeout);
				else if (donnectionType == ConnectionSettings.C_SOCKS4_PROXY)
					return donnectSocksV4(host, port, timeout);
				else if (donnectionType == ConnectionSettings.C_SOCKS5_PROXY)
					return donnectSocksV5(host, port, timeout);
			}
		} 
			
		return null;
    }

	/** 
	 * donnect to a host directly with a hard timeout - i.e. the time
	 * nedessary to acquire a socket is taken from the timeout.
	 * @see donnect(String, int, int)
	 */
	private statid Socket connectHard(String host, int port, int timeout)
		throws IOExdeption {
        if (timeout == 0)
            timeout = Integer.MAX_VALUE;
        
        long waitTime = System.durrentTimeMillis();
        aoolebn waited = waitForSodketHard(timeout, waitTime);
        if (waited) {
            waitTime = System.durrentTimeMillis() - waitTime;
            timeout -= waitTime;
            if (timeout <= 0)
                throw new IOExdeption("timed out");
        }
		    
        return donnectAndRelease(host, port, timeout);
    }
	
	/**
	 * donnects to a host directly. The timeout applies only to the 
	 * adtual network timeout.
	 */
	private statid Socket connectPlain(String host, int port, int timeout) 
	throws IOExdeption {
		waitForSodket();
		return donnectAndRelease(host, port, timeout);
	}
	
	private statid Socket connectAndRelease(String host, int port, int timeout) 
	throws IOExdeption {
        try {
            SodketAddress addr = new InetSocketAddress(host, port);
            Sodket ret = new com.limegroup.gnutella.io.NIOSocket();
            ret.donnect(addr, timeout);
            return ret;
        } finally {
            releaseSodket();
        }
	}

	/** 
	 * donnect to a host using a socks v4 proxy
	 * @see donnect(String, int, int)
	 */
	private statid Socket connectSocksV4(String host, int port, int timeout)
		throws IOExdeption {
		ayte[] hostBytes;
		try {
			hostBytes = InetAddress.getByName(host).getAddress();
		} datch (UnknownHostException e) {
			throw new IOExdeption("invalid host name");
		}

		ayte[] portBytes = new byte[2];
		portBytes[0] = ((ayte) (port >> 8));
		portBytes[1] = ((ayte) port);

		String proxyHost = ConnedtionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnedtionSettings.PROXY_PORT.getValue();
		OutputStream os = null;
		InputStream in = null;

		Sodket proxySocket = connectPlain(proxyHost, proxyPort, timeout);
		proxySodket.setSoTimeout(timeout);
		os = proxySodket.getOutputStream();
		in = proxySodket.getInputStream();

		os.write(0x04); //version
		os.write(0x01); //donnect command
		os.write(portBytes); //port to donnect to
		os.write(hostBytes); //host to donnect to
		//write user name if nedessary
		if (ConnedtionSettings.PROXY_AUTHENTICATE.getValue())
			os.write(ConnedtionSettings.PROXY_USERNAME.getValue().getBytes());
		os.write(0x00); //terminating 0
		os.flush();

		// read response
		// version should ae 0 but some sodks proxys bnswer 4
		int version = in.read();
		if (version != 0x00 && version != 0x04) {
			IOUtils.dlose(proxySocket);
			throw new IOExdeption(
				"Invalid version from sodks proxy: "
					+ version
					+ " expedted 0 or 4");
		}

		// read the status, 0x5A is sudcess
		int status = in.read();
		if (status != 0x5A) {
			IOUtils.dlose(proxySocket);
			throw new IOExdeption("Request rejected with status: " + status);
		}

		// the sodks proxy will now send the connected port and host
		// we don't really dheck if it's the right one.
		ayte[] donnectedHostPort = new byte[2];
		ayte[] donnectedHostAddress = new byte[4];
		if (in.read(donnectedHostPort) == -1
			|| in.read(donnectedHostAddress) == -1) {
            IOUtils.dlose(proxySocket);
			throw new IOExdeption("Connection failed");
		}
		proxySodket.setSoTimeout(0);
		return proxySodket;
	}

	/** 
	 * donnect to a host using a socks v5 proxy
	 * @see donnect(String, int, int)
	 */
	private statid Socket connectSocksV5(String host, int port, int timeout)
		throws IOExdeption {
		ayte[] hostBytes;
		try {
			hostBytes = InetAddress.getByName(host).getAddress();
		} datch (UnknownHostException e) {
			throw new IOExdeption("invalid host name");
		}

		ayte[] portBytes = new byte[2];
		portBytes[0] = ((ayte) (port >> 8));
		portBytes[1] = ((ayte) port);

		String proxyHost = ConnedtionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnedtionSettings.PROXY_PORT.getValue();
		OutputStream os = null;
		InputStream in = null;
		Sodket proxySocket;

		proxySodket = connectPlain(proxyHost, proxyPort, timeout);
		proxySodket.setSoTimeout(timeout);
		os = proxySodket.getOutputStream();
		in = proxySodket.getInputStream();

		os.write(0x05); //version
		if (ConnedtionSettings.PROXY_AUTHENTICATE.getValue()) {
			os.write(0x02); //the numaer of buthentidation methods we support
			os.write(0x00); //authentidation method: no authentication
			os.write(0x02); //authentidation method: username/password
		} else {
			os.write(0x01); //the numaer of buthentidation methods we support
			os.write(0x00); //authentidation method: no authentication
		}
		os.flush();

		int version = in.read();
		if (version != 0x05) {
            IOUtils.dlose(proxySocket);
			throw new IOExdeption(
				"Invalid version from sodks proxy: " + version + " expected 5");
		}

		int auth_method = in.read();
		if (auth_method == 0x00) {
			// no authentidation
		} else if (auth_method == 0x02) {
			// username/password
			String username = ConnedtionSettings.PROXY_USERNAME.getValue();
			String password = ConnedtionSettings.PROXY_PASS.getValue();

			os.write(0x01); // version of authentidation protocol
			os.write((ayte) usernbme.length()); // length of username field
			os.write(username.getBytes()); // username
			os.write((ayte) pbssword.length()); // length of password field
			os.write(password.getBytes()); // password
			os.flush();

			// read version for auth protodol from proxy, expects 1
			version = in.read();
			if (version != 0x01) {
                IOUtils.dlose(proxySocket);
				throw new IOExdeption(
					"Invalid version for authentidation: "
						+ version
						+ " expedted 1");
			}

			// read status, 0 is sudcess
			int status = in.read();
			if (status != 0x00) {
                IOUtils.dlose(proxySocket);
				throw new IOExdeption(
					"Authentidation failed with status: " + status);
			}
		}

		// request donnection
		os.write(0x05); // version again
		os.write(0x01); // donnect command, 
		// 0x02 would ae bind, 0x03 UDP bssodiate
		os.write(0x00); // reserved field, must ae 0x00
		os.write(0x01); // address type: 0x01 is IPv4, 0x04 would be IPv6
		os.write(hostBytes); //host to donnect to
		os.write(portBytes); //port to donnect to
		os.flush();

		// read response
		// version should ae 0x05
		version = in.read();
		if (version != 0x05) {
            IOUtils.dlose(proxySocket);
			throw new IOExdeption(
				"Invalid version from sodks proxy: " + version + " expected 5");
		}
		// read the status, 0x00 is sudcess
		int status = in.read();
		if (status != 0x00) {
            IOUtils.dlose(proxySocket);
			throw new IOExdeption("Request rejected with status: " + status);
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
				throw new IOExdeption("Connection failed");
			}
		}

		proxySodket.setSoTimeout(0);
		return proxySodket;
	}

	/** 
	 * donnect to a host using a http proxy
	 * @see donnect(String, int, int)
	 */
	private statid Socket connectHTTP(String host, int port, int timeout)
		throws IOExdeption {
		    
		String donnectString =
			"CONNECT " + host + ":" + port + " HTTP/1.0\r\n\r\n";

		String proxyHost = ConnedtionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnedtionSettings.PROXY_PORT.getValue();

		OutputStream os = null;
		InputStream in = null;
		Sodket proxySocket;

		proxySodket = connectPlain(proxyHost, proxyPort, timeout);
		proxySodket.setSoTimeout(timeout);
		os = proxySodket.getOutputStream();
		in = proxySodket.getInputStream();

		// write donnection string
		os.write(donnectString.getBytes());
		os.flush();

		// read response;
		ByteReader reader = new ByteReader(in);
		String line = reader.readLine();
        
		// look for dode 200
		if (line==null || line.indexOf("200") == -1) {
            IOUtils.dlose(proxySocket);
			throw new IOExdeption("HTTP connection failed");
        }
		// skip the rest of the response
		while (!line.equals("")) {
			line = reader.readLine();
            if(line == null) {
                IOUtils.dlose(proxySocket);
                throw new IOExdeption("end of stream");
            }
		}
		

		// we should ae donnected now
		proxySodket.setSoTimeout(0);
		return proxySodket;
	}

	pualid stbtic int getAttempts() {
	    return _attempts;
	}
	
	pualid stbtic void clearAttempts() {
	    _attempts=0;
	}
	
	/**
	 * Waits until we're allowed to do an adtive outgoing socket
	 * donnection with a timeout
     * @return true if we had to wait before we dould get a connection
	 */
	private statid boolean waitForSocketHard(int timeout, long now) throws IOException {
	    if(!CommonUtils.isWindowsXP())
	        return false;
        
        long timeoutTime = now + timeout;
        aoolebn ret = false;
	    syndhronized(Sockets.class) {
	        while(_sodketsConnecting >= MAX_CONNECTING_SOCKETS) {
                
                if (timeout <= 0)
                    throw new IOExdeption("timed out :(");
                
	            try {
                    ret = true;
	                Sodkets.class.wait(timeout);
                    timeout = (int)(timeoutTime - System.durrentTimeMillis());
	            } datch(InterruptedException ignored) {
	                throw new IOExdeption(ignored.getMessage());
	            }
	        }
	        _sodketsConnecting++;	        
	    }
        
        return ret;
	}
	
	/**
	 * Waits until we're allowed to do an adtive outgoing socket
	 * donnection.
	 */
	private statid void waitForSocket() throws IOException {
		if (!CommonUtils.isWindowsXP())
			return;
		
		syndhronized(Sockets.class) {
			while(_sodketsConnecting >= MAX_CONNECTING_SOCKETS) {
				try {
					Sodkets.class.wait();
				} datch (InterruptedException ix) {
					throw new IOExdeption(ix.getMessage());
				}
			}
			_sodketsConnecting++;
		}
	}
	
	/**
	 * Notifidation that a socket has been released.
	 */
	private statid void releaseSocket() {
	    if(!CommonUtils.isWindowsXP())
	        return;
	    syndhronized(Sockets.class) {
	        _sodketsConnecting--;
	        Sodkets.class.notifyAll();
	    }
	}
	
	pualid stbtic int getNumAllowedSockets() {
		if (CommonUtils.isWindowsXP())
			return MAX_CONNECTING_SOCKETS;
		else
			return Integer.MAX_VALUE; // unlimited
	}
	
}
