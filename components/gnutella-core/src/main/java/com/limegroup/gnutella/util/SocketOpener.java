package com.limegroup.gnutella.util;

import java.net.Socket;
import java.io.IOException;
import com.limegroup.gnutella.Assert;

/** 
 * Opens Java sockets with a bounded timeout.  Typical use:
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
 * to this may result in numerous threads waiting to die.  The next release of
 * Java ("Merlin") will have timeouts on connect, so this won't be needed.<p>
 *
 * This class is currently NOT thread safe.  Currently connect() can only be 
 * called once.
 */
public class SocketOpener {
    private String host;
    private int port;
    /** The established socket, or null if not established OR couldn't be
     *  established.. Notify this when socket becomes non-null. */
    private Socket socket=null;
    /** True iff the connecting thread should close the socket if/when it
     *  is established. */
    private boolean timedOut=false;

    public SocketOpener(String host, int port) {
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
        Thread t=new SocketOpenerThread();
        t.start();
        
        //Wait for socket to be established, or for timeout.
        Assert.that(socket==null, "Socket already established w.o. lock.");
        try {
            this.wait(timeout);
        } catch (InterruptedException e) {
            if (socket==null)
                timedOut=true;
            else
                try { socket.close(); } catch (IOException e2) { }
            throw new IOException();
        }

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

    private class SocketOpenerThread extends Thread {
        public void run() {
            Socket sock=null;
            try {
                sock=new Socket(host, port);
            } catch (IOException e) { }                

            synchronized (SocketOpener.this) {
                if (timedOut && sock!=null)
                    try { sock.close(); } catch (IOException e) { }
                else {
                    socket=sock;   //may be null
                    SocketOpener.this.notify();
                }
            }
        }
    }
}
