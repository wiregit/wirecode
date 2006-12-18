package com.limegroup.gnutella.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.settings.ConnectionSettings;


/**
 * A collection of utilities for proxy connection establishment.
 * Used with Sockets.
 */
class ProxyUtils {
    
    private static final Log LOG = LogFactory.getLog(ProxyUtils.class);
    
    private ProxyUtils() {}
    
    /**
     * Determines the kind of proxy to use for connecting to the given address.
     */
    static int getProxyType(InetAddress address) {
		// if the user specified that he wanted to use a proxy to connect
		// to the network, we will use that proxy unless the host we
		// want to connect to is a private address
		int connectionType = ConnectionSettings.CONNECTION_METHOD.getValue();
		boolean valid =  connectionType != ConnectionSettings.C_NO_PROXY &&
                        (!NetworkUtils.isPrivateAddress(address) ||
                         ConnectionSettings.USE_PROXY_FOR_PRIVATE.getValue());
        if(valid)
            return connectionType;
        else
            return ConnectionSettings.C_NO_PROXY;
    }
    
    /**
     * Establishes a proxy connection on the given socket.
     */
    static Socket establishProxy(int type, Socket proxySocket, InetSocketAddress addr, int timeout) throws IOException {
        switch(type) {
        case ConnectionSettings.C_HTTP_PROXY: return establishHTTPProxy(proxySocket, addr, timeout);
        case ConnectionSettings.C_SOCKS4_PROXY: return establishSocksV4(proxySocket, addr, timeout);
        case ConnectionSettings.C_SOCKS5_PROXY: return establishSocksV5(proxySocket, addr, timeout);
        default: throw new IOException("Unknown proxy type.");
        }
    }
    
    /**
     * Establishes a connection with a SOCKS V4 proxy.
     */
    private static Socket establishSocksV4(Socket proxySocket, InetSocketAddress addr, int timeout) throws IOException {
        byte[] hostBytes = addr.getAddress().getAddress();
        int port = addr.getPort();
        byte[] portBytes = new byte[2];
		portBytes[0] = ((byte) (port >> 8));
		portBytes[1] = ((byte) port);

		OutputStream os = proxySocket.getOutputStream();
		InputStream in = proxySocket.getInputStream();
		proxySocket.setSoTimeout(timeout);

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
		if (version != 0x00 && version != 0x04)
			throw new IOException("Invalid version from socks proxy: " + version + " expected 0 or 4");

		// read the status, 0x5A is success
		int status = in.read();
		if (status != 0x5A)
			throw new IOException("Request rejected with status: " + status);

		// the socks proxy will now send the connected port and host
		// we don't really check if it's the right one.
		byte[] connectedHostPort = new byte[2];
		byte[] connectedHostAddress = new byte[4];
		if (in.read(connectedHostPort) == -1 || in.read(connectedHostAddress) == -1)
			throw new IOException("Connection failed");
			
		proxySocket.setSoTimeout(0);
		return proxySocket;
    }
    
    /**
     * Establishes a connection with a SOCKS V5 proxy.
     */
    private static Socket establishSocksV5(Socket proxySocket, InetSocketAddress addr, int timeout) throws IOException {
        byte[] hostBytes = addr.getAddress().getAddress();
        int port = addr.getPort();
		byte[] portBytes = new byte[2];
		portBytes[0] = ((byte) (port >> 8));
		portBytes[1] = ((byte) port);

		OutputStream os = proxySocket.getOutputStream();
		InputStream in = proxySocket.getInputStream();
		proxySocket.setSoTimeout(timeout);

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
		if (version != 0x05)
			throw new IOException("Invalid version from socks proxy: " + version + " expected 5");

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
			if (version != 0x01)
				throw new IOException("Invalid version for authentication: " + version + " expected 1");

			// read status, 0 is success
			int status = in.read();
			if (status != 0x00)
				throw new IOException("Authentication failed with status: " + status);
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
		if (version != 0x05)
			throw new IOException("Invalid version from socks proxy: " + version + " expected 5");
			
		// read the status, 0x00 is success
		int status = in.read();
		if (status != 0x00)
			throw new IOException("Request rejected with status: " + status);

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
			if (in.read() == -1)
				throw new IOException("Connection failed");
		}

		proxySocket.setSoTimeout(0);
		return proxySocket;
    }    

    /**
     * Establishes a connection with an HTTP proxy.
     */
    private static Socket establishHTTPProxy(Socket proxySocket, InetSocketAddress addr, int timeout) throws IOException {
		proxySocket.setSoTimeout(timeout);
		OutputStream os = proxySocket.getOutputStream();
		InputStream in = proxySocket.getInputStream();

		// write connection string
		String connectString = "CONNECT " + addr.getAddress().getHostAddress() + ":" + addr.getPort() + " HTTP/1.0\r\n\r\n";
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
	

    
    /**
     * ConnectObserver that will establish a proxy prior to delegating the connect back
     * to the delegate.
     */
    static class ProxyConnector implements ConnectObserver {
        private final int proxyType;
        private final ConnectObserver delegate;
        private final InetSocketAddress addr;
        private final int timeout;
        
        ProxyConnector(int type, ConnectObserver observer, InetSocketAddress host, int tout) {
            proxyType = type;
            delegate = observer;
            addr = host;
            timeout = tout;
        }
        
        public void handleConnect(final Socket s) throws IOException {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        establishProxy(proxyType, s, addr, timeout);
                    } catch(IOException iox) {
                        LOG.warn("Error establishing proxy connection", iox);
                        IOUtils.close(s);
                        shutdown(); // couldn't establish, so let delegate know.
                        return;
                    }
                    
                    // the handleConnect() notification is expected on the NIODispatcher thread.
                    Runnable r = new Runnable() {
                    	public void run() {
                    		try {
                    			delegate.handleConnect(s);
                    		} catch(IOException iox) {
                    			LOG.warn("Delegate IOX", iox);
                    			IOUtils.close(s);
                    			// do not call shutdown, because then the delegate
                    			// would get both handleConnect & shutdown, which
                    			// is confusing.
                    		}
                    	}
                    };
                    NIODispatcher.instance().invokeLater(r);
                }
            }, "ProxyConnector");
        }
       
        public void shutdown() {
            delegate.shutdown();
        }
        
        // Ignored.
        public void handleIOException(IOException iox) {}
        
        public ConnectObserver getDelegateObserver() {
            return delegate;
        }
    }
}