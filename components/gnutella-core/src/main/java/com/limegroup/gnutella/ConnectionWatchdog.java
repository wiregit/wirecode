package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

/*
 * A "watchdog" thread that periodically examines connections and
 * replaces dud connections with better ones.  There are a number of
 * possible heuristics to use when examining connections.
 */
public class ConnectionWatchdog implements Runnable {
    /** How long (in msec) a connection can be a dud (see below)
     *  before being booted. */
    private static final int EVALUATE_TIME=10000;
    ///** Additional time (in msec) to wait before rechecking connections. */
    //private static final int WAIT_TIME=5000;
    private ConnectionManager manager;

    /** Creates a new ConnectionWatchdog to police the
     *  given manager. */
    public ConnectionWatchdog(ConnectionManager manager) {
        this.manager=manager;
    }

    /** A snapshot of a connection.  Used by run() */
    private static class ConnectionState {
        long sentDropped;
        long sent;

        /** Takes a snapshot of the given connection. */
        ConnectionState(ManagedConnection c) {
            this.sentDropped=c.getNumSentMessagesDropped();
            this.sent=c.getNumMessagesSent();
        }

        /**
         * Returns true if the state of this connection has not
         * made sufficient progress since the old snapshot was taken.
         */
        boolean notProgressedSince(ConnectionState old) {
            //Current policy: returns true if all packets sent since
            //snapshot were dropped.  We could also look for
            //connections that have received no data, but users
            //probably want to turn this off for private networks.
            long numSent=this.sent-old.sent;
            long numSentDropped=this.sentDropped-old.sentDropped;
            return (numSent==numSentDropped) && numSent!=0;
        }

        public String toString() {
            return "{sent: "+sent+", sdropped: "+sentDropped+"}";
        }
    }

    /** Loops forever, replacing old dud connections with new
     *  good ones. */
    public void run() {
        while (true) {
            //Wait a bit.  This seems more flexible then a
            //notification-based scheme, because then the connection
            //would have to know when to notify this.
            //try {
            //    Thread.currentThread().sleep(WAIT_TIME);
            //} catch (InterruptedException e) { /* do nothing */ }

            //Take a snapshot of all connections.  Different data
            //structures could be used here.
            HashMap /* Connection -> ConnectionState */ snapshot=new HashMap();
            for (Iterator iter=manager.getConnections().iterator();
                 iter.hasNext(); ) {
                ManagedConnection c=(ManagedConnection)iter.next();
                snapshot.put(c, new ConnectionState(c));
            }

            //Wait a bit more.
            try {
                Thread.currentThread().sleep(EVALUATE_TIME);
            } catch (InterruptedException e) { /* do nothing */ }

            //Loop through all connections, trying to find ones that
            //have not made sufficient progress.
            for (Iterator iter=manager.getConnections().iterator();
                 iter.hasNext(); ) {
                ManagedConnection c=(ManagedConnection)iter.next();
                Object state=snapshot.get(c);
                if (state==null)
                    continue;  //this is a new connection

                ConnectionState currentState=new ConnectionState(c);
                ConnectionState oldState=(ConnectionState)state;
                if (currentState.notProgressedSince(oldState)) {
                    //Got a dud; replace it.  Here we rely on the
                    //ConnectionManager to do the replacement.  A better
                    //policy might be to restart a new connection to c.
                    //TODO2: If replacing connections doesn't work well, we're
                    //probably too slow, so drop the KEEP_ALIVE.
                    manager.remove(c);
                }
            }
        }
    }
}
