package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import java.net.*;
import java.io.*;

/**
 * A handy class for creating incoming connections for in-process tests.  
 * Typical use: 
 *
 * <pre>
 * MiniAcceptor acceptor=new MiniAcceptor(inProperties, 6346);
 * Connection out=new Connection("localhost", 6346);
 * out.initialize();
 * Connection in=acceptor.accept();
 *
 * out.send(..);
 * in.receive();
 * in.send(..);
 * out.receive(..);
 * </pre>
 */
public class MiniAcceptor implements Runnable {
    Object lock=new Object();
    Connection c=null;
    boolean done=false;
    int port;
    IOException error=null;

    HandshakeResponder properties;
        
    /** Starts the listen socket on port 6346 without blocking. */
    public MiniAcceptor(HandshakeResponder properties) {
        this(properties, 6346);
    }

    /** Starts the listen socket without blocking. */
    public MiniAcceptor(HandshakeResponder properties, int port) {
        this.properties=properties;
        this.port=port;
        Thread runner=new Thread(this);
        runner.start();
        Thread.yield();  //hack to make sure runner creates socket
    }

    /** Blocks until a connection is available, and returns it. 
     *  Returns null if something went awry.  In this case, you 
     *  can get the exception via getError.  Bad design, but 
     *  exists for backwards compatibility. */
    public Connection accept() {
        synchronized (lock) {
            while (! done) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return c;
        }
    }
        
    public IOException getError() {
        return error;
    }

    /** Don't call.  For internal use only. */
    public void run() {
        ServerSocket ss=null;
        try {
            ss=new ServerSocket(port);
            Socket s=ss.accept();
            //Technically "GNUTELLA " should be read from s.  Turns out that
            //out implementation doesn't care;
            Connection c=new Connection(s, properties);
            c.initialize();
            ss.close();
            synchronized (lock) {
                this.c=c;
                done=true;
                lock.notify();
            } 
        } catch (IOException e) {
            if (ss==null) {
                System.err.println("Couldn't listen to port "+port);
                e.printStackTrace();  //Couldn't listen?  Serious.
            }
            error=e;                  //Record for later.
            synchronized (lock) {
                done=true;
                lock.notify();
            } 
        }
    }
}
