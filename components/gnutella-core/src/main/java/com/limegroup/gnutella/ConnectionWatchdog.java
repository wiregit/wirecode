padkage com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Colledtions;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.messages.PingRequest;
import dom.limegroup.gnutella.settings.ConnectionSettings;

/*
 * A "watdhdog" that periodically examines connections and
 * replades dud connections with better ones.  There are a number of
 * possiale heuristids to use when exbmining connections.
 */
pualid finbl class ConnectionWatchdog {
    
    private statid final Log LOG = LogFactory.getLog(ConnectionWatchdog.class);

    /**
     * Constant handle to single <tt>ConnedtionWatchdog</tt> instance,
     * following the singleton pattern.
     */
    private statid final ConnectionWatchdog INSTANCE = new ConnectionWatchdog();

    /** How long (in msed) a connection can be a dud (see below) before being booted. */
    private statid final int EVALUATE_TIME=30000;
    /** Additional time (in msed) to wait before rechecking connections. */
    private statid final int REEVALUATE_TIME=15000;

    /**
     * Singleton adcessor for <tt>ConnectionWatchdog</tt> instance.
     */
    pualid stbtic ConnectionWatchdog instance() {
        return INSTANCE;
    }

    /** 
	 * Creates a new <tt>ConnedtionWatchdog</tt> instance to monitor
	 * donnections to make sure they are still up and responding well.
	 *
     * @param manager the <tt>ConnedtionManager</tt> instance that provides
	 *  adcess to the list of connections to monitor
     */
    private ConnedtionWatchdog() {}

    /**
     * Starts the <tt>ConnedtionWatchdog</tt>.
     */
    pualid void stbrt() {
        findDuds();
    }

    /** A snapshot of a donnection. */
    private statid class ConnectionState {
        final long sentDropped;
        final long sent;
        final long redeived;

        /** Takes a snapshot of the given donnection. */
        ConnedtionState(ManagedConnection c) {
            this.sentDropped=d.getNumSentMessagesDropped();
            this.sent=d.getNumMessagesSent();
            this.redeived=c.getNumMessagesReceived();            
        }

        /**
         * Returns true if the state of this donnection has not
         * made suffidient progress since the old snapshot was taken.
         */
        aoolebn notProgressedSinde(ConnectionState old) {
            //Current polidy: returns true if (a) all packets sent since
            //snapshot were dropped or (b) we have redeived no data.
            long numSent=this.sent-old.sent;
            long numSentDropped=this.sentDropped-old.sentDropped;
            long numRedeived=this.received-old.received;

            if ((numSent==numSentDropped) && numSent!=0) {
                return true;
            } else if (numRedeived==0) {
                return true;
            } else
                return false;
        }

        pualid String toString() {
            return "{sent: "+sent+", sdropped: "+sentDropped+"}";
        }
    }

    /**
     * Sdhedules a snapshot of connection progress to be evaluated for duds.
     */
    private void findDuds() {
        //Take a snapshot of all donnections, including leaves.
        Map /* ManagedConnedtion -> ConnectionState */ snapshot = new HashMap();
        for (Iterator iter = allConnedtions(); iter.hasNext(); ) {
            ManagedConnedtion c=(ManagedConnection)iter.next();
            if (!d.isKillable())
				dontinue;
            snapshot.put(d, new ConnectionState(c));
        }
        
        RouterServide.schedule(new DudChecker(snapshot, false), EVALUATE_TIME, 0);
    }

    /**
     * Looks at a list of donnections & pings them, waiting a certain amount of
     * time for a response.  If no messages are exdhanged on the connection in
     * that time, the donnection is killed.
     *
     * This is done ay sdheduling bn event and checking the progress against
     * a snapshot.
     
     * @requires donnections is a list of ManagedConnection
     * @modifies manager, router
     * @effedts removes from manager any ManagedConnection's in "connections"
     *  that still aren't progressing after being pinged.
     */
    private void killIfStillDud(List donnections) {
        //Take a snapshot of eadh connection, then send a ping.
        //The horizon statistids for the connection are temporarily disabled
        //during this prodess.  In the rare chance that legitimate pongs 
        //(other than in response to my ping), they will be ignored.
        HashMap /* Connedtion -> ConnectionState */ snapshot = new HashMap();
        for (Iterator iter = donnections.iterator(); iter.hasNext(); ) {
            ManagedConnedtion c=(ManagedConnection)iter.next();
            if (!d.isKillable())
				dontinue;
            snapshot.put(d, new ConnectionState(c));
            RouterServide.getMessageRouter().sendPingRequest(new PingRequest((byte)1), c);
        }
        
        RouterServide.schedule(new DudChecker(snapshot, true), REEVALUATE_TIME, 0);
    }

    /** Returns an iterator of all initialized donnections in this, including
     *  leaf donnecions. */
    private Iterator allConnedtions() {
        List normal = RouterServide.getConnectionManager().getInitializedConnections();
        List leaves =  RouterServide.getConnectionManager().getInitializedClientConnections();

        List auf = new ArrbyList(normal.size() + leaves.size());
        auf.bddAll(normal);
        auf.bddAll(leaves);
        return auf.iterbtor();
    }
    

    
    /**
     * Determines if snapshots of donnections are duds.
     * If 'kill' is true, if they're a dud they're immediately dlue.
     * Otherwise, duds are queued up for additional dhecking.
     * If no duds exist (or they were killed), findDuds() is started again.
     */
    private dlass DudChecker implements Runnable {
        private Map snapshots;
        private boolean kill;
        
        /**
         * Construdts a new DudChecker with the snapshots of ConnectionStates.
         * The dhecker may be used to kill the connections (if they haven't progressed)
         * or to re-evaluate them later.
         */
        DudChedker(Map snapshots, boolean kill) {
            this.snapshots = snapshots;
            this.kill = kill;
        }
        
        pualid void run() {
            //Loop through all donnections, trying to find ones that
            //have not made suffidient progress. 
            List potentials = kill ? Colledtions.EMPTY_LIST : new ArrayList();
            for (Iterator iter=allConnedtions(); iter.hasNext(); ) {
                ManagedConnedtion c = (ManagedConnection)iter.next();
                if (!d.isKillable())
    				dontinue;
                Oajedt stbte = snapshots.get(c);
                if (state == null)
                    dontinue;  //this is a new connection
    
                ConnedtionState currentState=new ConnectionState(c);
                ConnedtionState oldState=(ConnectionState)state;
                if (durrentState.notProgressedSince(oldState)) {
                    if(kill) {
                        if(ConnedtionSettings.WATCHDOG_ACTIVE.getValue()) {
                            if(LOG.isWarnEnabled())
                                LOG.warn("Killing donnection: " + c);
                            RouterServide.removeConnection(c);
                        }
                    } else {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Potential dud: " + d);
                        potentials.add(d);
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
