package com.limegroup.gnutella;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.stubs.ConnectionListenerStub;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

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
    IOException error=null;
    
    ServerSocketChannel listener;
    ConnectionListener observer;
    HandshakeResponder properties;
        
    /** Starts the listen socket on port 6346 without blocking. */
    public MiniAcceptor(HandshakeResponder properties) 
            throws IOException {
        this(properties, 6346);
    }

    /** Starts the listen socket without blocking. */
    public MiniAcceptor(HandshakeResponder properties, int port) 
            throws IOException {
        this (new ConnectionListenerStub(), properties, port);
    }


    public MiniAcceptor(ConnectionListener observer,
                        HandshakeResponder properties, 
                        int port) throws IOException {
        this.observer=observer;
        this.properties=properties;
        
        //Listen on port
        listener=ServerSocketChannel.open();
        listener.configureBlocking(true);
        try {
            listener.socket().setReuseAddress(true);
        } catch (SocketException ignore) { }
        listener.socket().bind(new InetSocketAddress(port));

        Thread runner=new Thread(this);
        runner.start();
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
        boolean bound=false;
        try {
            //Accept connection and store.  Technically "GNUTELLA " should be
            //read from s.  Turns out that out implementation doesn't care;
            Socket s=listener.accept().socket();
            Connection c=new Connection(s, properties);
            c.initialize(observer);            
            synchronized (lock) {
                this.c=c;
                done=true;
                lock.notify();
            } 
        } catch (IOException e) {
            error=e;                  //Record for later.
            synchronized (lock) {
                done=true;
                lock.notify();
            } 
        } finally {
            //Kill listening socket.
            if (listener!=null) {
                try { listener.close(); } catch (IOException e) { }
                try { listener.socket().close(); } catch (IOException e) { }
            }
        }
    }
}
