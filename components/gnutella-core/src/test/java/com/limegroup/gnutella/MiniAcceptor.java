package com.limegroup.gnutella;

import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;

import java.net.*;
import java.nio.channels.ServerSocketChannel;
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
    private Object lock=new Object();
    private Connection conn = null;
    private boolean done = false;
    private int port;
    private IOException error=null;

    private HandshakeResponder properties;
        
    /** Starts the listen socket on port 6346 without blocking. */
    public MiniAcceptor(HandshakeResponder properties) {
        this(properties, 6346);
    }

    /** Starts the listen socket without blocking. */
    public MiniAcceptor(HandshakeResponder properties, int port) {
		//ConnectionSettings.NUM_CONNECTIONS.setValue(3);
        this.properties=properties;
        this.port=port;
        Thread runner=new Thread(this);
        runner.start();
        Thread.yield();  //hack to make sure runner creates socket
    }

    /** Starts the listen socket without blocking. */
    public MiniAcceptor(int port) {
		this(new UltrapeerHandshakeResponder("localhost"), port);
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
            return conn;
        }
    }
        
    public IOException getError() {
        return error;
    }

    /** Don't call.  For internal use only. */
    public void run() {
        ServerSocket ss = null;
        try {
            if(CommonUtils.isJava14OrLater() &&
               ConnectionSettings.USE_NIO.getValue()) {
               ServerSocketChannel ssc = ServerSocketChannel.open();
               ssc.configureBlocking(true);
               ssc.socket().bind(new InetSocketAddress(port));
               ss = ssc.socket();  
            } else {
                ss = new ServerSocket(port);
                ss.setReuseAddress(true);
            }
            Socket s = ss.accept();
            
            //Technically "GNUTELLA " should be read from s.  Turns out that
            //our implementation doesn't care;
            Connection c = new Connection(s, properties);
            c.initialize();
            ss.close();
            synchronized (lock) {
                this.conn=c;
                done=true;
                lock.notify();
            } 
        } catch (IOException e) {
            if (ss==null) {
                ErrorService.error(e);
            }
            error=e;                  //Record for later.
            synchronized (lock) {
                done=true;
                lock.notify();
            } 
        }
    }
}

