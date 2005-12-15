package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * A "watchdog" that periodically examines connections and
 * replaces dud connections with better ones.  There are a number of
 * possible heuristics to use when examining connections.
 */
public final class ConnectionWatchdog {
    
    private static final Log LOG = LogFactory.getLog(ConnectionWatchdog.class);

    /**
     * Constant handle to single <tt>ConnectionWatchdog</tt> instance,
     * following the singleton pattern.
     */
    private static final ConnectionWatchdog INSTANCE = new ConnectionWatchdog();

    /** How long (in msec) a connection can be a dud (see below) before being booted. */
    private static final int EVALUATE_TIME=30000;
    /** Additional time (in msec) to wait before rechecking connections. */
    private static final int REEVALUATE_TIME=15000;

    /**
     * Singleton accessor for <tt>ConnectionWatchdog</tt> instance.
     */
    public static ConnectionWatchdog instance() {
        return INSTANCE;
    }

    /** 
	 * Creates a new <tt>ConnectionWatchdog</tt> instance to monitor
	 * connections to make sure they are still up and responding well.
	 *
     * @param manager the <tt>ConnectionManager</tt> instance that provides
	 *  access to the list of connections to monitor
     */
    private ConnectionWatchdog() {}

    /**
     * Starts the <tt>ConnectionWatchdog</tt>.
     */
    public void start() {
        findDuds();
    }

    /** A snapshot of a connection. */
    private static class ConnectionState {
        final long sentDropped;
        final long sent;
        final long received;

        /** Takes a snapshot of the given connection. */
        ConnectionState(ManagedConnection c) {
            this.sentDropped=c.getNumSentMessagesDropped();
            this.sent=c.getNumMessagesSent();
            this.received=c.getNumMessagesReceived();            
        }

        /**
         * Returns true if the state of this connection has not
         * made sufficient progress since the old snapshot was taken.
         */
        boolean notProgressedSince(ConnectionState old) {
            //Current policy: returns true if (a) all packets sent since
            //snapshot were dropped or (b) we have received no data.
            long numSent=this.sent-old.sent;
            long numSentDropped=this.sentDropped-old.sentDropped;
            long numReceived=this.received-old.received;

            if ((numSent==numSentDropped) && numSent!=0) {
                return true;
            } else if (numReceived==0) {
                return true;
            } else
                return false;
        }

        public String toString() {
            return "{sent: "+sent+", sdropped: "+sentDropped+"}";
        }
    }

    /**
     * Schedules a snapshot of connection progress to be evaluated for duds.
     */
    private void findDuds() {
        //Take a snapshot of all connections, including leaves.
        Map /* ManagedConnection -> ConnectionState */ snapshot = new HashMap();
        for (Iterator iter = allConnections(); iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            if (!c.isKillable())
				continue;
            snapshot.put(c, new ConnectionState(c));
        }
        
        RouterService.schedule(new DudChecker(snapshot, false), EVALUATE_TIME, 0);
    }

    /**
     * Looks at a list of connections & pings them, waiting a certain amount of
     * time for a response.  If no messages are exchanged on the connection in
     * that time, the connection is killed.
     *
     * This is done by scheduling an event and checking the progress against
     * a snapshot.
     
     * @requires connections is a list of ManagedConnection
     * @modifies manager, router
     * @effects removes from manager any ManagedConnection's in "connections"
     *  that still aren't progressing after being pinged.
     */
    private void killIfStillDud(List connections) {
        //Take a snapshot of each connection, then send a ping.
        //The horizon statistics for the connection are temporarily disabled
        //during this process.  In the rare chance that legitimate pongs 
        //(other than in response to my ping), they will be ignored.
        HashMap /* Connection -> ConnectionState */ snapshot = new HashMap();
        for (Iterator iter = connections.iterator(); iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            if (!c.isKillable())
				continue;
            snapshot.put(c, new ConnectionState(c));
            RouterService.getMessageRouter().sendPingRequest(new PingRequest((byte)1), c);
        }
        
        RouterService.schedule(new DudChecker(snapshot, true), REEVALUATE_TIME, 0);
    }

    /** Returns an iterator of all initialized connections in this, including
     *  leaf connecions. */
    private Iterator allConnections() {
        List normal = RouterService.getConnectionManager().getInitializedConnections();
        List leaves =  RouterService.getConnectionManager().getInitializedClientConnections();

        List buf = new ArrayList(normal.size() + leaves.size());
        buf.addAll(normal);
        buf.addAll(leaves);
        return buf.iterator();
    }
    

    
    /**
     * Determines if snapshots of connections are duds.
     * If 'kill' is true, if they're a dud they're immediately clue.
     * Otherwise, duds are queued up for additional checking.
     * If no duds exist (or they were killed), findDuds() is started again.
     */
    private class DudChecker implements Runnable {
        private Map snapshots;
        private boolean kill;
        
        /**
         * Constructs a new DudChecker with the snapshots of ConnectionStates.
         * The checker may be used to kill the connections (if they haven't progressed)
         * or to re-evaluate them later.
         */
        DudChecker(Map snapshots, boolean kill) {
            this.snapshots = snapshots;
            this.kill = kill;
        }
        
        public void run() {
            //Loop through all connections, trying to find ones that
            //have not made sufficient progress. 
            List potentials = kill ? Collections.EMPTY_LIST : new ArrayList();
            for (Iterator iter=allConnections(); iter.hasNext(); ) {
                ManagedConnection c = (ManagedConnection)iter.next();
                if (!c.isKillable())
    				continue;
                Object state = snapshots.get(c);
                if (state == null)
                    continue;  //this is a new connection
    
                ConnectionState currentState=new ConnectionState(c);
                ConnectionState oldState=(ConnectionState)state;
                if (currentState.notProgressedSince(oldState)) {
                    if(kill) {
                        if(ConnectionSettings.WATCHDOG_ACTIVE.getValue()) {
                            if(LOG.isWarnEnabled())
                                LOG.warn("Killing connection: " + c);
                            RouterService.removeConnection(c);
                        }
                    } else {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Potential dud: " + c);
                        potentials.add(c);
                    }
                }
            }
            
            if(potentials.isEmpty())
                findDuds();
            else
                killIfStillDud(potentials);
        }
    }
}
