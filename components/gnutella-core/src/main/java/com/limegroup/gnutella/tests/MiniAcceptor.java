package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import java.net.*;
import java.io.*;

/**
 * A handy class for creating incoming connections for in-process tests.  
 */
public class MiniAcceptor implements Runnable {
    Object lock=new Object();
    Connection c=null;
    boolean done=false;

    HandshakeResponder properties;
        
    /** Starts the listen socket without blocking. */
    public MiniAcceptor(HandshakeResponder properties) {
        this.properties=properties;
        Thread runner=new Thread(this);
        runner.start();
        Thread.yield();  //hack to make sure runner creates socket
    }

    /** Blocks until a connection is available, and returns it. 
     *  Returns null if something went awry. */
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
        
    /** Don't call.  For internal use only. */
    public void run() {
        try {
            ServerSocket ss=new ServerSocket(6346);
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
            synchronized (lock) {
                done=true;
                lock.notify();
            } 
        }
    }
}
