package com.limegroup.gnutella;

/*
 * A "watchdog" thread that periodically examines connections and
 * replaces dud connections with better ones.  There are a number of
 * possible heuristics to use when examining connections.  Currently
 * we look at the number of routed query replies.  
 */
public class ConnectionWatchdog implements Runnable {
    /** How often to check connections, in msecs. */
    private static final int EVALUATE_TIME=20000;
    private ConnectionManager manager;

    /** Creates a new ConnectionWatchdog to police the
     *  given manager. */
    public ConnectionWatchdog(ConnectionManager manager) {
        this.manager=manager;
    }

    /** Loops forever, replacing old bad connections with new
     *  good ones. */
    public void run() {
        while (true) {
            //Recheck every few second.  This seems more flexible then
            //a notification-based scheme, because then the connection
            //would have to know when to notify this.
            try {
                Thread.currentThread().sleep(EVALUATE_TIME);
            } catch (InterruptedException e) { /* do nothing */ }            

            //Loop through all connections, trying to find one that
            //has high send or receive drop rates.  If replacing
            //connections doesn't work well, we're probably too slow,
            //so drop the KEEP_ALIVE.
        }        
    }
}
