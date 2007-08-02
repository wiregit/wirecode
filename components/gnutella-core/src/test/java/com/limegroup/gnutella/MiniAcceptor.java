package com.limegroup.gnutella;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.handshaking.HandshakeResponder;

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
    private Connection c=null;
    private boolean done=false;
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
		this(ProviderHacks.getHandshakeResponderFactory().createUltrapeerHandshakeResponder("localhost"), port);
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
            ss.setReuseAddress(true);
            Socket s=ss.accept();
            //Technically "GNUTELLA " should be read from s.  Turns out that
            //out implementation doesn't care;
            Connection c=new Connection(s);
            c.initialize(null, properties, 1000);
            ss.close();
            synchronized (lock) {
                this.c=c;
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

