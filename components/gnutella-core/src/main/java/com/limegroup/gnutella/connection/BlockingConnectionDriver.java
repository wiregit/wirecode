package com.limegroup.gnutella.connection;

import java.io.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;


/**
 * Encapsulates all threads for reading and writing messages with blocking IO.
 */
class BlockingConnectionDriver extends ConnectionDriver {
    ///////////////////////////// ConnectionListener Methods //////////////////

    public void initialized(Connection c) { 
        super.initialized(c);
        //Eventually this will be done BEFORE handshaking is over.
        Thread reader=new ReaderThread(c);
        Thread writer=new WriterThread(c);
        reader.setDaemon(true);
        writer.setDaemon(true);
        reader.start();
        writer.start();
    }

    public void needsWrite(Connection c) {
        //TODO: I'm not comfortable using c's monitor for writes.  What if
        //another thread is notifying c?  Then there could be lots of spurious
        //wakeups.
        super.needsWrite(c);
        synchronized(c) {
            c.notifyAll();
        }        
    }

    /** Repeatedly reads data from a connection until the connection closes. */
    class ReaderThread extends Thread {
        private Connection _connection;
        ReaderThread(Connection c) {
            this._connection=c;
        }

        public void run() {
            while (_connection.isOpen()) {
                //System.out.println(System.currentTimeMillis()
                //                   +" read "+_connection);
                _connection.read();
            }
        }
    }

    /** Repeatedly writes data to a connection until the connection closes. */
    class WriterThread extends Thread {
        private Connection _connection;
        WriterThread(Connection c) {
            this._connection=c;
        }

        public void run() {
            while (_connection.isOpen()) {
                //Wait for queued data.  Notify() called by needsWrite(), 
                //which is called from within ManagedConnection.write(m).
                synchronized (_connection) {
                    while (! _connection.hasQueued()) {
                        try { 
                            _connection.wait();
                        } catch (InterruptedException ignore) { }
                    }
                }
                //Write.  Ok if no data is available.  Do NOT hold monitor.
                //System.out.println(System.currentTimeMillis()
                //                   +" write "+_connection);
                _connection.write();
            }
        }
    }
}
