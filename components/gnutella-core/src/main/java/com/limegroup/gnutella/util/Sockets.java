package com.limegroup.gnutella.util;

import java.net.*;
import java.nio.channels.*;
import java.nio.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.ConnectionSettings;


/**
 * Provides socket operations that are not available on all platforms,
 * like connecting with timeouts and settings the SO_KEEPALIVE option.
 * Obsoletes the old SocketOpener class.
 */
public class Sockets {

    private static Selector _selector;

    private static final Object CONNECT_LOCK = new Object();

    private static final Object SELECTOR_LOCK = new Object();

    private static final LinkedList PENDING_SOCKETS = new LinkedList();

    static {
        start();
    }


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
            //Call socket.setKeepAlive(on) using reflection.  See below for
            //any explanation of why reflection must be used.
            try {
                socket.setKeepAlive(on);
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
        if (CommonUtils.isJava14OrLater() &&
            ConnectionSettings.USE_NIO.getValue()) {
            //a) Non-blocking IO using Java 1.4. 
            InetSocketAddress addr = new InetSocketAddress(host, port);

            // make sure the address was resolved to an InetAddress
            if (addr.isUnresolved())
                throw new IOException("Couldn't resolve address");
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(addr);
            SocketData data = new SocketData(sc);
            synchronized(PENDING_SOCKETS) {
                PENDING_SOCKETS.add(data);
            }
            
            if(_selector == null) {
                // wait for the selector to be initialized
                synchronized(SELECTOR_LOCK) {
                    try {
                        while(_selector == null) {
                            SELECTOR_LOCK.wait();
                        }
                    } catch(InterruptedException e) {
                        ErrorService.error(e);
                    }
                }
            }
            _selector.wakeup();
            while(!sc.isConnected()) {
                synchronized(CONNECT_LOCK) {
                    try {
                        if(timeout > 0) {
                            CONNECT_LOCK.wait(timeout);
                            if(data.errorConnecting()) {
                                //data.getException().printStackTrace();
                                throw data.getException();
                            }
                            if(!sc.isConnected()) {
                                throw new IOException("could not connect socket");
                            } 
                            break;
                        } else {
                            CONNECT_LOCK.wait();
                        }
                    } catch(InterruptedException e) {
                        // this should never happen
                        ErrorService.error(e);
                    }
                }
            }
            System.out.println("Sockets::RETURNING SOCKET"); 
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

    
    private static void start() {
        // don't do anything if we're not using NIO
        if(!CommonUtils.isJava14OrLater() || !ConnectionSettings.USE_NIO.getValue()) {
            return;
        }
                
        Thread selectorThread = new Thread(new NIOSocketConnector(), "NIO socket connector");
        selectorThread.setDaemon(true);
        selectorThread.start();        
    }

    /**
     * Utility class that stores whether or not any errors occurred connecting to the
     * desired host.
     */
    private static class SocketData {

        private final SocketChannel SOCKET_CHANNEL;

        private IOException _errorConnecting;

        SocketData(SocketChannel sc) {
            SOCKET_CHANNEL = sc;
        }

        private void errorConnecting(IOException e) {
            _errorConnecting = e;
        }

        private boolean errorConnecting() {
            return _errorConnecting != null;
        }

        private IOException getException() {
            return _errorConnecting;
        }

        SocketChannel channel() {
            return SOCKET_CHANNEL;
        }
    }

    /**
     * Helper class for outgoing connections that registers new channels with a 
     * selector and completes outgoing socket connections.
     */
    private static class NIOSocketConnector implements Runnable {
        public void run() {
            try {
                _selector = Selector.open();
                synchronized(SELECTOR_LOCK) {
                    SELECTOR_LOCK.notify();
                }
            } catch(IOException e) {
                // this hopefully should not happen, although we'll at least find 
                // out if and why it could
                ErrorService.error(e);
                return;
            }

            
            while(true) {
                try {          
                    int readyKeys = _selector.select();
                    if(readyKeys > 0) {
                        processSelectedKeys();
                    }
                    
                    processPendingSockets();
                } catch(IOException e) {
                }
            }                        
        }

        /**
         * Processes any keys that are ready for an attempt at fully establishing
         * their TCP connections.
         */
        private void processSelectedKeys() {
            for (Iterator iter = _selector.selectedKeys().iterator(); iter.hasNext();) {

                // Retrieve the next key and remove it from the set
                SelectionKey key = (SelectionKey)iter.next();
                iter.remove();

                // if the key is not either done connecting or in the process of 
                // connecting, then keep going -- this should never occur
                if(!key.isConnectable()) {
                    continue;
                }
                SocketChannel sc = (SocketChannel)key.channel();
                
                // Attempt to complete the connection sequence
                try {
                    if (sc.finishConnect()) {
                        System.out.println("Sockets::processSelectedKeys::finished connecting"); 
                        key.cancel();
                        synchronized(CONNECT_LOCK) {
                            CONNECT_LOCK.notify();
                        }
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                    SocketData data = (SocketData)key.attachment();
                    data.errorConnecting(e);
                    //((SocketData)key.attachment()).errorConnecting(e);
                    synchronized(CONNECT_LOCK) {
                        CONNECT_LOCK.notify();
                    }
                    NetworkUtils.close(sc.socket());
                    try {                        
                        sc.close();
                    } catch(IOException ioe) {
                        // nothing to do
                    }
                }
            }
        }

        /**
         * Loops through any sockets that have yet to be connected and registers 
         * them for connection events.
         */
        private void processPendingSockets() {
            synchronized (PENDING_SOCKETS) {
                while (PENDING_SOCKETS.size() > 0) {
                    SocketData data = (SocketData)PENDING_SOCKETS.removeFirst(); 
                    SocketChannel channel = data.channel();
                    try {
                        
                        // Register the channel with the selector, indicating
                        // interest in connection completion and attaching the
                        // target object so that we can get the target back
                        // after the key is added to the selector's
                        // selected-key set
                        channel.register(_selector, SelectionKey.OP_CONNECT, data);

                    } catch (IOException e) {
                        e.printStackTrace();
                        data.errorConnecting(e);
                        synchronized(CONNECT_LOCK) {
                            CONNECT_LOCK.notify();
                        }
                        NetworkUtils.close(channel.socket());
                        
                        try {
                            channel.close();
                        } catch(IOException ioe) {
                            // nothing to do
                        }
                    }
                    
                }
            }
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
	 * hang around until the connection really times out.  That means frequent calls
	 * to this may result in numerous threads waiting to die.<p>
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
