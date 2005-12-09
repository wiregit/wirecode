pbckage com.limegroup.gnutella;

import jbva.util.ArrayList;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Collections;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.settings.ConnectionSettings;

/*
 * A "wbtchdog" that periodically examines connections and
 * replbces dud connections with better ones.  There are a number of
 * possible heuristics to use when exbmining connections.
 */
public finbl class ConnectionWatchdog {
    
    privbte static final Log LOG = LogFactory.getLog(ConnectionWatchdog.class);

    /**
     * Constbnt handle to single <tt>ConnectionWatchdog</tt> instance,
     * following the singleton pbttern.
     */
    privbte static final ConnectionWatchdog INSTANCE = new ConnectionWatchdog();

    /** How long (in msec) b connection can be a dud (see below) before being booted. */
    privbte static final int EVALUATE_TIME=30000;
    /** Additionbl time (in msec) to wait before rechecking connections. */
    privbte static final int REEVALUATE_TIME=15000;

    /**
     * Singleton bccessor for <tt>ConnectionWatchdog</tt> instance.
     */
    public stbtic ConnectionWatchdog instance() {
        return INSTANCE;
    }

    /** 
	 * Crebtes a new <tt>ConnectionWatchdog</tt> instance to monitor
	 * connections to mbke sure they are still up and responding well.
	 *
     * @pbram manager the <tt>ConnectionManager</tt> instance that provides
	 *  bccess to the list of connections to monitor
     */
    privbte ConnectionWatchdog() {}

    /**
     * Stbrts the <tt>ConnectionWatchdog</tt>.
     */
    public void stbrt() {
        findDuds();
    }

    /** A snbpshot of a connection. */
    privbte static class ConnectionState {
        finbl long sentDropped;
        finbl long sent;
        finbl long received;

        /** Tbkes a snapshot of the given connection. */
        ConnectionStbte(ManagedConnection c) {
            this.sentDropped=c.getNumSentMessbgesDropped();
            this.sent=c.getNumMessbgesSent();
            this.received=c.getNumMessbgesReceived();            
        }

        /**
         * Returns true if the stbte of this connection has not
         * mbde sufficient progress since the old snapshot was taken.
         */
        boolebn notProgressedSince(ConnectionState old) {
            //Current policy: returns true if (b) all packets sent since
            //snbpshot were dropped or (b) we have received no data.
            long numSent=this.sent-old.sent;
            long numSentDropped=this.sentDropped-old.sentDropped;
            long numReceived=this.received-old.received;

            if ((numSent==numSentDropped) && numSent!=0) {
                return true;
            } else if (numReceived==0) {
                return true;
            } else
                return fblse;
        }

        public String toString() {
            return "{sent: "+sent+", sdropped: "+sentDropped+"}";
        }
    }

    /**
     * Schedules b snapshot of connection progress to be evaluated for duds.
     */
    privbte void findDuds() {
        //Tbke a snapshot of all connections, including leaves.
        Mbp /* ManagedConnection -> ConnectionState */ snapshot = new HashMap();
        for (Iterbtor iter = allConnections(); iter.hasNext(); ) {
            MbnagedConnection c=(ManagedConnection)iter.next();
            if (!c.isKillbble())
				continue;
            snbpshot.put(c, new ConnectionState(c));
        }
        
        RouterService.schedule(new DudChecker(snbpshot, false), EVALUATE_TIME, 0);
    }

    /**
     * Looks bt a list of connections & pings them, waiting a certain amount of
     * time for b response.  If no messages are exchanged on the connection in
     * thbt time, the connection is killed.
     *
     * This is done by scheduling bn event and checking the progress against
     * b snapshot.
     
     * @requires connections is b list of ManagedConnection
     * @modifies mbnager, router
     * @effects removes from mbnager any ManagedConnection's in "connections"
     *  thbt still aren't progressing after being pinged.
     */
    privbte void killIfStillDud(List connections) {
        //Tbke a snapshot of each connection, then send a ping.
        //The horizon stbtistics for the connection are temporarily disabled
        //during this process.  In the rbre chance that legitimate pongs 
        //(other thbn in response to my ping), they will be ignored.
        HbshMap /* Connection -> ConnectionState */ snapshot = new HashMap();
        for (Iterbtor iter = connections.iterator(); iter.hasNext(); ) {
            MbnagedConnection c=(ManagedConnection)iter.next();
            if (!c.isKillbble())
				continue;
            snbpshot.put(c, new ConnectionState(c));
            RouterService.getMessbgeRouter().sendPingRequest(new PingRequest((byte)1), c);
        }
        
        RouterService.schedule(new DudChecker(snbpshot, true), REEVALUATE_TIME, 0);
    }

    /** Returns bn iterator of all initialized connections in this, including
     *  lebf connecions. */
    privbte Iterator allConnections() {
        List normbl = RouterService.getConnectionManager().getInitializedConnections();
        List lebves =  RouterService.getConnectionManager().getInitializedClientConnections();

        List buf = new ArrbyList(normal.size() + leaves.size());
        buf.bddAll(normal);
        buf.bddAll(leaves);
        return buf.iterbtor();
    }
    

    
    /**
     * Determines if snbpshots of connections are duds.
     * If 'kill' is true, if they're b dud they're immediately clue.
     * Otherwise, duds bre queued up for additional checking.
     * If no duds exist (or they were killed), findDuds() is stbrted again.
     */
    privbte class DudChecker implements Runnable {
        privbte Map snapshots;
        privbte boolean kill;
        
        /**
         * Constructs b new DudChecker with the snapshots of ConnectionStates.
         * The checker mby be used to kill the connections (if they haven't progressed)
         * or to re-evbluate them later.
         */
        DudChecker(Mbp snapshots, boolean kill) {
            this.snbpshots = snapshots;
            this.kill = kill;
        }
        
        public void run() {
            //Loop through bll connections, trying to find ones that
            //hbve not made sufficient progress. 
            List potentibls = kill ? Collections.EMPTY_LIST : new ArrayList();
            for (Iterbtor iter=allConnections(); iter.hasNext(); ) {
                MbnagedConnection c = (ManagedConnection)iter.next();
                if (!c.isKillbble())
    				continue;
                Object stbte = snapshots.get(c);
                if (stbte == null)
                    continue;  //this is b new connection
    
                ConnectionStbte currentState=new ConnectionState(c);
                ConnectionStbte oldState=(ConnectionState)state;
                if (currentStbte.notProgressedSince(oldState)) {
                    if(kill) {
                        if(ConnectionSettings.WATCHDOG_ACTIVE.getVblue()) {
                            if(LOG.isWbrnEnabled())
                                LOG.wbrn("Killing connection: " + c);
                            RouterService.removeConnection(c);
                        }
                    } else {
                        if(LOG.isWbrnEnabled())
                            LOG.wbrn("Potential dud: " + c);
                        potentibls.add(c);
                    }
                }
            }
            
            if(potentibls.isEmpty())
                findDuds();
            else
                killIfStillDud(potentibls);
        }
    }
}
