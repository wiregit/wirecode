package com.limegroup.gnutella.util;

import java.net.*;
import java.io.*;
import com.limegroup.gnutella.*;

/** 
 * Opens a Java URLConnection with a bounded timeout.  Typical use:
 *
 * <pre>
 *    try {
 *        URLConnection url=(new URLOpener(host, port)).connect(timeout);
 *    } catch (IOException e) {
 *        System.out.println("Couldn't connect in time.");
 *    }
 * </pre>
 *
 * This is basically a hack to work around limitations in the URL/URLConnection
 * classes.  It is implemented with an extra thread much like SocketOpener.
 * That means frequent calls to this may result in numerous threads waiting to die.
 * The next release of Java ("Merlin") will have timeouts on connect, so this
 * won't be needed.<p>
 *
 * <b>This class is currently NOT thread safe.  Currently connect() can only be 
 * called once.</b> 
 */
public class URLOpener {
    /** The URL we are trying to open. */
    private URL url;
    /** The established connection, or null if not established OR couldn't be
     *  established.  Notify this when socket becomes non-null. */
    private URLConnection connection=null;
    /** True iff the connecting thread should close the socket if/when it
     *  is established. */
    private boolean timedOut=false;

    /** Creates a URLOpener that will open the given url when connect is
     *  called. */
    public URLOpener(URL url) {
        this.url=url;
    }

    /** 
     * Returns a new URLConnection to this' url.  If the connection couldn't be
     * established within timeout milliseconds, throws IOException.  If
     * timeout==0, no timeout occurs.  If this thread is interrupted while
     * making the connection, throws IOException.
     *
     * @requires connect has only been called once, no other thread calling
     *  connect.  Timeout must be non-negative.  
     */
    public synchronized URLConnection connect(int timeout) 
            throws IOException {
        //Asynchronously establish connection.
        Thread t = new Thread(new URLOpenerThread());
        t.start();
        
        //Wait for connection to be established, or for timeout.
        Assert.that(connection==null, "Connection already established w.o. lock.");
        try {
            this.wait(timeout);
        } catch (InterruptedException e) {
            if (connection==null)
                timedOut=true;
            else
                close(connection);
            throw new IOException();
        }

        //a) Normal case
        if (connection!=null) {
            return connection;
        } 
        //b) Timeout case
        else {            
            timedOut=true;
            throw new IOException();
        }            
    }

    private class URLOpenerThread implements Runnable {
        public void run() {
            try {
                URLConnection conn=null;
                try {
                    conn = url.openConnection();
                } catch (IOException e) { }                
                
                synchronized (URLOpener.this) {
                    if (timedOut && conn!=null)
                        close(conn);
                    else {
                        connection=conn;   //may be null
                        URLOpener.this.notify();
                    }
                }
            } catch(Throwable t) {
                RouterService.error(t);
            }
        }
    }

    /** If conn is an HttpURLConnection, calls the disconnect() method.
     *      @modifies conn */
	private static void close(URLConnection conn) {
		if (conn instanceof HttpURLConnection) {
			((HttpURLConnection)conn).disconnect();
		}
	}
}
