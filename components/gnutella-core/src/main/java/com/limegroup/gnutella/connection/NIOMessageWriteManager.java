package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
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
    
    /**
     * Instance of <tt>NIOMessageWriteManager</tt>, following singleton.
     */
    private static final NIOMessageWriteManager INSTANCE =
        new NIOMessageWriteManager();

    /** 
     * A lock for pending messages status.
     */
    private final Object MESSAGE_LOCK = new Object();        
    
    /**
     * <tt>List</tt> of <tt>MessageWriter</tt>s that have buffered messages to
     * write to the network.
     */    
    private final List PENDING_WRITERS = 
        Collections.synchronizedList(new LinkedList());
    
    /**
     * Instance accessor for the single <tt>NIOMessageWriteManager</tt> 
     * instance.
     * 
     * @return the <tt>MessageWriteManager</tt> instance
     */
    public static NIOMessageWriteManager instance() {
        return INSTANCE;
    }
    
    /**
     * Creates an <tt>NIOMessageWriteManager</tt>, starting the writing thread.
     */
    private NIOMessageWriteManager() {
        Thread managerThread = new Thread(this, "NIO manager thread");
        managerThread.setDaemon(true);
        managerThread.start();
    }
    
    /**
     * Adds a <tt>MessageWriter</tt> to the collection of message writers 
     * needing write notifications.
     * 
     * @param writer the <tt>MessageWriter</tt> with buffered messages to add
     */
    public void addWriter(Connection writer) {
        System.out.println("NIOMessageWriteManager::addWriter*********");
        PENDING_WRITERS.add(writer);
        synchronized (MESSAGE_LOCK) {
            MESSAGE_LOCK.notify();
        }
    }
    
    /**
     * Removes the specified <tt>MessageWriter</tt> from the collection of 
     * writers needing write notifications.
     * 
     * @param writer the <tt>MessageWriter</tt> to remove
     */
    public void removeWriter(Connection writer) {
        PENDING_WRITERS.remove(writer);
    }
    
    /**
     * Waits for connections needing write notifications and writes 
     * notifications when there are writers needing servicing.
     */
    public void run() {
        try {
            while (true) {
                waitForMessages();
                writeMessages();
            }                
        } catch(Throwable t) {    
            ErrorService.error(t);
        }
    }
    
    /** 
     * Wait until there are message writers needing notification to write.
     */
    private void waitForMessages() {
        synchronized (MESSAGE_LOCK) {
            while (PENDING_WRITERS.isEmpty()) {        
                try {
                    MESSAGE_LOCK.wait();
                } catch (InterruptedException e) {
                    Assert.that(false, "NIOMessageWriteManager interrupted");
                }
            }
        }
    }
    
    /**
     * Loops through the pending message writers, notifying each one to write.
     */
    private void writeMessages() {
        synchronized(PENDING_WRITERS) {
            Iterator iter = PENDING_WRITERS.iterator();
            while(iter.hasNext()) {
                Connection conn = (Connection)iter.next();
                try {
                    conn.writer().write();
                } catch (IOException e) {
                    RouterService.removeConnection(conn);
                    
                }
            }
        }
    }
        
}
