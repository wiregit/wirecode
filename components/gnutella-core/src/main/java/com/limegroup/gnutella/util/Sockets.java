
// Commented for the Learning branch

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
 * Open a TCP socket connection to a remote computer, through a proxy server if configured, and mindful of the Event 4226 limit.
 * 
 * Code in this class does 2 jobs.
 * It makes sure the program does not hit the half-open connection limit imposed by Windows XP Service Pack 2.
 * It connects through a proxy server if one is configured in settings.
 * 
 * There are 2 public methods here: connect(), and connectHardTimeout().
 * Both take a timeout, but they do not use it the same way.
 * connect() waits for Event 4226, and then uses the timeout for making the connection.
 * connectHardTimeout() uses the timeout for Event 4226 waiting and the connection.
 * So, connect() can block for longer than the timeout, while connectHardTimeout() won't.
 * 
 * This class has static methods the rest of the program uses.
 * The program should never make a Sockets object, that wouldn't make sense.
 * To make sure there is never a Sockets object, make the constructor empty and private.
 * 
 * This class makes the old SocketOpener class obsolete.
 */
public class Sockets {

    /**
     * 4, the maximum number of TCP connections that LimeWire will attempt when running on Windows XP Service Pack 2 or later.
     * 
     * Windows XP Service Pack 2 imposes a new limit on programs that try to open a lot of TCP connections.
     * If a program has too many half-open connections, Windows shuts off its Internet access.
     * When it does this, it logs Event 4226 in the Event Viewer.
     * 
     * Here is Microsoft's page that describes this security feature:
     * http://www.microsoft.com/technet/prodtechnol/winxppro/maintain/sp2netwk.mspx
     * 
     * This limit is in Windows XP Service Pack 2, and all later versions of Windows.
     * As people install the service pack and get new Windows computers, soon almost all Windows computers running LimeWire will have this limitation.
     * 
     * LimeWire doesn't limit itself this way when running on other operating systems.
     * On Macintosh and Linux, it will open connections without waiting to stay under any limit.
     * 
     * The limit is supposed to be 10 half-open connections, but LimeWire developers have found it to be much lower than that.
     * LimeWire is configured to only have 4 half-open connections at a time.
     */
    private static final int MAX_CONNECTING_SOCKETS = 4;

    /**
     * The number of TCP socket connections that we've initiated, but that haven't connected or failed yet.
     * These are the half-open socket connections we have to watch to avoid the Event 4226 limit on Windows.
     */
    private static int _socketsConnecting = 0;

    /**
     * The number of times connect() and connectHardTimeout() have tried to open new connections.
     */
    private static volatile int _attempts = 0;

	/**
     * This class has static methods the rest of the program uses.
     * The program should never make a Sockets object, that wouldn't make sense.
     * To make sure there is never a Sockets object, make the constructor empty and private.
	 */
	private Sockets() {}

    /**
     * Enables or disables the SO_KEEPALIVE option on the socket.
     * If SO_KEEPALIVE is enabled and the connection is silent for a very long time, the socket will send a keepalive probe packet. 
     * If the operating system Java is running on doesn't have sockets that support SO_KEEPALIVE, this method does nothing.
     * 
     * Only HTTPDownloader.connectTCP() uses this.
     * 
     * @param socket The java.net.Socket object to modify
     * @param on     True to turn SO_KEEPALIVE on, false to turn it off
     * @return       True if Java could set the option on the socket
     */
    public static boolean setKeepAlive(Socket socket, boolean on) {

        try {

            // Set the SO_KEEPALIVE option
            socket.setKeepAlive(on);

            // That worked without throwing an exception, return true
            return true;

        // There was an error in the underlying protocol, such as a TCP error
        } catch (SocketException se) {

            // Sockets on this platform must not support SO_KEEPALIVE
            return false;
        }
    }

    /**
     * Make a new NIOSocket object, and connect it to the given IP address and port number.
     * Connects through a proxy server if LimeWire is configured that way.
     * The timeout applies just to making the connection, not to waiting because of Event 4226 beforehand.
     * This method might exceed it if the program connects through a proxy server.
     * 
     * @param  host        The IP address we want to connect to, like "64.61.25.171"
     * @param  port        The port number at that address we want to connect to, like 6346
     * @param  timeout     How long NIOSocket will wait to make the connection, like 6 seconds, 0 for no timeout
     * @return             A new NIOSocket object connected to the remote computer at the given address
     * @throws IOException If the timeout expired before the connection could be made
     */
    public static Socket connect(String host, int port, int timeout) throws IOException {

        // Make sure the given port number is 1 through 63535
        if (!NetworkUtils.isValidPort(port)) throw new IllegalArgumentException("port out of range: " + port);

        // If LimeWire is configured to use a proxy server, connect through it
        Socket ret = connectThroughProxy(host, port, timeout); // Returns null if LimeWire isn't configured to use a proxy server
        if (ret != null) return ret; // Return the connection connectThroughProxy() made

        // Make a new NIOSocket object, connect it to the given IP address and port number, and return it
        _attempts++;                              // Count this attempt to connect
		return connectPlain(host, port, timeout); // Apply the timeout to the connection only
	}

    /**
     * Make a new NIOSocket object, and connect it to the given IP address and port number.
     * Connects through a proxy server if LimeWire is configured that way.
     * The timeout applies both to waiting for Event 4226, and making the connection.
     * This method might exceed it if the program connects through a proxy server.
     * 
     * Only ConnectionChecker.connectToHost uses connectHardTimeout().
     * Other parts of LimeWire use connect() instead.
     * Hard means the timeout applies to the time waiting for Event 4226 and the time NIOSocket takes to make the connection.
     * 
     * @param  host        The IP address we want to connect to, like "64.61.25.171"
     * @param  port        The port number at that address we want to connect to, like 6346
     * @param  timeout     The time limit for both waiting to connect, and connecting, 0 for no timeout
     * @return             A new NIOSocket object connected to the remote computer at the given address
     * @throws IOException If the timeout expired before the connection could be made
     */
    public static Socket connectHardTimeout(String host, int port, int timeout) throws IOException {

        // Make sure the given port number is 1 through 63535
    	if (!NetworkUtils.isValidPort(port)) throw new IllegalArgumentException("port out of range: " + port);

        // If LimeWire is configured to use a proxy server, connect through it
    	Socket ret = connectThroughProxy(host, port, timeout); // Returns null if LimeWire isn't configured to use a proxy server
    	if (ret != null) return ret; // Return the connection connectThroughProxy made()

        // Make a new NIOSocket object, connect it to the given IP address and port number, and return it
    	_attempts++;                             // Count this attempt to connect
    	return connectHard(host, port, timeout); // Apply the timeout to the wait and the connection
    }

    /**
     * Connect to the remote computer through the proxy server configured in settings.
     * 
     * With a proxy server, we make every connection to it instead of to remote computers on the Internet.
     * We tell it what IP address and port number to connect to, and it connects to the remote computer.
     * We keep our socket to the proxy server, and talk to the remote computer through it.
     * This lets the proxy server keep track of which computers we are connecting to and what we are saying to them.
     * 
     * @param  host        The IP address we want to connect to, like "64.61.25.171"
     * @param  port        The port number at that address we want to connect to, like 6346
     * @param  timeout     Not used.
     * @return             A new NIOSocket object connected to the remote computer through the proxy server
     */
    private static Socket connectThroughProxy(String host, int port, int timeout) throws IOException {
        
        // if the user specified that he wanted to use a proxy to connect
        // to the network, we will use that proxy unless the host we
        // want to connect to is a private address
        
        // Look in settings to see if we need to connect through a proxy server
        int connectionType = ConnectionSettings.CONNECTION_METHOD.getValue();
        if (connectionType == ConnectionSettings.C_NO_PROXY) return null; // No proxy needed, leave now

        // Turn the given string like "64.61.25.171" into an InetAddress object
        InetAddress address = null;
        try { address = InetAddress.getByName(host); } catch (UnknownHostException e) { throw new IOException(); }

        // If the given address is a LAN address and settings don't instruct us to use the proxy server for those, leave now
        if (NetworkUtils.isPrivateAddress(address) && !ConnectionSettings.USE_PROXY_FOR_PRIVATE.getValue()) return null;

        // Connect with HTTP, Socks 4, or Socks 5
        if      (connectionType == ConnectionSettings.C_HTTP_PROXY)   return connectHTTP(host, port, timeout);
        else if (connectionType == ConnectionSettings.C_SOCKS4_PROXY) return connectSocksV4(host, port, timeout);
        else if (connectionType == ConnectionSettings.C_SOCKS5_PROXY) return connectSocksV5(host, port, timeout);
        return null;
    }

	/**
     * Make a new NIOSocket object, and connect it to the given IP address and port number.
     * 
     * Hard means the timeout applies to the time waiting for Event 4226 and the time NIOSocket takes to make the connection.
     * The method subtracts the time we spent waiting to stay under Event 4226 from the timeout before handing it to NIOSocket.
     * If the timeout expires, this method throws an IOException.
     * Only ConnectionChecker uses connectHard, the rest of LimeWire uses connectPlain.
     * 
     * @param host    The IP address we want to connect to, like "64.61.25.171"
     * @param port    The port number at that address we want to connect to, like 6346
     * @param timeout The time limit for both waiting to connect, and connecting
     * @return        A new NIOSocket object connected to the remote computer at the given address
	 */
	private static Socket connectHard(String host, int port, int timeout) throws IOException {

        // If the caller gave us a timeout of 0, interpret that to mean a timeout of forever
        if (timeout == 0) timeout = Integer.MAX_VALUE;

        // Block until one of our 4 half-open TCP sockets connects or fails
        long waitTime = System.currentTimeMillis();            // waitTime is the time now
        boolean waited = waitForSocketHard(timeout, waitTime); // Blocks until there's an open slot or the timeout expires, increments _socketsConnecting

        // waitForSocketHard returns true if we had 4 half-open connections it it had to wait
        if (waited) {

            // Change waitTime to be the amount of time we waited
            waitTime = System.currentTimeMillis() - waitTime;

            // Subtract the amount of time we waited from the timeout we'll give to connectAndRelease and NIOSocket
            timeout -= waitTime; // Now, NIOSocket has less time

            // If we waited for the full timeout and are out of time, throw an exception
            if (timeout <= 0) throw new IOException("timed out");
        }

        // Make a new NIOSocket object, connect it to the given IP address and port number, and return it
        return connectAndRelease(host, port, timeout); // Blocks until NIOSocket makes the connection, decrements _socketsConnecting
    }

	/**
     * Make a new NIOSocket object, and connect it to the given IP address and port number.
     * 
     * This method may block a lot longer than the given timeout.
     * First, we'll wait as long as it takes to stay under the Event 4226 limit.
     * Then, we'll give NIOSocket the length of the timeout to connect.
     * 
     * @param host    The IP address we want to connect to, like "64.61.25.171"
     * @param port    The port number at that address we want to connect to, like 6346
     * @param timeout How long NIOSocket will wait to make the connection, like 6 seconds
     * @return        A new NIOSocket object connected to the remote computer at the given address
	 */
	private static Socket connectPlain(String host, int port, int timeout) throws IOException {

        // Block until one of our 4 half-open sockets is connected or fails
		waitForSocket(); // Blocks until there's an open slot, increments _socketsConnecting

        // Make a new NIOSocket object, connect it to the given IP address and port number, and return it
		return connectAndRelease(host, port, timeout); // Blocks until NIOSocket makes the connection, decrements _socketsConnecting
	}

    /**
     * Make a new NIOSocket object, and connect it to the given IP address and port number.
     * We've already waited to stay under the Event 4226 limit.
     * Now, we'll wait for the given timeout while the remote computer accepts our connection.
     * 
     * This is the method in Sockets that calls into NIOSocket to make the connection.
     * 
     * @param host    The IP address we want to connect to, like "64.61.25.171"
     * @param port    The port number at that address we want to connect to, like 6346
     * @param timeout How long this thread will block waiting before giving up, like 6 seconds
     * @return        A new NIOSocket object connected to the remote computer at the given address
     */
	private static Socket connectAndRelease(String host, int port, int timeout) throws IOException {

        try {

            // Make a new NIOSocket object, connect it to the given IP address and port number, and return it
            SocketAddress addr = new InetSocketAddress(host, port); // Wrap the given IP address adn port into a Java SocketAddress object
            Socket ret = new com.limegroup.gnutella.io.NIOSocket(); // Make a new LimeWire NIOSocket object, this doesn't try to connect anything yet
            ret.connect(addr, timeout);                             // Call NIOSocket.connect(), this blocks until the connection is made
            return ret;                                             // Return the NIOSocket we made and connected

        // If any of that caused an exception, let it pass to our caller, but do this first
        } finally {

            // Record we have one fewer half-open TCP socket, and wake up threads waiting for an open slot
            releaseSocket();
        }
	}

	/** 
     * Connect to the given IP address and port number through the Socks 4 proxy server configured in program settings.
     * Communicates with the proxy server using the binary language of Socks 4.
     * Returns the socket with the proxy server's response read, so now it will be the remote computer responding.
     * 
     * @param host    The IP address we want to connect to, like "64.61.25.171"
     * @param port    The port number at that address we want to connect to, like 6346
     * @param timeout Not used
     * @return        A new NIOSocket object connected to the given IP address through our proxy server
	 */
	private static Socket connectSocksV4(String host, int port, int timeout) throws IOException {

        // Turn the given IP address string, like "64.61.25.171", into an array of 4 bytes
		byte[] hostBytes;
		try { hostBytes = InetAddress.getByName(host).getAddress(); } catch (UnknownHostException e) { throw new IOException("invalid host name"); }

        // Compose an array of 2 bytes with the port number in it
		byte[] portBytes = new byte[2];
		portBytes[0] = ((byte)(port >> 8)); // Shift port to the right 8 bytes to clip just the high bits
		portBytes[1] = ((byte)port);

        // Get the IP address and port number of the proxy server on our LAN that we have to connect through
		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();

        // Make a new NIOSocket object, and connect it to the proxy server
		Socket proxySocket = connectPlain(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout); // Doesn't work with an NIOSocket object

        // Get the streams we can use to read and write data to the proxy server
        OutputStream os = proxySocket.getOutputStream(); // This is the NIOSocket's NIOOutputStream object
        InputStream  in = proxySocket.getInputStream();  // This is the NIOSocket's NIOInputStream object

        // Write binary data to the proxy server
		os.write(0x04);      // Socks Version 4
		os.write(0x01);      // Command to connect
		os.write(portBytes); // Port number to connect to
		os.write(hostBytes); // IP address to connect to

        // Settings say we have to authenticate ourselves to the proxy server
		if (ConnectionSettings.PROXY_AUTHENTICATE.getValue()) {

            // Write the bytes of the user name from settings to the proxy server
		    os.write(ConnectionSettings.PROXY_USERNAME.getValue().getBytes());
        }

        // End the communication with a terminating 0 byte, and have the output stream actually send everything to the proxy server
		os.write(0x00); // Terminating 0 byte
		os.flush();

        /*
         * Now, we'll read the proxy server's response
         */

        // The version byte should be 0, but some Socks proxys answer 4
		int version = in.read(); // Read one byte the proxy server sent
		if (version != 0x00 && version != 0x04) {

            // The proxy server said something else, close the connection and throw an exception
			IOUtils.close(proxySocket);
			throw new IOException("Invalid version from socks proxy: " + version + " expected 0 or 4");
		}

        // The status byte is next, 0x5A means success
		int status = in.read(); // Read the next byte the proxy server sent
		if (status != 0x5A) {

            // The proxy server said something else, close the connection and throw an exception
			IOUtils.close(proxySocket);
			throw new IOException("Request rejected with status: " + status);
		}

        /*
         * The proxy server sends the port number and IP address it connected to next.
         * We don't check to make sure it matches the information we gave it.
         */

        // Read the next 6 bytes
		byte[] connectedHostPort    = new byte[2];
		byte[] connectedHostAddress = new byte[4];
		if (in.read(connectedHostPort) == -1 || in.read(connectedHostAddress) == -1) {

            // Unable to read the 6 bytes
            IOUtils.close(proxySocket);
			throw new IOException("Connection failed");
		}
        
        // We're connected to the given IP address through the proxy server, return the connection socket
        proxySocket.setSoTimeout(0); // Socket timeout doesn't work with NIOSocket
        return proxySocket;
	}

	/**
     * Connect to the given IP address and port number through the Socks 4 proxy server configured in program settings.
     * Communicates with the proxy server using the binary language of Socks 4.
     * Returns the socket with the proxy server's response read, so now it will be the remote computer responding.
     * 
     * @param host    The IP address we want to connect to, like "64.61.25.171"
     * @param port    The port number at that address we want to connect to, like 6346
     * @param timeout Not used
     * @return        A new NIOSocket object connected to the given IP address through our proxy server
	 */
	private static Socket connectSocksV5(String host, int port, int timeout) throws IOException {

        // Turn the given IP address string, like "64.61.25.171", into an array of 4 bytes
		byte[] hostBytes;
		try { hostBytes = InetAddress.getByName(host).getAddress(); } catch (UnknownHostException e) { throw new IOException("invalid host name"); }

        // Compose an array of 2 bytes with the port number in it
		byte[] portBytes = new byte[2];
		portBytes[0] = ((byte)(port >> 8));
		portBytes[1] = ((byte)port);

        // Get the IP address and port number of the proxy server on our LAN that we have to connect through
		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();

        // Make a new NIOSocket object, and connect it to the proxy server
		Socket proxySocket = connectPlain(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout);

        // Get the streams we can use to read and write data to the proxy server
        OutputStream os = proxySocket.getOutputStream(); // This is the NIOSocket's NIOOutputStream object
        InputStream  in = proxySocket.getInputStream();  // This is the NIOSocket's NIOInputStream object

        // Write binary data to the proxy server
		os.write(0x05); // Socks Version 5

        // Settings has authentication information for the proxy server
		if (ConnectionSettings.PROXY_AUTHENTICATE.getValue()) {

            // Tell the proxy server how many authentication methods we support, and which ones
			os.write(0x02); // The number of authentication methods we support
			os.write(0x00); // Authentication method: No authentication
			os.write(0x02); // Authentication method: User name and password

        // Settings doesn't have proxy server authentication information
		} else {

            // Tell the proxy server how many authentication methods we support, and which ones
			os.write(0x01); // The number of authentication methods we support
			os.write(0x00); // Authentication method: No authentication
		}

        // Have the output stream actually send all that data to the proxy server
		os.flush();

        /*
         * Now, we'll read the proxy server's response
         */

        // Make sure the proxy server responded first with Version 5
		int version = in.read(); // Read the first byte the proxy server sent us
		if (version != 0x05) {

            // It didn't
            IOUtils.close(proxySocket);
			throw new IOException("Invalid version from socks proxy: " + version + " expected 5");
		}

        // Next, the proxy server will tell us what kind of authentication we should use
		int auth_method = in.read();

        // No authentication
		if (auth_method == 0x00) {
            
            // Don't do anything

        // User name and password authentication
		} else if (auth_method == 0x02) {

            // Get our user name and password from settings
			String username = ConnectionSettings.PROXY_USERNAME.getValue();
			String password = ConnectionSettings.PROXY_PASS.getValue();
            
            // Tell it to the proxy server as binary data
			os.write(0x01);                     // Version of authentication protocol
			os.write((byte) username.length()); // Length of user name
			os.write(username.getBytes());      // User name
			os.write((byte) password.length()); // Length of password
			os.write(password.getBytes());      // Password
			os.flush(); // Send all that data to the proxy server

            // Read the version for the authentication protocol from the proxy server, it should be 1
			version = in.read();
			if (version != 0x01) {

                // The proxy server said something else
                IOUtils.close(proxySocket);
				throw new IOException("Invalid version for authentication: " + version + " expected 1");
			}

            // Read the status, 0 is success
			int status = in.read();
			if (status != 0x00) {

                // The proxy server said something else
                IOUtils.close(proxySocket);
				throw new IOException("Authentication failed with status: " + status);
			}
		}

        // Ask the proxy server to connect to the remote computer we actually want to talk to
		os.write(0x05);      // Version 5 again
		os.write(0x01);      // Command to connect, some other commands are 0x02 Bind, 0x03 UDP associate
		os.write(0x00);      // Reserved field, must be 0x00
		os.write(0x01);      // Address type: 0x01 is IPv4, 0x04 would be IPv6
		os.write(hostBytes); // IP address to connect to
		os.write(portBytes); // Port number to connect to
		os.flush(); // Send all that data to the proxy server

		// Read response, the Version byte should be 0x05
		version = in.read();
		if (version != 0x05) {

            // The proxy server said something else
            IOUtils.close(proxySocket);
			throw new IOException("Invalid version from socks proxy: " + version + " expected 5");
		}

        // Status byte, 0x00 is success
		int status = in.read();
		if (status != 0x00) {

            // The proxy server said something else
            IOUtils.close(proxySocket);
			throw new IOException("Request rejected with status: " + status);
		}
        
        /*
         * Skip over the rest of the proxy server's response.
         */

        // Read the response byte and address type
		in.read();                // Read the reserved byte to skip over it
		int addrType = in.read(); // The address type

        // Calculate how many more bytes we need to skip over
		int bytesToSkip = 0;
		if      (addrType == 1) bytesToSkip = 4         + 2; // 4 bytes of an IP address
        else if (addrType == 3) bytesToSkip = in.read() + 2; // Domain name, read the length
		else if (addrType == 4) bytesToSkip = 16        + 2; // 16 bytes of an IPv6 address

        // Read that number of bytes to skip over them
		for (int i = 0; i < bytesToSkip; i++) {

            // Read a byte, and make sure the input stream doesn't reach EOF, the end of the stream
			if (in.read() == -1) throw new IOException("Connection failed");
		}

        // We're connected to the given IP address through the proxy server, return the connection socket
        proxySocket.setSoTimeout(0); // Socket timeout doesn't work with NIOSocket
        return proxySocket;
	}

	/**
     * Connect to the given IP address and port number through the HTTP proxy server configured in program settings.
     * Communicates with the proxy server using text HTTP headers.
     * Returns the socket with the proxy server's response read, so now it will be the remote computer responding.
     * 
     * @param host    The IP address we want to connect to, like "64.61.25.171"
     * @param port    The port number at that address we want to connect to, like 6346
     * @param timeout Not used
     * @return        A new NIOSocket object connected to the given IP address through our proxy server
	 */
	private static Socket connectHTTP(String host, int port, int timeout) throws IOException {

        // Compose text like "CONNECT 64.61.25.171:6346 HTTP/1.0" followed by a blank line
		String connectString = "CONNECT " + host + ":" + port + " HTTP/1.0\r\n\r\n";

        // Get the IP address and port number of the proxy server on our LAN that we have to connect through
		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();

        // Make a new NIOSocket object, and connect it to the proxy server
		Socket proxySocket = connectPlain(proxyHost, proxyPort, timeout);
		proxySocket.setSoTimeout(timeout); // Doesn't work since proxySocket is actually a NIOSocket object

        // Get the streams we can use to read and write data to the proxy server
        OutputStream os = proxySocket.getOutputStream(); // This is the NIOSocket's NIOOutputStream object
		InputStream  in = proxySocket.getInputStream();  // This is the NIOSocket's NIOInputStream object

		// Send the connection text to the proxy server
		os.write(connectString.getBytes());
		os.flush();

        // Read the proxy server's response
		ByteReader reader = new ByteReader(in);
		String line = reader.readLine();

		// Look for a status code of 200 from the proxy server
		if (line == null || line.indexOf("200") == -1) {

            // The proxy server didn't give us this code, the connection didn't work
            IOUtils.close(proxySocket); // Close the socket's input and output streams, and then the socket itself
			throw new IOException("HTTP connection failed");
        }

        /*
         * In response, the proxy server sent us a group of HTTP headers.
         * Each header ends "\r\n", and the last header is a blank line, just "\r\n".
         * We don't need to look at any of these headers, but we do need to move past them.
         */

        // Loop until line is a blank line
		while (!line.equals("")) {

            // Read the next line of text from the proxy server
			line = reader.readLine();
            
            // The stream reported EOF, end of file
            if (line == null) {

                // Close our connection to the proxy server and throw an exception
                IOUtils.close(proxySocket); // Close the socket's input and output streams, and then the socket itself
                throw new IOException("end of stream");
            }
		}
        
        // We're connected to the given IP address through the proxy server, return the connection socket
		proxySocket.setSoTimeout(0); // Socket timeout doesn't work with NIOSocket
		return proxySocket;
	}

    /**
     * The number of connection attempts we made.
     * This count includes both computers we connected to, and connections we attempted that were never made.
     * 
     * Only ConnectionManager.allowConnection() uses this.
     * 
     * @return The number of times connectPlain() and connectHard() have tried to connect to remote computers
     */
	public static int getAttempts() {
        
        // Return the number we increment before calling connectPlan() and connectHard()
	    return _attempts;
	}

    /**
     * Reset the count of connection attempts back to 0.
     * Only ConnectionManager.disconnect() does this.
     */
	public static void clearAttempts() {

        // Set it back to 0
	    _attempts = 0;
	}

    /**
     * Blocks until it's safe to open another TCP socket connection in Windows.
     * If we already have 4 half-open connections, starting a 5th might make cause event 4226.
     * The thread that calls this method will block here until one of our 4 half-open connections is connected or discarded.
     * 
     * Hard means the timeout applies to the time waiting for Event 4226 and the time NIOSocket takes to make the connection.
     * Only connectHard() calls this, and the timeout will never be 0.
     * 
     * Returns when we can connect.
     * Throws an IOException if the given timeout expired and we still can't connect.
     * 
     * Returns true if we had to wait.
     * Returns false if we didn't have to wait.
     * 
     * @param  timeout     The amount of time we're willing to wait
     * @param  now         The time it is now, from System.currentTimeMillis()
     * @return             True if the 4226 limit made us wait before trying to open our new connection.
     *                     False if we didn't have to wait.
     * @throws IOException If the timeout expires while we're waiting for one of our 4 half-open connections to connect or fail
     */
	private static boolean waitForSocketHard(int timeout, long now) throws IOException {

        // We only have to worry about the Event 4226 limit if we're running on Windows XP
	    if (!CommonUtils.isWindowsXP()) return false;

        /*
         * TODO:kfaaborg Actually, it's not Windows XP, it's just Windows XP with Service Pack 2.
         * And, it's not just Windows XP, it's every service pack and every version of Windows Microsoft released after summer 2005.
         */

        // Only one thread can enter any one of these blocks at a time
	    long timeoutTime = now + timeout; // Set timeoutTime to the time when the timeout will expire
	    boolean ret = false;              // We haven't had to wait yet
	    synchronized (Sockets.class) {

            // Loop until _socketsConnecting is 4 or less
	        while (_socketsConnecting >= MAX_CONNECTING_SOCKETS) {

                // _socketsConnecting is 5 or more

                // We ran out of time, leave by throwing an exception
                if (timeout <= 0) throw new IOException("timed out :(");

	            try {

                    // Wait here until releaseSocket() below calls Sockets.class.notifyAll()
                    ret = true; // We did have to wait, we'll return true
	                Sockets.class.wait(timeout); // Wait here until another thread calls Sockets.class.notifyAll(), or the timeout expires
                    timeout = (int)(timeoutTime - System.currentTimeMillis()); // How much waiting time we have left

                // Another thread called Sockets.class.interrupt(), wrap it in an IOException and throw it
	            } catch (InterruptedException e) { throw new IOException(e.getMessage()); }
	        }

            // Record that we're going to have one more half-open TCP connection
	        _socketsConnecting++;
	    }

        // Return if we waited in the while loop or not
        return ret;
	}

	/**
     * Blocks until it's safe to open another TCP socket connection in Windows.
     * If we already have 4 half-open connections, starting a 5th might make cause event 4226.
     * The thread that calls this method will block here until one of our 4 half-open connections is connected or discarded.
     * Returns when we can connect.
	 */
	private static void waitForSocket() throws IOException {

        // We only have to worry about the Event 4226 limit if we're running on Windows XP
		if (!CommonUtils.isWindowsXP()) return;

        // Only one thread can enter any one of these blocks at a time
		synchronized (Sockets.class) {

            // Loop until _socketsConnecting is 4 or less
			while (_socketsConnecting >= MAX_CONNECTING_SOCKETS) {

				try {

                    // Wait here until releaseSocket() below calls Sockets.class.notifyAll()
					Sockets.class.wait();

                // Another thread called Sockets.class.interrupt(), wrap it in an IOException and throw it
				} catch (InterruptedException ix) { throw new IOException(ix.getMessage()); }
			}

            // Record that we're going to have one more half-open TCP connection
			_socketsConnecting++;
		}
	}

    /**
     * Decrements _socketsConnecting and wakes up the threads sleeping in waitForSocketHard() and waitForSocket().
     * connectAndRelease() calls this after its new NIOSocket has connected.
	 */
	private static void releaseSocket() {

        // We only have to worry about the Event 4226 limit if we're running on Windows XP
	    if (!CommonUtils.isWindowsXP()) return;

        // Only one thread can enter one block like this at a time
	    synchronized (Sockets.class) {

            // Record we've got one fewer connection half-open TCP connection
	        _socketsConnecting--;

            // Wake up all the threads sleeping in waitForSocketHard() and waitForSocket()
	        Sockets.class.notifyAll(); // Now they'll loop and find _socketsConnecting is less than MAX_CONNECTING_SOCKETS
	    }
	}

    /**
     * The limit of half-open TCP connections that LimeWire is keeping itself under.
     * Threads that call methods to connect in this class block longer because we're waiting to stay under this limit.
     * 
     * This limit is related to Event 4226 on Windows XP with Service Pack 2, and later versions of Microsoft Windows.
     * If we're running on Windows XP, this method will return 4.
     * On other operating systems, it returns 2147483647 to indicate we're not keeping track of a limit.
     * 
     * Only ConnectionManager.adjustConnectionFetchers() calls this.
     * 
     * @return The limit of half-open TCP connections that LimeWire is keeping itself under.
     */
	public static int getNumAllowedSockets() {

        // We only have to worry about the Event 4226 limit if we're running on Windows XP
		if (CommonUtils.isWindowsXP()) {

            // Return 4, our limit on Windows
            return MAX_CONNECTING_SOCKETS;

        // We're not running on Windows XP, and are not making threads block to keep under this limit
        } else {

            // Return 2147483647 to indicate there is no limit
			return Integer.MAX_VALUE;
        }
	}
}
