package com.limegroup.gnutella;

import java.util.*;
import com.limegroup.gnutella.messages.PingReply;


/***************************************************************************
 * Horizon statistics. We measure the horizon by looking at all ping replies
 * coming per connection--regardless whether they are in response to pings
 * originating from us.  To avoid double-counting ping replies, we keep a
 * set of Endpoint's around, bounded in size to save memory.  This scheme is
 * robust in the face of pong throttling.  Note however that we cannot
 * discern pings from multiple hosts with the same private address.  But you
 * are probably not interested in such hosts anyway.
 *
 * The problem with this scheme is that the numbers tend to grow without
 * bound, even if hosts leave the network.  Ideally we'd like to clear all
 * pongs that are more than HORIZON_UPDATE_TIME milliseconds old, but that's
 * difficult to implement efficiently.  As a simplication, we periodically
 * clear the set of pongs every HORIZON_UPDATE_TIME milliseconds (by calling
 * updateHorizonStats) and start recounting.  While we are recounting, we
 * return the last size of the set.  So pongs in the set are
 * HORIZON_UPDATE_TIME to 2*HORIZON_UPDATE_TIME milliseconds old.
 * 
 * LOCKING: obtain this' monitor
 **************************************************************************/
public final class HorizonCounter {
    
    private static final HorizonCounter INSTANCE = new HorizonCounter();

    /** The approximate time to expire pongs, in milliseconds. */
    static long HORIZON_UPDATE_TIME=10*60*1000; //10 minutes
    /** The last time refreshHorizonStats was called. */
    private long _lastRefreshHorizonTime=System.currentTimeMillis();
    /** True iff refreshHorizonStats has been called. */
    private boolean _refreshedHorizonStats=false;
    /** The max number of pongs to save. */
    private static final int MAX_PING_REPLIES=4000;
    /** The endpoints of pongs seen before.  Eliminates duplicates. */
    private Set /* of Endpoint */ _pongs=new HashSet();
    /** The size of _pingReplies before updateHorizonStats was called. */
    private long _totalHorizonFileSize=0;
    private long _numHorizonFiles=0;
    private long _numHorizonHosts=0;
    /** INVARIANT: _nextTotalHorizonFileSize==_pingReplies.size() */
    private long _nextTotalHorizonFileSize=0;
    private long _nextNumHorizonFiles=0;
    private long _nextNumHorizonHosts=0;
    
    public static HorizonCounter instance() {
        return INSTANCE;
    }
    
    public synchronized void addPong(PingReply pong) {
        //Have we already seen a ping from this hosts?
        Endpoint host=new Endpoint(pong.getAddress(), pong.getPort());
        if (_pongs.size()<MAX_PING_REPLIES && _pongs.add(host)) {
            //Nope.  Increment numbers. 
            _nextTotalHorizonFileSize += pong.getKbytes();
            _nextNumHorizonFiles += pong.getFiles();
            _nextNumHorizonHosts++;           
        }
    }
    
    public synchronized void refresh() {
         //Makes sure enough time has elapsed.
         long now=System.currentTimeMillis();
         long elapsed=now-_lastRefreshHorizonTime;        
         if (elapsed<HORIZON_UPDATE_TIME)
             return;
         _lastRefreshHorizonTime=now;
        
         //Ok, now update stats.
         _numHorizonHosts=_nextNumHorizonHosts;
         _numHorizonFiles=_nextNumHorizonFiles;
         _totalHorizonFileSize=_nextTotalHorizonFileSize;

         _nextNumHorizonHosts=0;
         _nextNumHorizonFiles=0;
         _nextTotalHorizonFileSize=0;

         _pongs.clear();
         _refreshedHorizonStats=true;        
    }

    /** Returns the number of hosts reachable from me. */
    public synchronized long getNumHosts() {
        if (_refreshedHorizonStats)
            return _numHorizonHosts;
        else 
            return _nextNumHorizonHosts;
    }

    /** Returns the number of files reachable from me. */
    public synchronized long getNumFiles() {
        if (_refreshedHorizonStats) 
            return _numHorizonFiles;
        else
            return _nextNumHorizonFiles;
    }

    /** Returns the size of all files reachable from me. */
    public synchronized long getTotalFileSize() {
        if (_refreshedHorizonStats) 
            return _totalHorizonFileSize;
        else
            return _nextTotalHorizonFileSize;
    }    
}       