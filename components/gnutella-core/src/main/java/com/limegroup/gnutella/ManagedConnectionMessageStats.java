package com.limegroup.gnutella;

/**
 * Class which stores all the message statistics for a particular Managed 
 * Connection.  It hides the message statistics gathering details from external
 * callers to ManagedConnection. 
 */
public class ManagedConnectionMessageStats
{
    /**
     * Reference to the actual Managed Connection whose stats this class is
     * storing.
     */
    private ManagedConnection _managedConnection;

    /**
     * The number of messages sent.    
     */
    private int _numMessagesSent = 0;
    /**
     * The number of messages received.  This includes messages that are
     * eventually dropped.  This stat is not synchronized because receiving
     * is not thread-safe; callers are expected to make sure only one thread
     * at a time is calling receive on a given connection.
     */
    private int _numMessagesReceived = 0;
    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    private int _numReceivedMessagesDropped = 0;
    /**
     * The number of messages I dropped because the
     * output queue overflowed.  This happens when the remote host
     * cannot receive packets as quickly as I am trying to send them.
     * No synchronization is necessary.
     */
    private int _numSentMessagesDropped = 0;
    /**
     * _lastSent/_lastSentDropped and _lastReceived/_lastRecvDropped the values
     * of _numMessagesSent/_numSentMessagesDropped and
     * _numMessagesReceived/_numReceivedMessagesDropped at the last call to
     * getPercentDropped.  
     * LOCKING: These are synchronized by this;
     */
    private int _lastReceived = 0;
    private int _lastRecvDropped = 0;
    private int _lastSent = 0;
    private int _lastSentDropped = 0;

    public ManagedConnectionMessageStats(ManagedConnection managedConnection)
    {
        this._managedConnection = managedConnection;
    }

    /**
     * Increments the number of received messages dropped on this connection
     */
    public void countDroppedMessage() 
    {
        _numReceivedMessagesDropped++;
    }

    /**
     * Increments the number of messages sent on this connection
     */
    public void countSentMessage() 
    {
        _numMessagesSent++;
    }

    /**
     * Increments the number of received messages on this connection
     */
    public void countReceivedMessage() 
    {
        _numMessagesReceived++;
    }

    /**
     * Increments the number of messages originating on this connection, but not
     * actually sent out (i.e., dropped)
     */
    public void countSentMessageDropped() 
    {
        _numSentMessagesDropped++;
    }

    /** Returns the number of messages sent on this connection */
    public int getNumMessagesSent() 
    {
        return _numMessagesSent;
    }

    /** Returns the number of messages received on this connection */
    public int getNumMessagesReceived() 
    {
        return _numMessagesReceived;
    }

    /** Returns the number of messages I dropped while trying to send
     *  on this connection.  This happens when the remote host cannot
     *  keep up with me. */
    public int getNumSentMessagesDropped() 
    {
        return _numSentMessagesDropped;
    }

    /**
     * The number of messages received on this connection either filtered out
     * or dropped because we didn't know how to route them.
     */
    public long getNumReceivedMessagesDropped() 
    {
        return _numReceivedMessagesDropped;
    }

    /**
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentReceivedDropped that were
     *  dropped by this end of the connection.
     */
    public float getPercentReceivedDropped() 
    {
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
     *  dropped by this end of the connection.
     */
    public float getPercentSentDropped() {
        int rdiff = _numMessagesSent - _lastSent;
        int ddiff = _numSentMessagesDropped - _lastSentDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);

        _lastSent = _numMessagesSent;
        _lastSentDropped = _numSentMessagesDropped;
        return percent;
    }
}


