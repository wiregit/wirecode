package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.settings.ConnectionSettings;


/**
 * Provides socket operations that are not available on all platforms,
 * like connecting with timeouts and settings the SO_KEEPALIVE option.
 * Obsoletes the old SocketOpener class.
 */
public class Sockets {

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
        if (CommonUtils.isJava13OrLater()) {
            try {
                socket.setKeepAlive(on);
                return true;
            } catch (SocketException e1) {
                // all we can do is try to set the keep alive
            }
            return false;
        }
        return false;
    }

    /**
     * Connects and returns a socket to the given host, with a timeout.
     *
     * @param host the address of the host to connect to
     * @param port the port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
     *  or 0 for no timeout.
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
        if (ConnectionSettings.USE_NIO.getValue()) {
            //a) Non-blocking IO using Java 1.4. Conceptually, this code
            //   does the following:
            //      SocketAddress addr=new InetSocketAddress(host, port);
            //      Socket ret=new Socket();
            //      ret.connect(addr, timeout);
            //      return ret;
            //   Unfortunately that causes compile errors on older versions
            //   of Java.  Worse, it may cause runtime errors if class loading
            //   is not done lazily.  (See chapter 12.3.4 of the Java Language
            //   Specification.)  So we use reflection.
            InetSocketAddress addr = new InetSocketAddress(host, port);

            // make sure the address was resolved to an InetAddress
            if (addr.isUnresolved())
                throw new IOException("Couldn't resolve address");
            Socket sock = SocketChannel.open().socket();

            // use the blocking connect in this case
            sock.connect(addr, timeout);
            return sock;
        }
     
        if (timeout!=0) {
            //b) Emulation using threads
            return (new SocketOpener(host, port)).connect(timeout);
        } else {
            //c) No timeouts
            return new Socket(host, port);
        }
    }


    /**
     * Connects and returns a socket to the given host without blocking.
     *
     * @param host the address of the host to connect to
     * @param port the port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
     *  or 0 for no timeout.
     * @return the connected Socket
     * @throws IOException the connections couldn't be made in the 
     *  requested time
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connectNIO(String host, int port, int timeout) 
		throws IOException {

        // TODO::implement the timeout in this case??
        if(!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("port out of range: "+port);
        }
        if (ConnectionSettings.USE_NIO.getValue()) {
            InetSocketAddress addr = new InetSocketAddress(host, port);

            // make sure the address was resolved to an InetAddress
            if (addr.isUnresolved())
                throw new IOException("Couldn't resolve address");
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(addr);

            return sc.socket();
        }
     
        if (timeout!=0) {
            //b) Emulation using threads
            return (new SocketOpener(host, port)).connect(timeout);
        } else {
            //c) No timeouts
            return new Socket(host, port);
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
	 * implemented in a similar way as Wayne Conrad's SocketOpener class, except
	 * that it doesn't use Thread.stop() or interrupt().  Rather opening threads
	 * hang around until the connection really times out.  That means frequent 
	 * calls to this may result in numerous threads waiting to die.<p>
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
		
		public SocketOpener(String host, int port) {
			if((port & 0xFFFF0000) != 0) {
				throw new IllegalArgumentException("port out of range: "+port);
			} 
			this.host=host;
			this.port=port;
		}
		
		/** 
		 * Returns a new socket to the given host/port.  If the socket couldn't
		 * be established withing timeout milliseconds, throws IOException.  If
		 * timeout==0, no timeout occurs.  If this thread is interrupted while
		 * making connection, throws IOException.
		 *
		 * @requires connect has only been called once, no other thread calling
		 *  connect.  Timeout must be non-negative.  
		 */
		public synchronized Socket connect(int timeout) 
            throws IOException {
			//Asynchronously establish socket.
			Thread t = new Thread(new SocketOpenerThread(), "SocketOpener");
			t.setDaemon(true);
			Assert.that(socket==null, "Socket already established w.o. lock.");
			t.start();
			
			//Wait for socket to be established, or for timeout.
			try {
				this.wait(timeout);
			} catch (InterruptedException e) {
				if (socket==null)
					timedOut=true;
				else {
                    NetworkUtils.close(socket);
                }
				throw new IOException("socket timed out");
			}
			
			//a) Normal case
			if (socket!=null) {
				return socket;
			} 
			//b) Timeout case
			else {            
				timedOut=true;
				throw new IOException("SocketOpener timed out");
			}            
		}
		
		private class SocketOpenerThread implements Runnable {
			public void run() {
				try {
					Socket sock=null;
					try {
						sock=new Socket(host, port);
					} catch (IOException e) { }                
					
					synchronized (SocketOpener.this) {
						if (timedOut && sock!=null) {
                            NetworkUtils.close(sock);
						} else {
							socket=sock;   //may be null
							SocketOpener.this.notify();
						}
					}
				} catch(Throwable t) {
					ErrorService.error(t);
				}
			}
		}
	}

}
