package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
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
    /** Additional time (in msec) to wait before rechecking connections. */
    private static final int REEVALUATE_TIME=8000;

    private final ConnectionManager manager;

    /** 
	 * Creates a new <tt>ConnectionWatchdog</tt> instance to monitor
	 * connections to make sure they are still up and responding well.
	 *
     * @param manager the <tt>ConnectionManager</tt> instance that provides
	 *  access to the list of connections to monitor
     */
    public ConnectionWatchdog(ConnectionManager manager) {
        this.manager = manager;
    }

    /** A snapshot of a connection.  Used by run() */
    private static class ConnectionState {
        long sentDropped;
        long sent;
        long received;

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
     * Returns a list of connections that have not made progress in
     * the last few seconds.  This method takes several seconds to
     * work.
     */
    private List findDuds() {
        //Take a snapshot of all connections, including leaves.  Different data
        //structures could be used here.
        HashMap /* Connection -> ConnectionState */ snapshot=new HashMap();
        for (Iterator iter=allConnections(); iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            if (! c.isKillable())
                continue; //e.g., Clip2 reflector
            snapshot.put(c, new ConnectionState(c));
        }

        //Wait a bit more.
        try {
            Thread.currentThread().sleep(EVALUATE_TIME);
        } catch (InterruptedException e) { /* do nothing */ }

        //Loop through all connections, trying to find ones that
        //have not made sufficient progress. 
        List ret=new ArrayList();
        for (Iterator iter=allConnections(); iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            if (! c.isKillable())
                continue; //e.g., Clip2 reflector
            Object state=snapshot.get(c);
            if (state==null)
                continue;  //this is a new connection

            ConnectionState currentState=new ConnectionState(c);
            ConnectionState oldState=(ConnectionState)state;
            if (currentState.notProgressedSince(oldState)) {
                ret.add(c);
            }
        }
        return ret;
    }

    /**
     * @requires connections is a list of ManagedConnection
     * @modifies manager, router
     * @effects removes from manager any ManagedConnection's in "connections"
     *  that still aren't progressing after being pinged.  This method may
     *  take several seconds.
     */
    private void killIfStillDud(List connections) {
        //Take a snapshot of each connection, then send a ping.  Ideally we'd
        //use a TTL of 1, but Gnotella doesn't respond with TTL's less than 2.
        //The horizon statistics for the connection are temporarily disabled
        //during this process.  In the rare chance that legitimate pongs 
        //(other than in response to my ping), they will be ignored.
        HashMap /* Connection -> ConnectionState */ snapshot=new HashMap();
        for (Iterator iter=connections.iterator(); iter.hasNext();) {
            ManagedConnection c=(ManagedConnection)iter.next();
            if (! c.isKillable())
                continue; //e.g., Clip2 reflector
            snapshot.put(c, new ConnectionState(c));
            c.setHorizonEnabled(false);
            RouterService.getMessageRouter().sendPingRequest(
			    new PingRequest((byte)1), c);
        }
        
        //Wait a tiny amount of time.
        try {
            Thread.currentThread().sleep(REEVALUATE_TIME);
        } catch (InterruptedException e) { /* do nothing */ }

        //Loop through all connections again.  This time, any that
        //haven't made progress are killed.
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            if (! c.isKillable())
                continue; //e.g., Clip2 reflector
            c.setHorizonEnabled(true);
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


    /** 
     * Loop forever, replacing old dud connections with new good ones. 
     */
    public void run() {
        try {
            while (true) {
                //We make fresh data structures every time through the loop to
                //assure that no garbage connnections are retained.  (I'm not sure
                //you can guarantee that calling clear() actually clears reference.)
                List duds=findDuds();
                if (duds.size() > 0) {
                    killIfStillDud(duds);
                }
            }
        } catch (Throwable t) {
            RouterService.error(t);
        }
    }

    /** Returns an iterator of all initialized connections in this, including
     *  leaf connecions. */
    private Iterator allConnections() {
        List normal=manager.getInitializedConnections();
        List leaves=manager.getInitializedClientConnections();

        List buf=new ArrayList(normal.size()+leaves.size());
        buf.addAll(normal);
        buf.addAll(leaves);
        return buf.iterator();
    }
}
