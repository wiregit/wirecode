package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ErrorService;
import com.sun.java.util.collections.Collections;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.List;

/**
 * This class runs a thread that continually checks all connections for any
 * pending messages, notifying those connections to write their pending
 * messages out to their respective channels.
 */
public final class NIOMessageWriteManager implements Runnable {

    private static final NIOMessageWriteManager INSTANCE =
        new NIOMessageWriteManager();

    /** 
     * A lock for pending messages status.
     */
    private final Object MESSAGE_LOCK = new Object();        
        
    private final List PENDING_WRITERS = 
        Collections.synchronizedList(new LinkedList());
    
    public static NIOMessageWriteManager instance() {
        return INSTANCE;
    }
    
    private NIOMessageWriteManager() {
        Thread managerThread = new Thread(this, "NIO manager thread");
        managerThread.setDaemon(true);
        managerThread.start();
    }
    
    public void addConnection(Connection conn) {
        PENDING_WRITERS.add(conn);
    }
    
    public void removeConnection(Connection conn) {
        PENDING_WRITERS.remove(conn);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#write(com.limegroup.gnutella.messages.Message)
     */
    public void messageAdded(Connection conn) {
        synchronized (MESSAGE_LOCK) {
            MESSAGE_LOCK.notify();
        }
    }
    
    public void run() {
        //Exceptions are only caught to set the _runnerDied variable
        //to make testing easier.  For non-IOExceptions, Throwable
        //is caught to notify ErrorService.
        try {
            while (true) {
                waitForMessages();
                writeMessages();
            }                
        } catch (IOException e) {
        } catch(Throwable t) {    
            ErrorService.error(t);
        }
    }
    
    /** 
     * Wait until the queue is (probably) non-empty or closed. 
     * @throws IOException this was closed while waiting
     */
    private void waitForMessages() throws IOException {
        //The synchronized statement is outside the while loop to
        //protect _queued.
        synchronized (MESSAGE_LOCK) {
            while (PENDING_WRITERS.isEmpty()) {        
                try {
                    MESSAGE_LOCK.wait();
                } catch (InterruptedException e) {
                    Assert.that(false, "NIOMessageWriteManager nterrupted");
                }
            }
        }
    }
    
    private void writeMessages() {
        synchronized(PENDING_WRITERS) {
            Iterator iter = PENDING_WRITERS.iterator();
            while(iter.hasNext()) {
                Connection conn = (Connection)iter.next();
                MessageWriter writer = conn.writer();
                try {
                    writer.write();
                } catch (IOException e) {
                    // TODO:: remove connection here??
                    
                }
            }
        }
    }
        
}
