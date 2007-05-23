package com.limegroup.gnutella.connection;

import java.util.Map;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Keeps track of sent/received messages & the amount that dropped.
 */
public class ConnectionStats {

    /** The number of messages sent.  This includeds messages that are dropped. */
    private volatile int _numMessagesSent;
    
    /** The number of messages received. This includes messages that are spam. */
    private volatile int _numMessagesReceived;
    
    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    private volatile int _numReceivedMessagesDropped;
    
    /**
     * The number of messages I dropped because the
     * output queue overflowed.  This happens when the remote host
     * cannot receive packets as quickly as I am trying to send them.
     * No synchronization is necessary.
     */
    private volatile int _numSentMessagesDropped;
    
    private volatile long repliesReceived;
    
    private volatile long repliesSent;
    
    private volatile long queriesReceived;
    
    private volatile long queriesSent;
    /**
     * _lastSent/_lastSentDropped and _lastReceived/_lastRecvDropped the values
     * of _numMessagesSent/_numSentMessagesDropped and
     * _numMessagesReceived/_numReceivedMessagesDropped at the last call to
     * getPercentDropped.  LOCKING: These are synchronized by this;
     * finer-grained schemes could be used. 
     */
    private volatile int _lastReceived;
    private volatile int _lastRecvDropped;
    private volatile int _lastSent;
    private volatile int _lastSentDropped;
    
    // Getters.
    public int getSent()  { return _numMessagesSent; }
    public int getReceived() { return _numMessagesReceived; }
    public int getSentDropped() { return _numSentMessagesDropped; }
    public int getReceivedDropped() { return _numReceivedMessagesDropped; }
    public long getRepliesReceived() { return repliesReceived; }

    /** Adds a number of dropped sent messages */
    public void addSentDropped(int dropped) {
        _numSentMessagesDropped += dropped;
    }
    
    /** Adds a sent message */
    public void addSent(Message m) {
        _numMessagesSent++;
        if (m instanceof QueryRequest)
            queriesSent++;
        else if (m instanceof QueryReply)
            repliesSent++;
    }
    
    /** Increments the number of received messages that have been dropped. */
    public void addReceivedDropped() {
        _numReceivedMessagesDropped++;   
    }
    
    /** Increments the stat for the number of messages received. */
    public void addReceived() {
        _numMessagesReceived++;
    }
    
    public void replyReceived() {
        repliesReceived++;
    }
    public void queryReceived() {
        queriesReceived++;
    }
    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentReceivedDropped that were
     *  dropped by this end of the connection.
     */
    public synchronized float getPercentReceivedDropped() {
        int rdiff = _numMessagesReceived - _lastReceived;
        int ddiff = _numReceivedMessagesDropped - _lastRecvDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastReceived = _numMessagesReceived;
        _lastRecvDropped = _numReceivedMessagesDropped;
        return percent;
    }

    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentSentDropped that were
     *  dropped by this end of the connection.  This value may be
     *  greater than 100%, e.g., if only one message is sent but
     *  four are dropped during a given time period.
     */
    public synchronized float getPercentSentDropped() {
        int rdiff = _numMessagesSent - _lastSent;
        int ddiff = _numSentMessagesDropped - _lastSentDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastSent = _numMessagesSent;
        _lastSentDropped = _numSentMessagesDropped;
        return percent;
    }
    
    public void addStats(Map<String,Object> m) {
        m.put("nqr",repliesReceived);
        m.put("nqs",repliesSent);
        m.put("npr",queriesReceived);
        m.put("nps",queriesSent);
    }
}