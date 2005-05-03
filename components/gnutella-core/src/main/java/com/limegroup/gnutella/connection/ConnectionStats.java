package com.limegroup.gnutella.connection;

/**
 * Keeps track of sent/received messages & the amount that dropped.
 */
public class ConnectionStats {

    /** The number of messages sent.  This includeds messages that are dropped. */
    private int _numMessagesSent;
    
    /** The number of messages received. This includes messages that are spam. */
    private int _numMessagesReceived;
    
    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    private int _numReceivedMessagesDropped;
    
    /**
     * The number of messages I dropped because the
     * output queue overflowed.  This happens when the remote host
     * cannot receive packets as quickly as I am trying to send them.
     * No synchronization is necessary.
     */
    private int _numSentMessagesDropped;
    
    /**
     * _lastSent/_lastSentDropped and _lastReceived/_lastRecvDropped the values
     * of _numMessagesSent/_numSentMessagesDropped and
     * _numMessagesReceived/_numReceivedMessagesDropped at the last call to
     * getPercentDropped.  LOCKING: These are synchronized by this;
     * finer-grained schemes could be used. 
     */
    private int _lastReceived;
    private int _lastRecvDropped;
    private int _lastSent;
    private int _lastSentDropped;
    
    // Getters.
    public int getSent()  { return _numMessagesSent; }
    public int getReceived() { return _numMessagesReceived; }
    public int getSentDropped() { return _numSentMessagesDropped; }
    public int getReceivedDropped() { return _numReceivedMessagesDropped; }


    /** Adds a number of dropped sent messages */
    public void addSentDropped(int dropped) {
        _numSentMessagesDropped += dropped;
    }
    
    /** Adds a sent message */
    public void addSent() {
        _numMessagesSent++;
    }
    
    /** Increments the number of received messages that have been dropped. */
    public void addReceivedDropped() {
        _numReceivedMessagesDropped++;   
    }
    
    /** Increments the stat for the number of messages received. */
    public void addReceived() {
        _numMessagesReceived++;
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
}